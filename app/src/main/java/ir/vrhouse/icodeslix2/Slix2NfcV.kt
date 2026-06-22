package ir.vrhouse.icodeslix2

import android.nfc.Tag
import android.nfc.tech.NfcV
import java.io.Closeable
import java.io.IOException
import kotlin.experimental.xor

class Slix2NfcV private constructor(
    private val tech: NfcV,
    private val uid: ByteArray,
    private val logger: (String) -> Unit
) : Closeable {

    companion object {
        fun connect(tag: Tag, logger: (String) -> Unit): Slix2NfcV {
            val nfcV = NfcV.get(tag) ?: throw IOException("این تگ NFC-V / ISO15693 نیست.")
            nfcV.connect()
            return Slix2NfcV(nfcV, tag.id, logger)
        }
    }

    fun uidRaw(): ByteArray = uid.copyOf()

    fun uidHumanReadable(): String = HexUtils.reversedHex(uid)

    fun writeSingleBlock(block: Int, data: ByteArray) {
        require(data.size == 4) { "Each ICODE SLIX2 block must be exactly 4 bytes." }
        val command = byteArrayOf(0x60.toByte(), 0x21.toByte()) + uid + byteArrayOf(block.toByte()) + data
        val response = transceiveChecked("Write Single Block ${HexUtils.blockLabel(block)}", command)
        requireNoPayload(response, "Write Single Block ${HexUtils.blockLabel(block)}")
    }

    fun readSingleBlock(block: Int): ByteArray {
        val command = byteArrayOf(0x22.toByte(), 0x20.toByte()) + uid + byteArrayOf(block.toByte())
        val response = transceiveChecked("Read Single Block ${HexUtils.blockLabel(block)}", command)
        if (response.size < 5) {
            throw IOException("Read response for block ${HexUtils.blockLabel(block)} is too short: ${HexUtils.toHex(response)}")
        }
        return response.copyOfRange(1, 5)
    }

    fun getRandomNumber(): ByteArray {
        val command = customCommand(0x22, 0xB2)
        val response = transceiveChecked("Get Random Number", command)
        if (response.size < 3) {
            throw IOException("Get Random Number response is too short: ${HexUtils.toHex(response)}")
        }
        // Android returns the ISO15693 response flags as byte 0; random number is the next 2 bytes.
        val random = response.copyOfRange(1, 3)
        logger("RN = ${HexUtils.toHex(random)}")
        return random
    }

    fun presentPassword(passwordId: Int, password: ByteArray) {
        require(password.size == 4) { "Password must be 4 bytes." }
        val random = getRandomNumber()
        val xorPassword = xorPasswordWithRandom(password, random)
        val command = customCommand(0x22, 0xB3, byteArrayOf(passwordId.toByte()) + xorPassword)
        val response = transceiveChecked("Present Password id=0x%02X".format(passwordId), command)
        requireNoPayload(response, "Present Password id=0x%02X".format(passwordId))
    }

    fun writePassword(passwordId: Int, newPassword: ByteArray) {
        require(newPassword.size == 4) { "Password must be 4 bytes." }
        val command = customCommand(0x22, 0xB4, byteArrayOf(passwordId.toByte()) + newPassword)
        val response = transceiveChecked("Write Password id=0x%02X".format(passwordId), command)
        requireNoPayload(response, "Write Password id=0x%02X".format(passwordId))
    }

    fun writeAccessCondition() {
        val command = customCommand(0x22, 0xB6, byteArrayOf(0x00.toByte(), 0x11.toByte()))
        val response = transceiveChecked("Write access condition 00 11", command)
        requireNoPayload(response, "Write access condition")
    }

    fun passwordProtect64Bit() {
        val command = customCommand(0x22, 0xBB)
        val response = transceiveChecked("64-bit Password Protect", command)
        requireNoPayload(response, "64-bit Password Protect")
    }

    fun writeDsfid(value: Int) {
        val command = byteArrayOf(0x22.toByte(), 0x29.toByte()) + uid + byteArrayOf(value.toByte())
        val response = transceiveChecked("Write DSFID 0x%02X".format(value), command)
        requireNoPayload(response, "Write DSFID")
    }

    private fun customCommand(flags: Int, commandCode: Int, tail: ByteArray = byteArrayOf()): ByteArray =
        byteArrayOf(flags.toByte(), commandCode.toByte(), IcodeSlix2Config.MANUFACTURER_NXP.toByte()) + uid + tail

    private fun xorPasswordWithRandom(password: ByteArray, random: ByteArray): ByteArray {
        if (random.size != 2) throw IOException("Random number must be 2 bytes.")
        return byteArrayOf(
            password[0] xor random[0],
            password[1] xor random[1],
            password[2] xor random[0],
            password[3] xor random[1]
        )
    }

    private fun transceiveChecked(label: String, command: ByteArray): ByteArray {
        logger("→ $label: ${HexUtils.toHex(command)}")
        val response = try {
            tech.transceive(command)
        } catch (e: IOException) {
            throw IOException("NFC transceive failed at $label: ${e.message}", e)
        }
        logger("← $label: ${HexUtils.toHex(response)}")
        if (response.isEmpty()) throw IOException("Empty response at $label")
        val flags = response[0].toInt() and 0xFF
        if ((flags and 0x01) != 0) {
            val code = response.getOrNull(1)?.toInt()?.and(0xFF) ?: -1
            throw IOException("Tag returned ISO15693 error at $label. Error code: 0x%02X, response: %s".format(code, HexUtils.toHex(response)))
        }
        return response
    }

    private fun requireNoPayload(response: ByteArray, label: String) {
        if (response.size != 1) {
            logger("ℹ $label returned extra payload: ${HexUtils.toHex(response)}")
        }
    }

    override fun close() {
        try {
            tech.close()
        } catch (_: IOException) {
        }
    }
}
