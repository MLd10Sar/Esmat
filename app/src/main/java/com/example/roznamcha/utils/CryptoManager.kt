package com.example.roznamcha.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// <<< REMOVED 'context' from constructor, it's not needed here >>>
class CryptoManager {

    private val secretKey: SecretKeySpec
    private val iv: IvParameterSpec

    init {
        // Derive a key and IV from a hardcoded secret.
        val password = SecurityUtils.getSalt()
        val keyBytes = password.padEnd(32, ' ').substring(0, 32).toByteArray(Charsets.UTF_8)
        val ivBytes = password.padEnd(16, ' ').substring(0, 16).toByteArray(Charsets.UTF_8)

        secretKey = SecretKeySpec(keyBytes, "AES")
        iv = IvParameterSpec(ivBytes)
    }

    // Encrypts the content of an input file and writes it to an output file.
    fun encrypt(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val output = cipher.update(buffer, 0, bytesRead)
                    if (output != null) fos.write(output)
                }
                val output = cipher.doFinal()
                if (output != null) fos.write(output)
            }
        }
    }

    // Decrypts the content of an input file and writes it to an output file.
    fun decrypt(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val output = cipher.update(buffer, 0, bytesRead)
                    if (output != null) fos.write(output)
                }
                val output = cipher.doFinal()
                if (output != null) fos.write(output)
            }
        }
    }
}