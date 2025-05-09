package com.example.roznamcha.ui.addedit // Or your package

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R

class TrialEndedDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // isCancelable = false ensures the user MUST make a choice.
        isCancelable = false

        return AlertDialog.Builder(requireContext())
            .setTitle("نسخه آزمایشی تمام شد")
            .setMessage("شما به حد اکثر معاملات در نسخه آزمایشی رسیده‌اید. برای ثبت معاملات نامحدود، لطفاً برنامه را فعال سازید.")
            .setPositiveButton("فعال سازی") { _, _ ->
                // The dialog is stable and can navigate safely.
                try {
                    findNavController().navigate(R.id.action_global_to_activationCodeFragment)
                } catch (e: Exception) {
                    // Handle navigation error if it occurs
                }
            }
            .setNegativeButton("بعداً", null) // 'null' listener simply dismisses the dialog
            .create()
    }
}