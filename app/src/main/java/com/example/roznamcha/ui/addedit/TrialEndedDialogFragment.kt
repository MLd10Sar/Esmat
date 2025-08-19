package com.example.roznamcha.ui.addedit // Or your package

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R

class TrialEndedDialogFragment : DialogFragment() {

    // In TrialEndedDialogFragment.kt

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return AlertDialog.Builder(requireContext())
            .setTitle("نسخه آزمایشی تمام شد")
            .setMessage("شما به حد اکثر معاملات در نسخه آزمایشی رسیده‌اید. برای ثبت معاملات نامحدود، لطفاً برنامه را فعال سازید.")
            .setPositiveButton("فعال سازی") { _, _ ->
                try {
                    // <<< THIS IS THE ONLY CHANGE NEEDED >>>
                    // Instead of going to the code entry screen, go to the helpful prompt screen.
                    findNavController().navigate(R.id.action_global_to_activationPromptFragment)
                } catch (e: Exception) {
                    Log.e("TrialEndedDialog", "Navigation to Activation Prompt failed", e)
                }
            }
            .setNegativeButton("بعداً", null)
            .create()
    }
}