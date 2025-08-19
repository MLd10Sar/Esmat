package com.example.roznamcha.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentSettingsBinding
import java.text.SimpleDateFormat
import java.util.*
import com.example.roznamcha.workers.ReminderWorker // Import the new worker
import com.example.roznamcha.workers.SummaryWorker // Keep the old one


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireActivity().application)
    }

    // This launcher is ONLY for selecting a file to RESTORE
    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showRestoreConfirmationDialog(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.settings)

        setupClickListeners()
        setupObservers()
        displayBackupStatus()
    }

    private fun displayBackupStatus() {
        val lastBackupTimestamp = SettingsManager.getLastBackupTimestamp(requireContext())
        if (lastBackupTimestamp == 0L) {
            binding.tvLastBackupStatus.text = "هیچ وقت بکاپ گرفته نشده است"
        } else {
            val date = Date(lastBackupTimestamp)
            val format = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US)
            binding.tvLastBackupStatus.text = "آخرین بکاپ: ${format.format(date)}"
        }
    }

    private fun setupClickListeners() {
        binding.tvChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
        }
        binding.tvAboutApp.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
        }

        binding.tvBackupData.setOnClickListener {
            showBackupOptionsDialog()
        }

        binding.tvRestoreData.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Allow user to select the .enc file
            }
            restoreLauncher.launch(intent)
        }
    }

    private fun setupObservers() {
        // Observer for when the encrypted file is ready to be shared
        viewModel.encryptedBackupReadyEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { fileUri ->
                showShareOptions(fileUri)
            }
        }

        viewModel.backupFailedEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(context, "خطا در ایجاد نسخه پشتیبان", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.restoreStatusEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(context, "اطلاعات با موفقیت بازیابی شد. برنامه مجددا راه اندازی می شود.", Toast.LENGTH_LONG).show()
                    restartApp()
                } else {
                    Toast.makeText(context, "خطا در بازیابی اطلاعات. فایل ممکن است خراب باشد.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBackupOptionsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("تهیه نسخه پشتیبان (بکاپ)")
            .setMessage("فایل بکاپ شما رمزگذاری خواهد شد. لطفاً برای امنیت بیشتر، آنرا در یک مکان امن مانند ایمیل یا واتساپ تان ذخیره کنید.")
            .setPositiveButton("ادامه") { _, _ ->
                // Tell the ViewModel to start creating the encrypted file
                viewModel.createEncryptedBackup()
                Toast.makeText(context, "در حال آماده سازی فایل بکاپ...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showShareOptions(fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream" // Generic binary file type
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Roznamcha App Backup File")
            putExtra(Intent.EXTRA_TEXT, "This is your encrypted Roznamcha backup file. Keep it safe!")
            // Grant read permission to the receiving app
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "ارسال بکاپ از طریق..."))
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

    private fun restartApp() {
        val context = requireContext()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let {
            context.startActivity(it)
            requireActivity().finishAffinity()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}