
package com.example.roznamcha.ui.inventory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roznamcha.R
import com.example.roznamcha.data.db.entity.InventoryItem
import com.example.roznamcha.databinding.FragmentInventoryListBinding

class InventoryListFragment : Fragment(), OnInventoryItemClickListener {

    private var _binding: FragmentInventoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryListViewModel by viewModels {
        InventoryListViewModelFactory(requireActivity().application)
    }

    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.inventory_godam)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(this)
        binding.recyclerViewInventory.apply {
            adapter = inventoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        // This observer is now correct. It receives a list of InventoryItems
        // and submits it to the InventoryAdapter.
        viewModel.allInventoryItems.observe(viewLifecycleOwner) { items ->
            Log.d("InventoryList", "Observer received new list with ${items.size} items.")
            inventoryAdapter.submitList(items)
            binding.tvEmptyInventory.isVisible = items.isEmpty()
            binding.recyclerViewInventory.isVisible = items.isNotEmpty()
        }
    }

    private fun setupClickListeners() {
        // This will now correctly find the FAB in the layout
        binding.fabAddInventoryItem.setOnClickListener {
            navigateToAddEditScreen(-1L)
        }
    }

    // --- OnInventoryItemClickListener ---
    override fun onItemClick(item: InventoryItem) {
        // This navigates to the Add/Edit screen for an EXISTING item
        navigateToAddEditScreen(item.id)
    }

    override fun onItemLongClick(item: InventoryItem): Boolean {
        showDeleteConfirmationDialog(item)
        return true
    }

    // --- Helper Functions ---
    private fun navigateToAddEditScreen(itemId: Long) {
        try {
            // This will now correctly find the action in the nav_graph
            val action = InventoryListFragmentDirections.actionInventoryListFragmentToAddEditInventoryItemFragment(itemId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("InventoryListFragment", "Navigation to Add/Edit failed", e)
        }
    }


    private fun showDeleteConfirmationDialog(item: InventoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف جنس")
            .setMessage("آیا از حذف '${item.name}' مطمئن هستید؟")
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}