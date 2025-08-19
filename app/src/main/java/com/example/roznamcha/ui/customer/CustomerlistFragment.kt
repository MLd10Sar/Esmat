package com.example.roznamcha.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import java.util.Locale
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.databinding.FragmentCustomerListBinding

// The fragment NO LONGER needs to implement a click listener interface.
class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerListViewModel by viewModels {
        CustomerListViewModelFactory(requireActivity().application)
    }

    // The adapter is declared here but initialized later
    private lateinit var customerAdapter: CustomerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = getString(R.string.customer_list_title)

        setupMenu()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.customer_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_new_item -> {
                        // Navigate to add a new customer, pass -1L
                        navigateToAddEditCustomer(-1L)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        // --- THE FIX IS HERE ---
        // We create the adapter and pass it a lambda function directly.
        // The `customer` variable inside the lambda is the Customer object that was clicked.
        customerAdapter = CustomerAdapter { customer ->
            // This is the code that runs when a list item is clicked
            navigateToCustomerDetail(customer.id)        }

        binding.recyclerViewCustomers.apply {
            adapter = customerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.allCustomers.observe(viewLifecycleOwner) { customers ->
            customerAdapter.submitList(customers)
            binding.tvEmptyList.isVisible = customers.isEmpty()
            binding.recyclerViewCustomers.isVisible = customers.isNotEmpty()
        }
        val currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"

        viewModel.customerCount.observe(viewLifecycleOwner) { count ->
            binding.tvCustomerCount.text = count.toString()
        }
        viewModel.totalOutstandingBalance.observe(viewLifecycleOwner) { balance ->
            binding.tvOutstandingBalance.text = String.format(Locale.US, "%,.2f %s", balance, currencySymbol)
        }
    }



    private fun navigateToAddEditCustomer(customerId: Long) {
        try {
            val action = CustomerListFragmentDirections.actionCustomerListFragmentToAddEditCustomerFragment(customerId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("CustomerListFragment", "Navigation to Add/Edit Customer failed.", e)
        }
    }

    // --- ADD THIS NEW HELPER FUNCTION ---
    // Add this helper function for navigation
    private fun navigateToCustomerDetail(customerId: Long) {
        try {
            val action = CustomerListFragmentDirections.actionCustomerListFragmentToCustomerDetailFragment(customerId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("CustomerListFragment", "Navigation to Customer Detail failed.", e)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewCustomers.adapter = null
        _binding = null
    }
}