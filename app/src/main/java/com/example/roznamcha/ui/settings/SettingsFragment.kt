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
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentSettingsBinding
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireActivity().application)
    }

    // Launcher for selecting a backup file to RESTORE
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
        loadCurrentDateFormat()

        binding.rgDateFormat.setOnCheckedChangeListener { _, checkedId ->
            val selectedFormat = if (checkedId == R.id.rbShamsi) "SHAMSI" else "GREGORIAN"
            SettingsManager.saveDateFormat(requireContext(), selectedFormat)
            Toast.makeText(context, "Date format updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentDateFormat() {
        val currentFormat = SettingsManager.getDateFormat(requireContext())
        if (currentFormat == "SHAMSI") {
            binding.rgDateFormat.check(R.id.rbShamsi)
        } else {
            binding.rgDateFormat.check(R.id.rbGregorian)
        }
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

        // <<< THIS IS THE CORRECT LISTENER >>>
        // It navigates to the dedicated ShopInfoFragment.
        binding.tvEditShopInfo.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settingsFragment_to_shopInfoFragment)
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Navigation to ShopInfoFragment failed. Check nav_graph.", e)
                Toast.makeText(context, "Could not open shop info page.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRestoreData.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            restoreLauncher.launch(intent)
        }
    }

    private fun setupObservers() {
        viewModel.encryptedBackupReadyEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { fileUri ->
                showShareOptions(fileUri)
                displayBackupStatus() // Update the "Last backup" text after a successful backup
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
                viewModel.createEncryptedBackup()
                Toast.makeText(context, "در حال آماده سازی فایل بکاپ...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showShareOptions(fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Roznamcha App Backup File")
            putExtra(Intent.EXTRA_TEXT, "This is your encrypted Roznamcha backup file. Keep it safe!")
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