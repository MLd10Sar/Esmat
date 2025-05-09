package com.example.roznamcha.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentActivationCodeBinding // Use this binding class

class ActivationCodeFragment : Fragment() {

    private val TAG = "ActivationFragment"
    private var _binding: FragmentActivationCodeBinding? = null
    private val binding get() = _binding!!

    // Initialize the ViewModel
    private val viewModel: ActivationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivationCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnVerifyCode.setOnClickListener { verifyCode() }
        binding.etAccessCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyCode()
                true
            } else { false }
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarActivation.isVisible = isLoading
            binding.btnVerifyCode.isEnabled = !isLoading
            binding.etAccessCode.isEnabled = !isLoading
        }

        viewModel.activationResultEvent.observe(viewLifecycleOwner) { event ->
            // Use getContentIfNotHandled() to ensure the event is consumed only once
            event.getContentIfNotHandled()?.let { (isSuccess, message) ->
                if (isSuccess) {
                    // --- THE CRITICAL FIX IS HERE ---
                    // 1. Mark access as permanently granted in SharedPreferences.
                    SettingsManager.markAccessGranted(requireContext(), true)
                    Log.d(TAG, "Activation successful! isAccessGranted flag is now TRUE.")

                    // 2. Show success message.
                    Toast.makeText(context, message ?: getString(R.string.activation_successful), Toast.LENGTH_LONG).show()

                    // 3. Navigate to the dashboard.
                    try {
                        findNavController().navigate(R.id.action_activationCodeFragment_to_dashboardFragment)
                    } catch (e: Exception) {
                        Log.e(TAG, "Navigation failed after activation", e)
                    }
                } else {
                    // FAILURE: Show error message from backend
                    binding.tilAccessCode.error = message
                }
            }
        }
    }

    private fun restartApp() {
        val context = activity?.applicationContext ?: return
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            val componentName = it.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            // Kill the current process
            Runtime.getRuntime().exit(0)
        }
    }

    private fun verifyCode() {
        val enteredCode = binding.etAccessCode.text.toString()
        binding.tilAccessCode.error = null // Clear previous error
        viewModel.attemptActivation(enteredCode) // Trigger the ViewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}