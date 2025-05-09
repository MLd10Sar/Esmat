package com.example.roznamcha.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.roznamcha.R
import com.example.roznamcha.data.db.entity.InventoryItem
import com.example.roznamcha.databinding.FragmentAddEditInventoryItemBinding
import java.util.*

class AddEditInventoryItemFragment : Fragment() {

    private var _binding: FragmentAddEditInventoryItemBinding? = null
    private val binding get() = _binding!!

    // Use the new, dedicated ViewModel
    private val viewModel: AddEditInventoryItemViewModel by viewModels {
        AddEditInventoryItemViewModelFactory(requireActivity().application)
    }

    // Safely get the itemId argument passed from the list screen
    private val args: AddEditInventoryItemFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditInventoryItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUnitSpinner()
        setupClickListeners()
        setupObservers()

        // Change screen title and load data if we are editing
        if (args.itemId != -1L) {
            requireActivity().title = "ویرایش جنس"
            // Tell the ViewModel to load the existing item's data
            viewModel.loadItem(args.itemId)
        } else {
            requireActivity().title = "اضافه کردن جنس جدید"
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveInventoryItem.setOnClickListener {
            saveItem()
        }
    }

    private fun setupUnitSpinner() {
        val quantityUnits = resources.getStringArray(R.array.quantity_units)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, quantityUnits)
        (binding.spinnerUnit as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    // --- THE CRITICAL FIX IS HERE ---
    private fun setupObservers() {
        // Observe the LiveData for the loaded item
        viewModel.loadedItem.observe(viewLifecycleOwner) { loadedItem ->
            // When the item data arrives from the database, populate the form
            loadedItem?.let {
                populateForm(it)
            }
        }
    }

    /**
     * Fills the UI form fields with data from an InventoryItem object.
     */
    private fun populateForm(item: InventoryItem) {
        binding.etItemName.setText(item.name)
        binding.etInitialQuantity.setText(item.quantity.toString())
        binding.etPurchasePrice.setText(item.purchasePrice?.toString() ?: "")
        binding.etSalePrice.setText(item.salePrice?.toString() ?: "")
        binding.etRemarks.setText(item.remarks ?: "")

        // Set the text of the unit dropdown, not just the selection
        (binding.spinnerUnit as? AutoCompleteTextView)?.setText(item.unit ?: "", false)
    }

    /**
     * Gathers data from the form, validates it, and tells the ViewModel to save.
     */
    private fun saveItem() {
        val name = binding.etItemName.text.toString().trim()
        val quantity = binding.etInitialQuantity.text.toString().toDoubleOrNull()
        val purchasePrice = binding.etPurchasePrice.text.toString().toDoubleOrNull()
        val salePrice = binding.etSalePrice.text.toString().toDoubleOrNull()
        val remarks = binding.etRemarks.text.toString().trim()
        val unit = binding.spinnerUnit.text.toString().ifBlank { null }

        // --- Validation ---
        if (name.isBlank()) {
            binding.tilItemName.error = "نام جنس الزامی است"
            return
        } else {
            binding.tilItemName.error = null
        }
        if (quantity == null || quantity < 0) {
            binding.tilInitialQuantity.error = "مقدار اولیه معتبر وارد کنید"
            return
        } else {
            binding.tilInitialQuantity.error = null
        }

        // --- Create InventoryItem Object and Save ---
        val itemToSave = InventoryItem(
            id = if (args.itemId == -1L) 0L else args.itemId, // Use 0 for new item
            name = name,
            quantity = quantity,
            unit = unit,
            purchasePrice = purchasePrice,
            salePrice = salePrice,
            remarks = remarks.ifEmpty { null }
        )

        // Call the ViewModel to handle the database operation
        viewModel.saveItem(itemToSave)

        Toast.makeText(context, "جنس با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack() // Go back to the list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}