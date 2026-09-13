package com.example.smweditor.rom

import java.io.File

/**
 * Loads a SMW ROM file and handles:
 *  - Detecting the optional 512-byte copier header
 *  - Translating SNES (LoROM) addresses to file offsets and back
 *
 * SMW is a LoROM SNES cartridge. LoROM maps each 32KB bank into the
 * upper half of the SNES $8000-$FFFF address space, banks $00-$7D / $80-$FF.
 */
class SmwRom private constructor(
    val bytes: ByteArray,
    val hasCopierHeader: Boolean
) {
    companion object {
        private const val COPIER_HEADER_SIZE = 0x200

        /** For JVM-side use / unit tests, where a plain filesystem path is available. */
        fun load(path: String): SmwRom = fromBytes(File(path).readBytes())

        /**
         * For Android, where files are read via ContentResolver/SAF
         * (`contentResolver.openInputStream(uri)?.readBytes()`) rather than
         * a raw filesystem path.
         */
        fun fromBytes(raw: ByteArray): SmwRom {
            // A copier header makes the file size 512 bytes more than a
            // power-of-two-ish SNES ROM size. The simplest reliable check:
            // (size % 0x8000) == 0x200 means there's a header, plain LoROM
            // dumps are multiples of 0x8000 (32KB) with no remainder.
            val hasHeader = (raw.size % 0x8000) == COPIER_HEADER_SIZE
            return SmwRom(raw, hasHeader)
        }

        /**
         * Converts a SNES LoROM address (e.g. 0x05E000) to an *unheadered*
         * PC file offset. Add COPIER_HEADER_SIZE afterwards if the loaded
         * file has a header (see [toFileOffset]).
         */
        fun snesToPcLoRom(snesAddress: Int): Int {
            val bank = (snesAddress ushr 16) and 0xFF
            val offset = snesAddress and 0xFFFF
            return ((bank and 0x7F) shl 15) or (offset and 0x7FFF)
        }
    }

    /** SNES address -> actual byte offset inside [bytes] for this loaded file. */
    fun toFileOffset(snesAddress: Int): Int {
        val pc = snesToPcLoRom(snesAddress)
        return if (hasCopierHeader) pc + COPIER_HEADER_SIZE else pc
    }

    fun readByte(snesAddress: Int): Int = bytes[toFileOffset(snesAddress)].toInt() and 0xFF

    fun readBytes(snesAddress: Int, length: Int): ByteArray {
        val start = toFileOffset(snesAddress)
        return bytes.copyOfRange(start, start + length)
    }

    /** Reads a 3-byte little-endian SNES pointer (bank, hi, lo order as stored) as an address. */
    fun readPointer24(snesAddress: Int): Int {
        val off = toFileOffset(snesAddress)
        val lo = bytes[off].toInt() and 0xFF
        val hi = bytes[off + 1].toInt() and 0xFF
        val bank = bytes[off + 2].toInt() and 0xFF
        return (bank shl 16) or (hi shl 8) or lo
    }
}
