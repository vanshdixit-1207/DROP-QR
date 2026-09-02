package com.example.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val AES_KEY_LENGTH_BYTES = 32 // 256-bit key

    private val secureRandom = SecureRandom()

    /**
     * Generates a secure random 256-bit AES key.
     */
    fun generateKey(): ByteArray {
        val key = ByteArray(AES_KEY_LENGTH_BYTES)
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * Derives a 256-bit AES key from a user-supplied password / PIN with key stretching.
     */
    fun derivePasswordKey(password: String, salt: String = "DropQR-AirGap-UserPassword-v1"): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        var key = digest.digest(password.toByteArray(Charsets.UTF_8))
        for (i in 0 until 5000) {
            digest.reset()
            key = digest.digest(key)
        }
        return key
    }

    /**
     * Derives a 256-bit AES key from a transfer ID and salt.
     */
    fun deriveKey(seed: String, salt: String = "DropQR-AirGap-Salt-v1"): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        return digest.digest(seed.toByteArray(Charsets.UTF_8))
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns: [12-byte IV] + [Ciphertext + Auth Tag]
     */
    fun encryptAesGcm(plaintext: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val secretKey = SecretKeySpec(key, "AES")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertext = cipher.doFinal(plaintext)

        val output = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(ciphertext, 0, output, iv.size, ciphertext.size)
        return output
    }

    /**
     * Decrypts AES-256-GCM payload.
     * Input format: [12-byte IV] + [Ciphertext + Auth Tag]
     */
    fun decryptAesGcm(encryptedData: ByteArray, key: ByteArray): ByteArray {
        require(encryptedData.size > GCM_IV_LENGTH_BYTES) { "Invalid ciphertext length" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val ciphertext = ByteArray(encryptedData.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedData, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.size)

        val secretKey = SecretKeySpec(key, "AES")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Compresses bytes using GZIP.
     */
    fun compressGzip(data: ByteArray): ByteArray {
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { it.write(data) }
        return byteOut.toByteArray()
    }

    /**
     * Decompresses GZIP bytes with size cap for security.
     */
    fun decompressGzip(data: ByteArray, maxOutputBytes: Long = 100L * 1024 * 1024): ByteArray {
        val byteIn = ByteArrayInputStream(data)
        val byteOut = ByteArrayOutputStream()
        GZIPInputStream(byteIn).use { gzip ->
            val buffer = ByteArray(8192)
            var totalRead = 0L
            var read: Int
            while (gzip.read(buffer).also { read = it } != -1) {
                totalRead += read
                if (totalRead > maxOutputBytes) {
                    throw IllegalStateException("Decompressed size exceeds maximum allowed threshold")
                }
                byteOut.write(buffer, 0, read)
            }
        }
        return byteOut.toByteArray()
    }

    /**
     * Computes CRC32 checksum as 8-character hex string.
     */
    fun computeCrc32(data: ByteArray): String {
        val crc = CRC32()
        crc.update(data)
        return String.format("%08X", crc.value)
    }

    /**
     * Computes SHA-256 hash as hex string.
     */
    fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
