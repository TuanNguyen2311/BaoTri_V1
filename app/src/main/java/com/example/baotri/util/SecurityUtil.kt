package com.example.baotri.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtil {

    // ── Hashing ─────────────────────────────────────────────
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyHash(input: String, hash: String): Boolean {
        return sha256(input) == hash
    }

    // ── AES-256 encryption for backup ───────────────────────
    // Fixed key derived from app signature — embedded in app
    private val AES_KEY = "BaoTriThietBi2026SecretKey32Byte".toByteArray(Charsets.UTF_8)
    private val AES_IV  = "BaoTri16ByteIV!!".toByteArray(Charsets.UTF_8)
    private const val CIPHER_ALGO = "AES/CBC/PKCS5Padding"

    fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val keySpec = SecretKeySpec(AES_KEY, "AES")
        val ivSpec  = IvParameterSpec(AES_IV)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(plainText)
    }

    fun decrypt(cipherBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val keySpec = SecretKeySpec(AES_KEY, "AES")
        val ivSpec  = IvParameterSpec(AES_IV)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(cipherBytes)
    }

    fun encryptToBase64(plainText: String): String {
        val encrypted = encrypt(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decryptFromBase64(base64: String): String {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        return String(decrypt(bytes), Charsets.UTF_8)
    }

    // ── Checksum for backup integrity ───────────────────────
    fun checksum(data: ByteArray): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(data)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
