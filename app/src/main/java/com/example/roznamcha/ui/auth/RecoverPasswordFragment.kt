package com.example.roznamcha.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentRecoverPasswordBinding
import com.example.roznamcha.utils.RecoveryUtils

class RecoverPasswordFragment : Fragment() {

    private var _binding: FragmentRecoverPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecoverPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = "بازیابی رمز"

        displaySavedQuestions()

        binding.btnVerifyAnswers.setOnClickListener {
            verifyAnswers()
        }
    }

    private fun displaySavedQuestions() {
        val questionsArray = resources.getStringArray(R.array.security_questions)
        val q1Index = SettingsManager.getQuestion1Index(requireContext())
        val q2Index = SettingsManager.getQuestion2Index(requireContext())

        if (q1Index != -1 && q2Index != -1) {
            binding.tvQuestion1.text = questionsArray[q1Index]
            binding.tvQuestion2.text = questionsArray[q2Index]
        } else {
            // This should not happen if setup was completed.
            Toast.makeText(context, "سوالات امنیتی یافت نشد.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    private fun verifyAnswers() {
        val storedHash1 = SettingsManager.getAnswer1Hash(requireContext())
        val storedHash2 = SettingsManager.getAnswer2Hash(requireContext())
        val enteredAnswer1 = binding.etAnswer1.text.toString()
        val enteredAnswer2 = binding.etAnswer2.text.toString()

        // Use RecoveryUtils to check if answers are correct
        val isAnswer1Correct = RecoveryUtils.isAnswerCorrect(enteredAnswer1, storedHash1)
        val isAnswer2Correct = RecoveryUtils.isAnswerCorrect(enteredAnswer2, storedHash2)

        if (isAnswer1Correct && isAnswer2Correct) {
            // Success!
            SettingsManager.clearPassword(requireContext())
            Toast.makeText(context, "جوابات درست است! لطفاً رمز جدید انتخاب کنید.", Toast.LENGTH_LONG).show()
            // Navigate to the create password screen to set a new one
            findNavController().navigate(R.id.action_recoverPasswordFragment_to_createPasswordFragment)
        } else {
            // Failure
            Toast.makeText(context, "یک یا هر دو جواب اشتباه است.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}