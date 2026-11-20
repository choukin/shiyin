package com.shiyin.player.data.media

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 把 16-bit PCM 封装为标准 RIFF/WAVE 文件。
 *
 * WAV 只是「 PCM + 44 字节头」，无需编码，是最简单的无损导出路径。
 * 数据本身由 [PcmDecoder] 产出，这里只负责写头与回写长度字段。
 */
object WavEncoder {

    fun encode(pcm: PcmDecoder.PcmData, output: File) {
        val channels = pcm.channelCount
        val sampleRate = pcm.sampleRate
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.bytes.size

        RandomAccessFile(output, "rw").use { raf ->
            // 44 字节标准头
            raf.write("RIFF".toByteArray(Charsets.US_ASCII))
            raf.writeLE(36 + dataSize, 4)
            raf.write("WAVE".toByteArray(Charsets.US_ASCII))
            raf.write("fmt ".toByteArray(Charsets.US_ASCII))
            raf.writeLE(16, 4)                 // fmt chunk 长度
            raf.writeLE(1, 2)                  // PCM 格式代码
            raf.writeLE(channels, 2)
            raf.writeLE(sampleRate, 4)
            raf.writeLE(byteRate, 4)
            raf.writeLE(blockAlign, 2)
            raf.writeLE(bitsPerSample, 2)
            raf.write("data".toByteArray(Charsets.US_ASCII))
            raf.writeLE(dataSize, 4)
            raf.write(pcm.bytes)
        }
    }

    private fun RandomAccessFile.writeLE(value: Int, byteCount: Int) {
        val buf = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN)
        when (byteCount) {
            2 -> buf.putShort(value.toShort())
            4 -> buf.putInt(value)
        }
        write(buf.array())
    }
}
