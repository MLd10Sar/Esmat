package com.example.roznamcha.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentPasswordLockBinding // <<< ENSURE BINDING NAME IS CORRECT
import com.example.roznamcha.utils.SecurityUtils

// <<< CORRECT CLASS NAME >>>
class PasswordLockFragment : Fragment() {

    private var _binding: FragmentPasswordLockBinding? = null // <<< ENSURE BINDING NAME IS CORRECT
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasswordLockBinding.inflate(inflater, container, false) // <<< ENSURE BINDING NAME IS CORRECT
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Assuming your button ID in fragment_password_lock.xml is btnUnlock
        binding.btnUnlock.setOnClickListener { unlockApp() }
        // Assuming your EditText ID is etPasswordLock
        binding.etPasswordLock.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                unlockApp()
                true
            } else { false }
        }
    }

    private fun unlockApp() {
        // Assuming your EditText ID is etPasswordLock
        val enteredPin = binding.etPasswordLock.text.toString()
        val savedPinHash = SettingsManager.getPasswordHash(requireContext())

        // Assuming your TextInputLayout ID is tilPasswordLock
        binding.tilPasswordLock.error = null

        if (savedPinHash == null) {
            // Fallback: This assumes the action from the lock screen to create pin screen exists
            // and is named action_passwordLockFragment_to_createPinFragment
            findNavController().navigate(R.id.action_passwordLockFragment_to_createPasswordFragment)
            return
        }

        if (SecurityUtils.hashPassword(enteredPin) == savedPinHash) {
            // --- THE FIX IS HERE ---
            // Before navigating, check if we are still on the PasswordLockFragment.
            // This prevents a crash if the user double-taps the button.
            // <<< USE THE CORRECT FRAGMENT ID FROM NAV_GRAPH >>>
            if (findNavController().currentDestination?.id == R.id.passwordLockFragment) {
                // <<< USE THE CORRECT ACTION ID FROM NAV_GRAPH >>>
                findNavController().navigate(R.id.action_passwordLockFragment_to_dashboardFragment)
            }
        } else {
            binding.tilPasswordLock.error = "رمز اشتباه است"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}