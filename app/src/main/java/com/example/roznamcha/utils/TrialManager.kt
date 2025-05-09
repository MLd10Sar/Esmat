package com.example.roznamcha.utils

import android.content.Context
import android.util.Log
import com.example.roznamcha.SettingsManager

object TrialManager {

    const val TRIAL_TRANSACTION_LIMIT = 5

    fun isTrialOver(context: Context): Boolean {
        // This function's logic is still correct. It just reads values.
        val isActivated = SettingsManager.isAccessGranted(context)
        val transactionCount = SettingsManager.getTransactionCount(context)

        Log.d("TrialDebug", "TrialManager: Checking if trial is over...")
        Log.d("TrialDebug", "--> Is Activated? $isActivated")
        Log.d("TrialDebug", "--> Transaction Count: $transactionCount / $TRIAL_TRANSACTION_LIMIT")

        val result = !isActivated && transactionCount >= TRIAL_TRANSACTION_LIMIT
        Log.d("TrialDebug", "--> Conclusion: Is Trial Over? $result")
        return result
    }
}