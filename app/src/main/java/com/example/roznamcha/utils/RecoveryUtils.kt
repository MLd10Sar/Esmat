package com.example.roznamcha.utils

import android.content.Context
import com.example.roznamcha.SettingsManager

/**
 * A utility object to handle the logic for the Security Questions recovery method.
 */
object RecoveryUtils {

    /**
     * Verifies if an answer entered by the user matches the stored hash for that question.
     *
     * @param enteredAnswer The plain text answer the user just typed.
     * @param storedHash The securely stored hash of the correct answer from SharedPreferences.
     * @return True if the answer is correct, false otherwise.
     */
    fun isAnswerCorrect(enteredAnswer: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) {
            // If there's no stored hash, we can't verify.
            return false
        }

        // 1. Hash the answer the user just entered using the exact same method as when it was saved.
        // We trim whitespace and make it lowercase to make the check more lenient.
        // For example, "Kabul", "kabul", and " kabul " will all be treated the same.
        val enteredHash = SecurityUtils.hashPassword(enteredAnswer.trim().lowercase())

        // 2. Compare the newly generated hash with the one we saved during setup.
        return storedHash == enteredHash
    }
}