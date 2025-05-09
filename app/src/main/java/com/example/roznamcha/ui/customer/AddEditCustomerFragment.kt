package com.example.roznamcha.ui.customer // Adjust package

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
// import androidx.navigation.fragment.navArgs // Only if using Safe Args
import com.example.roznamcha.R
import com.example.roznamcha.data.db.entity.Customer // Import Customer
import com.example.roznamcha.databinding.FragmentAddEditCustomerBinding // Use correct binding

class AddEditCustomerFragment : Fragment() {

    private val TAG = "AddEditCustomerFrag"
    private var _binding: FragmentAddEditCustomerBinding? = null
    private val binding get() = _binding!!

    // private val args: AddEditCustomerFragmentArgs by navArgs() // Use if using Safe Args
    private var customerId: Long = -1L

    private val viewModel: AddEditCustomerViewModel by viewModels {
        AddEditCustomerViewModelFactory(requireActivity().application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use Safe Args or manual retrieval
        // customerId = args.customerId
        customerId = arguments?.getLong("customerId", -1L) ?: -1L // Manual retrieval
        Log.d(TAG, "onCreate - Received customerId: $customerId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView - Inflating layout")
        _binding = FragmentAddEditCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated - Setting up UI and Observers")

        setupSpinner()
        setupClickListeners()
        setupObservers()

        if (customerId != -1L) {
            requireActivity().title = getString(R.string.edit_customer_title)
            Log.d(TAG, "onViewCreated - Edit Mode: Loading customer $customerId")
            viewModel.loadCustomer(customerId) // Load existing customer data
        } else {
            requireActivity().title = getString(R.string.add_customer_title)
            Log.d(TAG, "onViewCreated - Add Mode")
            binding.btnDeleteCustomer.isVisible = false // Hide delete for new
        }
    }

    private fun setupSpinner() {
        Log.d(TAG, "setupSpinner called")
        try {
            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.customer_types, // Correct reference
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCustomerType.setAdapter(adapter) // Use setAdapter
                Log.d(TAG, "setupSpinner - Adapter set for Customer Type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up customer type spinner", e)
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "setupClickListeners")
        if (_binding == null) return
        binding.btnSaveCustomer.setOnClickListener { saveCustomer() }
        binding.btnDeleteCustomer.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers")
        viewModel.loadedCustomer.observe(viewLifecycleOwner) { customer ->
            if (_binding == null) { Log.w(TAG,"loadedCustomer Observer: Binding is null"); return@observe }
            Log.d(TAG, "loadedCustomer observer triggered. Customer null? ${customer == null}. Current ID: $customerId")
            if (customer != null && customerId != -1L) {
                Log.d(TAG, "Populating form for existing customer: ${customer.name}")
                populateForm(customer)
                binding.btnDeleteCustomer.isVisible = true
            } else if (customer == null && customerId != -1L) {
                Log.e(TAG, "Error: Customer with ID $customerId not found for editing. Navigating back.")
                Toast.makeText(context, "Error loading customer data", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                // Adding new customer or initial null state
                Log.d(TAG, "Observer: Add mode or initial state, hiding delete button")
                binding.btnDeleteCustomer.isVisible = false
            }
        }
    }

    private fun populateForm(customer: Customer) {
        if (_binding == null) { Log.e(TAG,"Binding null in populateForm"); return }
        Log.d(TAG, "populateForm for customer: ${customer.name} (ID: ${customer.id})")
        binding.etCustomerName.setText(customer.name)
        binding.etCustomerCode.setText(customer.code ?: "")
        binding.etCustomerContact.setText(customer.contactInfo ?: "")
        binding.switchCustomerActive.isChecked = customer.isActive

        // Set spinner selection
        val typeAdapter = binding.spinnerCustomerType.adapter as? ArrayAdapter<String>
        if (typeAdapter != null && !customer.type.isNullOrBlank()) {
            val position = typeAdapter.getPosition(customer.type)
            Log.d(TAG, "populateForm: Trying to set type spinner to '${customer.type}' at position $position")
            if (position >= 0) {
                // For AutoCompleteTextView used in ExposedDropdownMenu
                binding.spinnerCustomerType.setText(typeAdapter.getItem(position), false)
                Log.d(TAG, "populateForm: Set spinner text.")
            } else {
                binding.spinnerCustomerType.setText("", false) // Clear if type not in adapter
                Log.w(TAG, "populateForm: Customer type '${customer.type}' not found in adapter.")
            }
        } else {
            binding.spinnerCustomerType.setText("", false) // Clear if no type saved
            Log.d(TAG, "populateForm: No customer type saved, clearing spinner text.")
        }
    }

    private fun saveCustomer() {
        if (_binding == null) { Log.e(TAG, "Binding null in saveCustomer"); return }
        Log.d(TAG, "saveCustomer called")

        val name = binding.etCustomerName.text.toString().trim()
        val code = binding.etCustomerCode.text?.toString()?.trim()
        val type = binding.spinnerCustomerType.text?.toString()?.trim() // Get text, trim
        val contactInfo = binding.etCustomerContact.text?.toString()?.trim()
        val isActive = binding.switchCustomerActive.isChecked

        Log.d(TAG, "saveCustomer: Data read: Name='$name', Code='$code', Type='$type', Contact='$contactInfo', Active=$isActive")

        // Validation
        var isValid = true
        binding.tilCustomerName.error = null
        if (name.isEmpty()) {
            binding.tilCustomerName.error = getString(R.string.error_field_required)
            isValid = false
            Log.w(TAG, "saveCustomer: Validation FAILED - Name empty")
        }
        // Add other validation...

        if (!isValid) {
            Log.w(TAG, "saveCustomer: Validation failed, returning.")
            return
        }

        Log.d(TAG, "saveCustomer: Validation passed. Calling ViewModel.")
        viewModel.saveCustomer(
            name = name,
            code = code, // Pass potentially null/empty
            type = if (type.isNullOrBlank()) null else type,  // Pass selected type or null if empty/prompt
            contactInfo = contactInfo, // Pass potentially null/empty
            isActive = isActive
        )

        Toast.makeText(requireContext(), R.string.customer_save_success, Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun showDeleteConfirmationDialog() {
        val customerName = viewModel.loadedCustomer.value?.name ?: "this customer"
        Log.d(TAG, "showDeleteConfirmationDialog for customer: $customerName")
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_customer_message, customerName))
            .setPositiveButton(R.string.delete) { _, _ ->
                Log.d(TAG, "Deletion confirmed for customer ID: $customerId")
                viewModel.deleteCustomer()
                Toast.makeText(requireContext(), R.string.customer_delete_success, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}