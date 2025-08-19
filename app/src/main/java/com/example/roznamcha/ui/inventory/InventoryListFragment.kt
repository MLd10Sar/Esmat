package com.example.roznamcha.ui.inventory

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
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
import com.example.roznamcha.data.db.entity.InventoryItem
import com.example.roznamcha.databinding.FragmentInventoryListBinding
import java.util.Locale

class InventoryListFragment : Fragment(), OnInventoryItemClickListener {

    private var _binding: FragmentInventoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryListViewModel by viewModels {
        InventoryListViewModelFactory(requireActivity().application)
    }

    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu() // <<< ADDED
        setupRecyclerView()
        setupObservers()
    }

    // <<< ADDED NEW FUNCTION >>>
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.inventory_list_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_new_inventory_item -> {
                        // When '+' is clicked, navigate to add a new inventory item
                        navigateToAddEditScreen(-1L)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(this)
        binding.recyclerViewInventory.apply {
            adapter = inventoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        val currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"

        viewModel.totalItemCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalItemCount.text = String.format(Locale.US, "%.0f", count)
        }
        viewModel.totalInventoryValue.observe(viewLifecycleOwner) { value ->
            binding.tvTotalInventoryValue.text =
                String.format(Locale.US, "%,.2f %s", value, currencySymbol)
            viewModel.allInventoryItems.observe(viewLifecycleOwner) { items ->
                inventoryAdapter.submitList(items)
                binding.tvEmptyInventory.isVisible = items.isEmpty()
                binding.recyclerViewInventory.isVisible = items.isNotEmpty()
            }
            val currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"

            viewModel.totalItemCount.observe(viewLifecycleOwner) { count ->
                // Format as a whole number
                binding.tvTotalItemCount.text = String.format(Locale.US, "%.0f", count)
            }
            viewModel.totalInventoryValue.observe(viewLifecycleOwner) { value ->
                binding.tvTotalInventoryValue.text =
                    String.format(Locale.US, "%,.2f %s", value, currencySymbol)
            }
        }
    }


    // --- Implementation of OnInventoryItemClickListener ---
    override fun onItemClick(item: InventoryItem) {
        navigateToAddEditScreen(item.id)
    }

    override fun onItemLongClick(item: InventoryItem): Boolean {
        showDeleteConfirmationDialog(item)
        return true
    }

    // --- Helper Functions ---
    private fun navigateToAddEditScreen(itemId: Long) {
        try {
            val bundle = Bundle().apply {
                putLong("itemId", itemId)
            }
            findNavController().navigate(R.id.action_inventoryListFragment_to_addEditInventoryItemFragment, bundle)
        } catch (e: Exception) {
            Log.e("InventoryListFragment", "Navigation to Add/Edit failed", e)
        }
    }

    private fun showDeleteConfirmationDialog(item: InventoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف جنس")
            .setMessage("آیا از حذف '${item.name}' مطمئن هستید؟")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("حذف") { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewInventory.adapter = null
        _binding = null
    }
}