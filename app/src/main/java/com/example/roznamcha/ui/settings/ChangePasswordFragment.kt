package com.example.roznamcha.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.databinding.FragmentChangePasswordBinding // Assuming you create this layout
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.utils.SecurityUtils

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnConfirmChange.setOnClickListener { changePin() }
    }

    private fun changePin() {
        val oldPin = binding.etOldPassword.text.toString()
        val newPin1 = binding.etNewPassword.text.toString()
        val newPin2 = binding.etConfirmNewPassword.text.toString()

        // Clear previous errors
        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmNewPassword.error = null

        // 1. Get the currently saved hash
        val savedPinHash = SettingsManager.getPasswordHash(requireContext())

        // 2. Verify the old PIN is correct
        if (SecurityUtils.hashPassword(oldPin) != savedPinHash) {
            binding.tilOldPassword.error = "رمز فعلی اشتباه است"
            return
        }

        // 3. Verify the new PIN meets requirements
        if (newPin1.length < 6) {
            binding.tilNewPassword.error = "رمز جدید حداقل باید ۶ حرف باشد"
            return
        }
        // 4. Verify the new PINs match
        if (newPin1 != newPin2) {
            binding.tilConfirmNewPassword.error = "رمزهای جدید مطابقت ندارند"
            return
        }

        // 5. All checks passed, save the new hash
        val newPinHash = SecurityUtils.hashPassword(newPin1)
        SettingsManager.savePasswordHash(requireContext(), newPinHash)

        Toast.makeText(context, "رمز با موفقیت تغییر کرد", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack() // Go back to Settings screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}