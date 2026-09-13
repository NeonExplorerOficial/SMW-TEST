package com.example.smweditor.rom

/**
 * SMW level pointer tables (documented format, vanilla ROM layout):
 *
 *   $05E000  Layer 1 data pointers   - 0x200 levels, 3 bytes each (SNES pointer)
 *   $05E600  Layer 2 data pointers   - 0x200 levels, 3 bytes each
 *                                      (if high byte == 0xFF -> layer 2 is a
 *                                       background tilemap, not object data)
 *   $05EC00  Sprite data pointers    - 0x200 levels, 2 bytes each, bank fixed to $07
 *
 * The first 5 bytes pointed to by the Layer 1 pointer are the level's
 * "primary header":
 *   Byte1: BBBLLLLL   BBB=BG palette, LLLLL=level length (screens)
 *   Byte2: CCCOOOOO   CCC=BG color,   OOOOO=level mode
 *   Byte3: 3MMMSSSS   3=layer3 priority, MMM=music, SSSS=sprite set
 *   Byte4: TTPPPFFF   TT=time, PPP=sprite palette, FFF=FG palette
 *   Byte5: IIVVZZZZ   II=item memory, VV=vertical scroll, ZZZZ=tileset
 */
object LevelTables {
    const val LAYER1_POINTERS: Int = 0x05E000
    const val LAYER2_POINTERS: Int = 0x05E600
    const val SPRITE_POINTERS: Int = 0x05EC00
    const val LEVEL_COUNT: Int = 0x200
}

data class PrimaryHeader(
    val bgPalette: Int,
    val levelLengthScreens: Int,
    val bgColor: Int,
    val levelMode: Int,
    val layer3Priority: Boolean,
    val music: Int,
    val spriteSet: Int,
    val time: Int,
    val spritePalette: Int,
    val fgPalette: Int,
    val itemMemory: Int,
    val verticalScroll: Int,
    val tileset: Int
) {
    companion object {
        fun parse(b: ByteArray): PrimaryHeader {
            require(b.size >= 5) { "Primary header needs 5 bytes" }
            val b1 = b[0].toInt() and 0xFF
            val b2 = b[1].toInt() and 0xFF
            val b3 = b[2].toInt() and 0xFF
            val b4 = b[3].toInt() and 0xFF
            val b5 = b[4].toInt() and 0xFF
            return PrimaryHeader(
                bgPalette = (b1 ushr 5) and 0x07,
                levelLengthScreens = b1 and 0x1F,
                bgColor = (b2 ushr 5) and 0x07,
                levelMode = b2 and 0x1F,
                layer3Priority = (b3 and 0x80) != 0,
                music = (b3 ushr 4) and 0x07,
                spriteSet = b3 and 0x0F,
                time = (b4 ushr 6) and 0x03,
                spritePalette = (b4 ushr 3) and 0x07,
                fgPalette = b4 and 0x07,
                itemMemory = (b5 ushr 6) and 0x03,
                verticalScroll = (b5 ushr 4) and 0x03,
                tileset = b5 and 0x0F
            )
        }
    }
}

class LevelReader(private val rom: SmwRom) {

    /** Resolves and decompresses the raw Layer 1 block for [levelNumber] (0..0x1FF). */
    fun readLayer1Raw(levelNumber: Int): ByteArray {
        require(levelNumber in 0 until LevelTables.LEVEL_COUNT) { "Level number out of range" }
        val pointerAddr = LevelTables.LAYER1_POINTERS + levelNumber * 3
        val target = rom.readPointer24(pointerAddr)
        // Layer 1 data is LZ2-compressed starting right at the target address.
        val compressed = rom.bytes.copyOfRange(
            rom.toFileOffset(target),
            rom.bytes.size // decompressor stops itself at the 0xFF terminator
        )
        return Lz2.decompress(compressed)
    }

    fun readPrimaryHeader(levelNumber: Int): PrimaryHeader {
        val layer1 = readLayer1Raw(levelNumber)
        return PrimaryHeader.parse(layer1.copyOfRange(0, 5))
    }

    /** Object bytes after the 5-byte primary header, up to (not including) the 0xFF level-end marker. */
    fun readObjectBytes(levelNumber: Int): ByteArray {
        val layer1 = readLayer1Raw(levelNumber)
        return layer1.copyOfRange(5, layer1.size)
    }
}
