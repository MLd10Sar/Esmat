package com.example.roznamcha.utils

import java.security.MessageDigest

object SecurityUtils {

    private const val SALT = "Roznamcha_Super_Secret_Salt_12345"

    fun hashPassword(password: String): String {
        val dataToHash = SALT + password
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(dataToHash.toByteArray(Charsets.UTF_8))
        return bytesToHexString(hashBytes)
    }

    fun getSalt(): String {
        return SALT
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF".toCharArray()
        val result = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            result[j * 2] = hexChars[v ushr 4]
            result[j * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(result)
    }
}