package com.example.roznamcha.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentDisclaimerBinding

class DisclaimerFragment : Fragment() {

    private var _binding: FragmentDisclaimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = getString(R.string.title_disclaimer)

        // Enable the continue button only when the checkbox is checked
        binding.cbAgree.setOnCheckedChangeListener { _, isChecked ->
            binding.btnContinue.isEnabled = isChecked
        }

        binding.btnContinue.setOnClickListener {
            // Mark that the user has accepted the disclaimer
            SettingsManager.markDisclaimerAccepted(requireContext(), true)
            // Navigate to the next step in the setup process
            findNavController().navigate(R.id.action_disclaimerFragment_to_currencySelectionFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}