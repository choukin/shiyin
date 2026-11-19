package com.shiyin.player.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 真实 PCM 波形提取：用 MediaCodec 把音频轨解码成 16-bit PCM，
 * 按 presentationTimeUs 分桶计算 RMS，再归一化到 0.05..1。
 *
 * 一期 WaveformPlaceholder 是 sin() 伪随机柱条；二期这里替换为真实振幅。
 * 任何解码失败都返回空列表，由 UI 降级到静态波形，绝不抛异常打断页面。
 */
class WaveformExtractor(private val context: Context) {

    suspend fun extract(uri: Uri, barCount: Int = DEFAULT_BAR_COUNT): List<Float> =
        withContext(Dispatchers.IO) {
            if (barCount <= 0) return@withContext emptyList()
            var mediaExtractor: MediaExtractor? = null
            var codec: MediaCodec? = null
            try {
                mediaExtractor = MediaExtractor()
                mediaExtractor.setDataSource(context, uri, null)
                val audioTrack = findAudioTrack(mediaExtractor)
                    ?: return@withContext emptyList()
                val format = mediaExtractor.getTrackFormat(audioTrack)
                val mime = format.getString(MediaFormat.KEY_MIME)
                    ?: return@withContext emptyList()
                mediaExtractor.selectTrack(audioTrack)

                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val samples = ArrayList<Pair<Long, Float>>(SAMPLE_CAPACITY)
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                var guard = 0

                while (!outputDone && guard++ < MAX_ITERATIONS) {
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                            if (inputBuffer == null) {
                                codec.queueInputBuffer(
                                    inputIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                                if (size < 0) {
                                    codec.queueInputBuffer(
                                        inputIndex, 0, 0, 0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputDone = true
                                } else {
                                    codec.queueInputBuffer(
                                        inputIndex, 0, size,
                                        mediaExtractor.sampleTime, 0
                                    )
                                    mediaExtractor.advance()
                                }
                            }
                        }
                    }

                    val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                    if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && info.size > 0 && samples.size < SAMPLE_CAPACITY) {
                            samples += info.presentationTimeUs to
                                computeRms(outputBuffer, info.offset, info.size)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }

                normalize(samples, barCount)
            } catch (_: Throwable) {
                emptyList()
            } finally {
                runCatching { codec?.stop() }
                runCatching { codec?.release() }
                runCatching { mediaExtractor?.release() }
            }
        }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return index
        }
        return null
    }

    /** 从解码后的 PCM ByteBuffer 里抽样算 RMS；小端 16-bit。 */
    private fun computeRms(buffer: ByteBuffer, offset: Int, size: Int): Float {
        val duplicate = buffer.duplicate()
        duplicate.position(offset)
        duplicate.limit(offset + size)
        duplicate.order(ByteOrder.LITTLE_ENDIAN)
        val shorts = duplicate.asShortBuffer()
        val total = shorts.remaining()
        if (total <= 0) return 0f
        val step = (total / SAMPLES_PER_FRAME).coerceAtLeast(1)
        var sum = 0.0
        var count = 0
        var index = 0
        while (index < total) {
            val sample = shorts.get(index).toDouble() / Short.MAX_VALUE
            sum += sample * sample
            count++
            index += step
        }
        return sqrt(sum / count.coerceAtLeast(1)).toFloat()
    }

    /** 按最大时间戳分桶，避免依赖可能缺失的 KEY_DURATION。 */
    private fun normalize(samples: List<Pair<Long, Float>>, barCount: Int): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val maxTime = samples.maxOf { it.first }.coerceAtLeast(1L)
        val buckets = FloatArray(barCount)
        val counts = IntArray(barCount)
        samples.forEach { (timeUs, rms) ->
            val index = ((timeUs.toDouble() / maxTime) * barCount)
                .toInt().coerceIn(0, barCount - 1)
            buckets[index] += rms
            counts[index]++
        }
        val averages = FloatArray(barCount) { index ->
            if (counts[index] > 0) buckets[index] / counts[index] else 0f
        }
        val peak = averages.maxOrNull()?.coerceAtLeast(MIN_PEAK) ?: 1f
        return averages.map { (it / peak).coerceIn(MIN_BAR, 1f) }
    }

    private companion object {
        const val DEFAULT_BAR_COUNT = 64
        const val TIMEOUT_US = 10_000L
        const val MAX_ITERATIONS = 200_000
        const val SAMPLE_CAPACITY = 20_000
        const val SAMPLES_PER_FRAME = 256
        const val MIN_PEAK = 0.001f
        const val MIN_BAR = 0.05f
    }
}
