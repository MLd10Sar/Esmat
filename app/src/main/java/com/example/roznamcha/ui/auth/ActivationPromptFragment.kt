package com.example.roznamcha.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.databinding.FragmentActivationPromptBinding

class ActivationPromptFragment : Fragment() {

    private var _binding: FragmentActivationPromptBinding? = null
    private val binding get() = _binding!!

    // Your contact number
    private val supportPhoneNumber = "+93773140505"
    private val supportWhatsAppNumber = "+93785444401"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivationPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prevent the user from simply pressing "back" to bypass this screen
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing, or show a toast "Please activate to continue"
                Toast.makeText(context, "برای ادامه، برنامه را فعال سازید.", Toast.LENGTH_SHORT).show()
            }
        })

        // Navigate to the screen where they can enter the code
        binding.btnGoToActivation.setOnClickListener {
            findNavController().navigate(R.id.action_activationPromptFragment_to_activationCodeFragment)
        }

        // Open WhatsApp with a pre-filled message
        binding.btnContactWhatsapp.setOnClickListener {
            openWhatsAppSupport()
        }

        // Open the phone dialer
        binding.btnContactCall.setOnClickListener {
            openPhoneDialer()
        }
    }

    private fun openWhatsAppSupport() {
        val message = "سلام، من میخواهم برنامه «روزنامچه» را فعال سازم. لطفاً رهنمایی کنید."
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$supportWhatsAppNumber&text=${Uri.encode(message)}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "واتساپ نصب نیست.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhoneDialer() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$supportPhoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "برنامه تلفن یافت نشد.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}