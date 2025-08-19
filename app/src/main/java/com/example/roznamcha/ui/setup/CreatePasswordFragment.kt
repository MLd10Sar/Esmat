package com.example.roznamcha.ui.setup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentCreatePasswordBinding
import com.example.roznamcha.utils.SecurityUtils

class CreatePasswordFragment : Fragment() {

    private var _binding: FragmentCreatePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSavePassword.setOnClickListener { savePasswordAndProceed() }
    }

    private fun savePasswordAndProceed() {
        val pass1 = binding.etCreatePassword.text.toString()
        val pass2 = binding.etConfirmPassword.text.toString()

        binding.tilCreatePassword.error = null
        binding.tilConfirmPassword.error = null

        if (pass1.length < 6) {
            binding.tilCreatePassword.error = "رمز عبور باید حداقل ۶ حرف باشد"
            return
        }
        if (pass1 != pass2) {
            binding.tilConfirmPassword.error = "رمزها مطابقت ندارند"
            return
        }

        // Hash and save the password
        val passwordHash = SecurityUtils.hashPassword(pass1)
        SettingsManager.savePasswordHash(requireContext(), passwordHash)
        Log.d("CreatePasswordFragment", "Password hash saved successfully.")

        // --- THE CRITICAL FIX ---
        // After creating a password, navigate to the security questions screen.
        try {
            Log.d("CreatePasswordFragment", "Navigating to SaveSecurityQuestionsFragment...")
            findNavController().navigate(R.id.action_createPasswordFragment_to_saveSecurityQuestionsFragment)
        } catch (e: Exception) {
            Log.e("CreatePasswordFragment", "Navigation to Security Questions failed!", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}