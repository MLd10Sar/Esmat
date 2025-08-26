package com.example.roznamcha.ui.settings

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentShopInfoBinding
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory

class ShopInfoFragment : Fragment() {

    private var _binding: FragmentShopInfoBinding? = null
    private val binding get() = _binding!!

    // Activity Result Launcher for picking a logo image from the gallery
    private val logoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // When an image is picked, save it and update the preview
            saveLogo(it)
            binding.imgShopLogoPreview.setImageURI(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShopInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "معلومات دکان"

        loadShopInfo()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnUploadLogo.setOnClickListener {
            // Launch the system's image picker
            logoPickerLauncher.launch("image/*")
        }
        binding.imgShopLogoPreview.setOnClickListener {
            // Also allow clicking the image itself to upload
            logoPickerLauncher.launch("image/*")
        }

        binding.btnSaveShopInfo.setOnClickListener {
            saveAllInfo()
        }
    }

    /**
     * Loads existing shop info from SettingsManager and populates the form.
     */
    private fun loadShopInfo() {
        val context = requireContext()
        binding.etShopName.setText(SettingsManager.getShopName(context))
        binding.etShopAddress.setText(SettingsManager.getShopAddress(context))
        binding.etShopPhone.setText(SettingsManager.getShopPhone(context))

        // Load and display the saved logo if it exists
        val logoFile = File(context.filesDir, "shop_logo.png")
        if (logoFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                binding.imgShopLogoPreview.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("ShopInfoFragment", "Error loading existing logo", e)
            }
        }
    }

    /**
     * Saves the selected logo image URI to the app's private storage.
     */
    private fun saveLogo(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val logoFile = File(requireContext().filesDir, "shop_logo.png")
            val outputStream = FileOutputStream(logoFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            Toast.makeText(context, "لوگو با موفقیت انتخاب شد", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در ذخیره کردن لوگو", Toast.LENGTH_SHORT).show()
            Log.e("ShopInfoFragment", "Error saving logo", e)
        }
    }

    /**
     * Saves all the text fields to SettingsManager.
     */
    private fun saveAllInfo() {
        val context = requireContext()
        val shopName = binding.etShopName.text.toString().trim()

        if (shopName.isBlank()) {
            binding.tilShopName.error = "نام دکان الزامی است"
            return
        } else {
            binding.tilShopName.error = null
        }

        SettingsManager.saveShopName(context, shopName)
        SettingsManager.saveShopAddress(context, binding.etShopAddress.text.toString().trim())
        SettingsManager.saveShopPhone(context, binding.etShopPhone.text.toString().trim())

        Toast.makeText(context, "معلومات دکان با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack() // Go back to settings screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}