package com.example.roznamcha.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.databinding.FragmentShopInfoBinding
import java.io.File

class ShopInfoFragment : Fragment() {

    private var _binding: FragmentShopInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShopInfoViewModel by viewModels {
        ShopInfoViewModelFactory(requireActivity().application)
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { imageUri ->
                binding.imgShopLogoPreview.setImageURI(imageUri) // Show preview
                viewModel.saveShopLogo(imageUri) // Save logo in background
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShopInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = "معلومات دکان"

        loadExistingInfo()

        binding.btnUploadLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        binding.btnSaveShopInfo.setOnClickListener {
            val name = binding.etShopName.text.toString().trim()
            val address = binding.etShopAddress.text.toString().trim()
            val phone = binding.etShopPhone.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(context, "نام دکان الزامی است", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveShopInfo(name, address, phone)
            Toast.makeText(context, "معلومات ذخیره شد", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun loadExistingInfo() {
        binding.etShopName.setText(SettingsManager.getShopName(requireContext()) ?: "")
        binding.etShopAddress.setText(SettingsManager.getShopAddress(requireContext()) ?: "")
        val logoFile = File(requireContext().filesDir, "shop_logo.png")
        if (logoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            binding.imgShopLogoPreview.setImageBitmap(bitmap)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}