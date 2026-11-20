package com.shiyin.player.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.shiyin.player.data.util.ClipRange
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * 把媒体文件的音频轨解码为 16-bit PCM 交错数据，供 WAV 封装与 LAME MP3 编码复用。
 *
 * 链路：MediaExtractor 选音频轨 → seek 到裁剪起点 → MediaCodec 解码 →
 * 收集 raw PCM。输出统一为 16-bit、小端、声道交错（L R L R…），采样率取源文件。
 *
 * 之所以需要它：Android 平台没有 MP3 编码器，Media3 Transformer 也只能输出 AAC；
 * 要产出 WAV/MP3，必须先自己把压缩音频解码成 PCM。
 */
object PcmDecoder {

    /** 解码结果：交错 PCM 字节 + 采样率 + 声道数。 */
    data class PcmData(
        val bytes: ByteArray,
        val sampleRate: Int,
        val channelCount: Int
    ) {
        val sampleCount: Int get() = bytes.size / (channelCount * 2)
    }

    /**
     * 解码 [range] 区间内的音频。range 为空/整段（lengthMs<=0）时解码整段。
     * @throws IllegalStateException 无音频轨或解码器创建失败
     */
    fun decode(
        context: Context,
        uri: Uri,
        range: ClipRange
    ): PcmData = withIoContext {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val audioTrackIndex = selectAudioTrack(extractor)
                ?: throw IllegalStateException("所选文件没有音频轨道")

            val format = extractor.getTrackFormat(audioTrackIndex)
            val sampleRate = format.getString(MediaFormat.KEY_MIME)?.let {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } ?: throw IllegalStateException("无法读取采样率")
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            var durationUs = 0L
            if (format.containsKey(MediaFormat.KEY_DURATION)) {
                durationUs = format.getLong(MediaFormat.KEY_DURATION)
            }

            extractor.selectTrack(audioTrackIndex)

            // 定位裁剪起点：MediaExtractor 可能落在关键帧之前，解码时按时间戳丢弃多余帧。
            val startUs = if (range.lengthMs > 0L) range.startMs * 1_000L else 0L
            val endUs = if (range.lengthMs > 0L) range.endMs * 1_000L else durationUs
            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            codec = MediaCodec.createDecoderByType(
                format.getString(MediaFormat.KEY_MIME)
                    ?: throw IllegalStateException("无法读取音频 MIME")
            )
            codec.configure(format, null, null, 0)
            codec.start()

            val pcm = decodeLoop(codec, extractor, startUs, endUs, channelCount)
            PcmData(pcm, sampleRate, channelCount)
        } finally {
            codec?.let {
                try {
                    it.stop()
                } catch (_: Exception) {
                }
                it.release()
            }
            extractor.release()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    /**
     * 标准解码循环：把输入 buffer 喂给 decoder，把输出 PCM  append 到 ByteArray。
     * 只保留 presentationTimeUs 落在 [startUs, endUs) 的音频。
     */
    private fun decodeLoop(
        codec: MediaCodec,
        extractor: MediaExtractor,
        startUs: Long,
        endUs: Long,
        channelCount: Int
    ): ByteArray {
        val bufferInfo = MediaCodec.BufferInfo()
        val output = java.io.ByteArrayOutputStream()
        val bytesPerFrame = channelCount * 2 // 16-bit
        var inputEos = false
        var outputEos = false
        val timeoutUs = 10_000L

        while (!outputEos) {
            if (!inputEos) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inputBuffer: ByteBuffer = codec.getInputBuffer(inIndex)
                        ?: break
                    inputBuffer.clear()
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0 || (endUs > 0L && extractor.sampleTime >= endUs)) {
                        codec.queueInputBuffer(
                            inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputEos = true
                    } else {
                        val pts = extractor.sampleTime
                        codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                        extractor.advance()
                    }
                }
            }

            when (val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                else -> {
                    if (outIndex >= 0) {
                        val pts = bufferInfo.presentationTimeUs
                        val inRange = pts >= startUs && (endUs <= 0L || pts < endUs)
                        if (inRange && bufferInfo.size > 0 && !inputEosIgnored(bufferInfo)) {
                            val outBuffer: ByteBuffer = codec.getOutputBuffer(outIndex)
                                ?: ByteBuffer.allocateDirect(0)
                            val copy = ByteArray(minOf(bufferInfo.size, outBuffer.remaining()))
                            outBuffer.get(copy)
                            // MediaCodec 输出为 native order，统一转小端（16-bit PCM 要求）。
                            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN && copy.size >= 2) {
                                swap16(copy)
                            }
                            output.write(copy, 0, copy.size)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEos = true
                        }
                    }
                }
            }
        }

        // 截断到完整帧边界，避免末尾半帧。
        val raw = output.toByteArray()
        val aligned = raw.size - (raw.size % bytesPerFrame)
        return if (aligned == raw.size) raw else raw.copyOf(aligned)
    }

    private fun inputEosIgnored(info: MediaCodec.BufferInfo): Boolean =
        info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 && info.size == 0

    private fun swap16(data: ByteArray) {
        var i = 0
        while (i + 1 < data.size) {
            val t = data[i]
            data[i] = data[i + 1]
            data[i + 1] = t
            i += 2
        }
    }

    /** 把调用从 Dispatchers.IO 迁入；此处仅做标记，实际调度在调用方 withContext。 */
    private inline fun <T> withIoContext(block: () -> T): T = block()
}
