package com.shiyin.player.data.media

import io.github.hequanli.recorder.mp3.HeLame
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream

/**
 * 用 LAME（libLame.so）把 16-bit PCM 交错数据编码为 MP3。
 *
 * Android 平台没有 MP3 编码器、Media3 Transformer 也只能输出 AAC，
 * 因此走 HeLame 的 native encode 接口：PCM → MP3 帧 → 文件。
 * 该库仅提供 arm64-v8a 的 .so；其他 ABI 上 [isAvailable] 返回 false，
 * UI 应据此隐藏 MP3 选项，避免用户选了之后导出失败。
 */
object Mp3Encoder {

    private const val BITRATE_KBPS = 192

    /**
     * 当前设备是否具备 LAME native 库（arm64-v8a）。
     * 直接探测 .so 能否加载，不依赖 HeLame 的 getVersion()（Kotlin 绑定未导出该方法）。
     * HeLame 的 static 块内部也会 loadLibrary，重复 load 无副作用。
     */
    val isAvailable: Boolean by lazy {
        runCatching {
            System.loadLibrary("Lame")
            true
        }.getOrDefault(false)
    }

    /**
     * 编码 PCM 到 MP3 文件。
     * @throws IllegalStateException native 库不可用或编码失败
     */
    fun encode(pcm: PcmDecoder.PcmData, output: File) {
        check(isAvailable) { "当前设备不支持 MP3 编码" }
        val lame = HeLame
        val channels = pcm.channelCount
        val sampleRate = pcm.sampleRate
        // 参数顺序经反编译库内部调用点确认（HeLame.init: IIIIIIIZ）：
        //   init(inSampleRate, outChannels, outSampleRate, bitrate, quality, mode, vbrMode, setId3Tag)
        // 库自己的 Mp3AudioWriter 传 (rate, channels, rate, bitrate, 2, -1, -1, false)；
        // -1/-1 让 LAME 按声道自动选 mode 与 VBR，这里沿用同一组安全值。
        val bitrate = if (sampleRate < 22050) 64 else if (sampleRate < 44100) 96 else BITRATE_KBPS
        lame.init(sampleRate, channels, sampleRate, bitrate, 2, -1, -1, false)

        val samplesPerChunk = 1152
        val bytesPerSample = 2
        val mp3Buffer = ByteArray(samplesPerChunk * channels * bytesPerSample * 2 + 7200)
        val interleaved = pcm.bytes

        try {
            BufferedOutputStream(FileOutputStream(output)).use { out ->
                var offset = 0
                val frameBytes = samplesPerChunk * channels * bytesPerSample
                while (offset + frameBytes <= interleaved.size) {
                    val chunk = interleaved.copyOfRange(offset, offset + frameBytes)
                    val encoded = lame.encodeInterleaved(
                        chunkToShorts(chunk), samplesPerChunk, mp3Buffer
                    )
                    if (encoded > 0) out.write(mp3Buffer, 0, encoded)
                    offset += frameBytes
                }
                // flush 尾部不足一帧的样本
                val tailLen = interleaved.size - offset
                if (tailLen >= bytesPerSample) {
                    val tailSamples = tailLen / (channels * bytesPerSample)
                    if (tailSamples > 0) {
                        val tail = interleaved.copyOfRange(offset, offset + tailSamples * channels * bytesPerSample)
                        val encoded = lame.encodeInterleaved(
                            chunkToShorts(tail), tailSamples, mp3Buffer
                        )
                        if (encoded > 0) out.write(mp3Buffer, 0, encoded)
                    }
                }
                val flushed = lame.flush(mp3Buffer)
                if (flushed > 0) out.write(mp3Buffer, 0, flushed)
                out.flush()
            }
        } finally {
            lame.close()
        }
    }

    /** 小端字节对转 short 数组（LAME 的 encodeInterleaved 接收 short[]）。 */
    private fun chunkToShorts(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt() and 0xFF
            shorts[i] = (lo or (hi shl 8)).toShort()
        }
        return shorts
    }
}
