package com.example.roznamcha.ui.customer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.data.db.entity.Transaction
import com.example.roznamcha.databinding.FragmentCustomerDetailBinding
import com.example.roznamcha.ui.list.OnTransactionClickListener
import com.example.roznamcha.ui.list.TransactionAdapter
import com.example.roznamcha.ui.list.TransactionListItem
import java.util.*

class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    private var customerId: Long = -1L

    // Initialize the ViewModel, passing the customerId from arguments
    private val viewModel: CustomerDetailViewModel by viewModels {
        CustomerDetailViewModelFactory(requireActivity().application, customerId)
    }

    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get customerId safely from navigation arguments
        arguments?.let {
            customerId = it.getLong("customerId")
        }
        if (customerId == -1L) {
            // If no valid ID was passed, we can't show details, so go back.
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        // --- THIS IS THE FIX ---
        // We create a listener object. Since we don't want to handle clicks
        // on this screen, most methods can be empty.
        val clickListener = object : OnTransactionClickListener {
            override fun onTransactionClick(transaction: Transaction) {
                // Do nothing when a row is clicked, or maybe show a toast with details.
                // For now, we do nothing.
            }
            override fun onTransactionLongClick(transaction: Transaction): Boolean {
                // Do nothing on long click.
                return true
            }
            override fun onSettleClick(transaction: Transaction) {
                // Settle actions should be done from the main Debt/Receivable lists, not here.
            }
            override fun onShareClick(transaction: Transaction) {
                // We could implement sharing from here too, but for now, we'll keep it simple.
            }
        }

        // Now we initialize the adapter with the listener object.
        transactionAdapter = TransactionAdapter(
            clickListener = clickListener,
            currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"
        )
        binding.recyclerViewCustomerTransactions.adapter = transactionAdapter
    }

    private fun setupObservers() {
        val currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"

        // Observe the customer's details (name, contact)
        viewModel.customerDetails.observe(viewLifecycleOwner) { customer ->
            customer?.let {
                requireActivity().title = it.name // Set screen title to the customer's name
                binding.tvCustomerDetailName.text = it.name
                binding.tvCustomerDetailContact.text = it.contactInfo
            }
        }

        // Observe the customer's transaction history and submit it to the adapter
        viewModel.transactionHistory.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions as List<TransactionListItem?>?)
        }

        // Observe the final calculated balance
        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            val formattedBalance = String.format(Locale.US, "%,.2f %s", balance, currencySymbol)
            binding.tvCustomerBalance.text = formattedBalance

            // Creatively change the color based on the balance
            val color = when {
                balance > 0 -> ContextCompat.getColor(requireContext(), R.color.negative_red) // They owe you (your asset, their debt)
                balance < 0 -> ContextCompat.getColor(requireContext(), R.color.positive_green) // You owe them (your debt, their asset)
                else -> ContextCompat.getColor(requireContext(), R.color.primary_text_color) // Neutral
            }
            binding.tvCustomerBalance.setTextColor(color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}