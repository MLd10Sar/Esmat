package com.example.roznamcha.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction
import com.example.roznamcha.databinding.FragmentCustomerDetailBinding
import com.example.roznamcha.ui.list.OnTransactionClickListener
import com.example.roznamcha.ui.list.TransactionAdapter
import com.example.roznamcha.ui.list.TransactionListItem
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit // <<< IMPORT THIS

class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    private val args: CustomerDetailFragmentArgs by navArgs()
    private val viewModel: CustomerDetailViewModel by viewModels {
        CustomerDetailViewModelFactory(requireActivity().application, args.customerId)
    }

    private lateinit var transactionAdapter: TransactionAdapter
    private var currentCustomer: Customer? = null
    private var currencySymbol: String = "AFN"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"
        setupMenu()
        setupRecyclerView()
        setupObservers()
    }



    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.customer_detail_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_customer -> {
                        navigateToAddEditCustomer(args.customerId)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            clickListener = object : OnTransactionClickListener {
                override fun onTransactionClick(transaction: Transaction) {
                    val action = CustomerDetailFragmentDirections.actionGlobalAddEditTransactionFragment(
                        category = transaction.category,
                        transactionId = transaction.id
                    )
                    findNavController().navigate(action)
                }
                override fun onTransactionLongClick(transaction: Transaction): Boolean { return true }
                override fun onSettleClick(transaction: Transaction) { /* Can be implemented later */ }
                override fun onShareClick(transaction: Transaction) {
                    shareTransactionAsReceipt(transaction)
                }
            },
            currencySymbol = currencySymbol
        )
        binding.recyclerViewCustomerTransactions.adapter = transactionAdapter
    }

    private fun setupObservers() {
        viewModel.customer.observe(viewLifecycleOwner) { customer ->
            this.currentCustomer = customer
            customer?.let {
                requireActivity().title = it.name
                binding.tvCustomerDetailName.text = it.name
                binding.tvCustomerDetailContact.text = it.contactInfo
            }
        }

        viewModel.transactionHistoryWithHeaders.observe(viewLifecycleOwner) { historyWithHeaders ->
            transactionAdapter.submitList(historyWithHeaders)
            val firstTransactionItem = historyWithHeaders.firstOrNull { it is TransactionListItem.TransactionItem }
            if (firstTransactionItem != null) {
                val lastTransaction = (firstTransactionItem as TransactionListItem.TransactionItem).transaction
                val daysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastTransaction.dateMillis)
                binding.tvLastSeen.text = when {
                    daysAgo == 0L -> "آخرین معامله: امروز"
                    daysAgo == 1L -> "آخرین معامله: دیروز"
                    else -> "آخرین معامله: $daysAgo روز قبل"
                }
            } else {
                binding.tvLastSeen.text = "هیچ معامله ثبت نشده"
            }
        }

        viewModel.repaymentInsight.observe(viewLifecycleOwner) { insight ->
            binding.tvCustomerInsight.isVisible = !insight.isNullOrBlank()
            binding.tvCustomerInsight.text = if (insight != null) "💡 تحلیل: $insight" else ""
        }

        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            val formattedBalance = String.format(Locale.US, "%,.2f %s", balance, currencySymbol)
            binding.tvCustomerBalance.text = formattedBalance
            val colorRes = when {
                balance > 0 -> R.color.negative_red
                balance < 0 -> R.color.positive_green
                else -> R.color.primary_text_color
            }
            binding.tvCustomerBalance.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }

        viewModel.totalSalesToCustomer.observe(viewLifecycleOwner) { totalSales ->
            val formattedSales = String.format(Locale.US, "%,.2f %s", totalSales, currencySymbol)
            binding.tvTotalSalesToCustomer.text = "مجموع فروشات: $formattedSales"
        }
    }

    // --- ADD THIS HELPER FUNCTION for navigation ---
    private fun navigateToAddEditCustomer(customerId: Long) {
        try {
            val action =
                CustomerDetailFragmentDirections.actionCustomerDetailFragmentToAddEditCustomerFragment(
                    customerId
                )
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("CustomerDetailFragment", "Navigation to Add/Edit Customer failed", e)
        }
    }

    // --- ADD THIS COMPLETE HELPER FUNCTION ---
    private fun shareTransactionAsReceipt(transaction: Transaction) {
        val currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"
        val dateFormat = SimpleDateFormat("yyyy/MM/dd, hh:mm a", Locale.US)
        dateFormat.timeZone = TimeZone.getDefault()

        val receiptBuilder = StringBuilder()
        receiptBuilder.appendLine("*رسید معامله - روزنامچه*")
        receiptBuilder.appendLine("-----------------------------------")
        receiptBuilder.appendLine("تاریخ: ${dateFormat.format(Date(transaction.dateMillis))}")

        // <<< ACCESS currentCustomer safely >>>
        currentCustomer?.let { customer ->
            receiptBuilder.appendLine("مشتری: ${customer.name}")
        }

        receiptBuilder.appendLine("شرح: ${transaction.description}")

        if (transaction.quantity != null && transaction.unitPrice != null) {
            receiptBuilder.appendLine("مقدار: ${transaction.quantity} ${transaction.quantityUnit ?: ""}")
            receiptBuilder.appendLine(
                "قیمت واحد: ${String.format(Locale.US, "%,.2f", transaction.unitPrice)}" // <<< Fixed formatting
            )
        }

        val originalAmount = transaction.originalAmount ?: transaction.amount
        receiptBuilder.appendLine("*مبلغ مجموعی: ${String.format(Locale.US, "%,.2f %s", originalAmount, currencySymbol)}*")

        if (transaction.isSettled) {
            receiptBuilder.appendLine("وضعیت: پرداخت شده")
        } else {
            val remaining = transaction.remainingAmount ?: transaction.amount
            receiptBuilder.appendLine("وضعیت: باقی")
            receiptBuilder.appendLine("*مبلغ باقی مانده: ${String.format(Locale.US, "%,.2f %s", remaining, currencySymbol)}*")
        }
        receiptBuilder.appendLine("-----------------------------------")
        receiptBuilder.appendLine("تشکر از همکاری شما!")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptBuilder.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "اشتراک گذاری رسید"))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}