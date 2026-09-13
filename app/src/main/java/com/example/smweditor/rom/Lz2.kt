package com.example.smweditor.rom

/**
 * LC_LZ2 decompressor.
 *
 * Format: a stream of chunks, each starting with a 1-byte header
 *   CCCLLLLL   (CCC = command, LLLLL = length-1, 5 bits)
 * A header of 0xFF ends the stream.
 *
 * If CCC == 111 (0x07), it's a "long length" 2-byte header instead:
 *   111CCCLL LLLLLLLL   (real CCC in bits 4-2, length-1 spread across 10 bits)
 *
 * Commands:
 *   000  Direct copy      - copy (len+1) literal bytes
 *   001  Byte fill        - repeat 1 byte (len+1) times
 *   010  Word fill        - alternate 2 bytes for (len+1) total bytes
 *   011  Increasing fill  - 1 byte, incrementing by 1 each write, (len+1) times
 *   100  Repeat           - back-reference: 2-byte BIG-ENDIAN address into the
 *                           *decompressed output so far*, copy (len+1) bytes from there
 */
object Lz2 {

    fun decompress(data: ByteArray, startOffset: Int = 0): ByteArray {
        val out = ArrayList<Byte>(data.size * 2)
        var pos = startOffset

        while (true) {
            val header = data[pos++].toInt() and 0xFF
            if (header == 0xFF) break

            var cmd = (header ushr 5) and 0x07
            var length = header and 0x1F

            if (cmd == 0x07) {
                val second = data[pos++].toInt() and 0xFF
                cmd = (header ushr 2) and 0x07
                length = ((header and 0x03) shl 8) or second
            }
            val count = length + 1

            when (cmd) {
                0 -> { // direct copy
                    repeat(count) { out.add(data[pos++]) }
                }
                1 -> { // byte fill
                    val b = data[pos++]
                    repeat(count) { out.add(b) }
                }
                2 -> { // word fill
                    val b1 = data[pos++]
                    val b2 = data[pos++]
                    for (i in 0 until count) out.add(if (i % 2 == 0) b1 else b2)
                }
                3 -> { // increasing fill
                    var b = data[pos++].toInt() and 0xFF
                    repeat(count) {
                        out.add(b.toByte())
                        b = (b + 1) and 0xFF
                    }
                }
                4 -> { // repeat / back-reference (big-endian addr into output)
                    val addrHi = data[pos++].toInt() and 0xFF
                    val addrLo = data[pos++].toInt() and 0xFF
                    val addr = (addrHi shl 8) or addrLo
                    for (i in 0 until count) out.add(out[addr + i])
                }
                else -> throw IllegalStateException("Unused LZ2 command $cmd at offset $pos")
            }
        }
        return out.toByteArray()
    }
}
