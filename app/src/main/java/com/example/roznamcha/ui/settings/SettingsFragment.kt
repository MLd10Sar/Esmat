package com.example.roznamcha.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.databinding.FragmentSettingsBinding
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.roznamcha.workers.SummaryWorker

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireActivity().application)
    }

    // --- Activity Result Launchers ---
    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.backupDatabase(uri)
            }
        }
    }

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showRestoreConfirmationDialog(uri)
            }
        }
    }

    // --- Fragment Lifecycle ---
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvChangePin.setOnClickListener {
            try {
                // This action must exist in nav_graph.xml, from settingsFragment to changePinFragment
                findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Navigation to Change PIN failed", e)
                Toast.makeText(context, "Cannot open Change PIN screen", Toast.LENGTH_SHORT).show()
            }
        }
        setupClickListeners()
        binding.tvAboutApp.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
        }
        setupObservers()
    }

    private fun setupClickListeners() {
        // Assume you have a tvChangePin, handle it here
        // binding.tvChangePin.setOnClickListener { ... }


        binding.tvBackupData.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "ketabat_backup_${System.currentTimeMillis()}.db")
            }
            backupLauncher.launch(intent)
        }

        binding.tvRestoreData.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Allow selection of any file type
            }
            restoreLauncher.launch(intent)
        }
    }

    private fun setupObservers() {
        viewModel.backupStatus.observe(viewLifecycleOwner) { success ->
            val message = if (success) "نسخه پشتیبان با موفقیت ایجاد شد" else "خطا در ایجاد نسخه پشتیبان"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        viewModel.restoreStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "اطلاعات با موفقیت بازیابی شد. برنامه مجددا راه اندازی می شود.", Toast.LENGTH_LONG).show()
                restartApp() // Call helper function for clarity
            } else {
                Toast.makeText(context, "خطا در بازیابی اطلاعات", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRestoreConfirmationDialog(backupUri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("تایید بازیابی اطلاعات")
            .setMessage("هشدار: تمام اطلاعات فعلی شما حذف و با اطلاعات نسخه پشتیبان جایگزین خواهد شد. آیا مطمئن هستید؟")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("بله، بازیابی کن") { _, _ ->
                viewModel.restoreDatabase(backupUri)
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    // --- Helper function to restart the app ---
    private fun restartApp() {
        // Use a safe call `.let` to only execute if the intent is not null
        val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
        intent?.let { // <<< THIS IS THE FIX
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 'it' refers to the non-null intent
            startActivity(it)
            requireActivity().finish() // Close the current activity instance
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}