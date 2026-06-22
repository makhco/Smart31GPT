package ir.vrhouse.icodeslix2

import java.util.Locale

object HexUtils {
    fun fromHex(hex: String): ByteArray {
        val cleaned = hex.replace(" ", "").replace(":", "").replace("-", "").trim()
        require(cleaned.length % 2 == 0) { "Hex length must be even: $hex" }
        return ByteArray(cleaned.length / 2) { index ->
            cleaned.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    fun toHex(bytes: ByteArray, separator: String = " "): String =
        bytes.joinToString(separator) { "%02X".format(Locale.US, it.toInt() and 0xFF) }

    fun reversedHex(bytes: ByteArray, separator: String = " "): String =
        toHex(bytes.reversedArray(), separator)

    fun blockLabel(block: Int): String = "%02d / 0x%02X".format(Locale.US, block, block)
}
