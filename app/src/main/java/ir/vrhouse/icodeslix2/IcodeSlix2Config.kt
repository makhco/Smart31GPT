package ir.vrhouse.icodeslix2

object IcodeSlix2Config {
    const val MANUFACTURER_NXP: Int = 0x04

    const val READ_PASSWORD_ID: Int = 0x01
    const val WRITE_PASSWORD_ID: Int = 0x02

    val DEFAULT_READ_KEY: ByteArray = HexUtils.fromHex("00000000")
    val DEFAULT_WRITE_KEY: ByteArray = HexUtils.fromHex("00000000")

    val TARGET_READ_KEY: ByteArray = HexUtils.fromHex("CF8B1B78")
    val TARGET_WRITE_KEY: ByteArray = HexUtils.fromHex("F1B32A1D")

    /**
     * Only these blocks are written. Blocks 05..47 are intentionally not touched.
     * Block numbers are interpreted as decimal block indexes from the user's list.
     */
    val TARGET_BLOCKS: LinkedHashMap<Int, ByteArray> = linkedMapOf(
        0 to HexUtils.fromHex("43545633"),
        1 to HexUtils.fromHex("32323032"),
        2 to HexUtils.fromHex("38303731"),
        3 to HexUtils.fromHex("4D530100"),
        4 to HexUtils.fromHex("0903FA00"),
        48 to HexUtils.fromHex("243AA753"),
        49 to HexUtils.fromHex("34E5001D"),
        50 to HexUtils.fromHex("E83C3EBC"),
        51 to HexUtils.fromHex("0CB45089"),
        52 to HexUtils.fromHex("554960A4"),
        53 to HexUtils.fromHex("8DD2A161"),
        54 to HexUtils.fromHex("57916A28"),
        55 to HexUtils.fromHex("CCBCA88D"),
        56 to HexUtils.fromHex("6AB78B79"),
        57 to HexUtils.fromHex("3A3E7722"),
        58 to HexUtils.fromHex("80545140"),
        59 to HexUtils.fromHex("A2E1E6F0"),
        60 to HexUtils.fromHex("92E266C2"),
        61 to HexUtils.fromHex("881A2DDB"),
        62 to HexUtils.fromHex("7D2FFDBE"),
        63 to HexUtils.fromHex("590B96F6")
    )
}
