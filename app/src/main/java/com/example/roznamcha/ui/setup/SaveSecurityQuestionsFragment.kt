package com.example.roznamcha.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentSaveSecurityQuestionsBinding

class SaveSecurityQuestionsFragment : Fragment() {

    private var _binding: FragmentSaveSecurityQuestionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSaveSecurityQuestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "ایجاد سوالات امنیتی"

        // Populate the dropdowns with the questions from arrays.xml
        val questions = resources.getStringArray(R.array.security_questions)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, questions)
        (binding.spinnerQuestion1 as? AutoCompleteTextView)?.setAdapter(adapter)
        (binding.spinnerQuestion2 as? AutoCompleteTextView)?.setAdapter(adapter)

        binding.btnSaveQuestions.setOnClickListener {
            saveQuestionsAndNavigate()
        }
    }

    private fun saveQuestionsAndNavigate() {
        val q1Text = binding.spinnerQuestion1.text.toString()
        val a1Text = binding.etAnswer1.text.toString().trim()
        val q2Text = binding.spinnerQuestion2.text.toString()
        val a2Text = binding.etAnswer2.text.toString().trim()

        // Validation
        if (a1Text.isBlank() || a2Text.isBlank()) {
            Toast.makeText(context, "لطفاً به هر دو سوال جواب دهید.", Toast.LENGTH_SHORT).show()
            return
        }
        if (q1Text == q2Text) {
            Toast.makeText(context, "لطفاً دو سوال متفاوت انتخاب کنید.", Toast.LENGTH_SHORT).show()
            return
        }

        val questionsArray = resources.getStringArray(R.array.security_questions)
        val q1Index = questionsArray.indexOf(q1Text)
        val q2Index = questionsArray.indexOf(q2Text)

        if (q1Index == -1 || q2Index == -1) {
            Toast.makeText(context, "لطفاً سوالات را از لیست انتخاب کنید.", Toast.LENGTH_SHORT).show()
            return
        }

        // Save the questions and hashed answers
        SettingsManager.saveSecurityQuestions(requireContext(), q1Index, a1Text, q2Index, a2Text)

        // Mark that the full setup process is complete
        SettingsManager.markSetupCompleted(requireContext(), true)

        Toast.makeText(context, "سوالات امنیتی با موفقیت ذخیره شد.", Toast.LENGTH_SHORT).show()

        // <<< THIS IS THE CRITICAL FIX >>>
        // After setting security questions, navigate DIRECTLY to the dashboard.
        // The user is now fully set up and in Trial Mode.
        findNavController().navigate(R.id.action_saveSecurityQuestionsFragment_to_dashboardFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}