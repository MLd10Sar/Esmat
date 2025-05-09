package com.example.roznamcha.utils // Or your utils package

import java.security.MessageDigest

object SecurityUtils {

    // A simple, hardcoded salt. In a real production app, this should be unique per user
    // and stored securely, but for an offline app, this is a starting point.
    private const val PASSWORD_SALT = "Ketabat_Offline_App_Salt_!@#$"

    /**
     * Hashes a PIN using SHA-256 with a salt.
     * @param pin The 4-digit PIN to hash.
     * @return The hexadecimal string representation of the hash.
     */
    fun hashPassword(password: String): String {
        val dataToHash = PASSWORD_SALT + password // Combine PIN with salt
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(dataToHash.toByteArray(Charsets.UTF_8))
        // Convert byte array to Hex String
        return bytesToHexString(hashBytes)
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[j * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
}