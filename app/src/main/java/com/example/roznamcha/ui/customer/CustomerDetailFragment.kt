package com.example.roznamcha.ui.customer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.roznamcha.*
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction
import com.example.roznamcha.databinding.FragmentCustomerDetailBinding
import com.example.roznamcha.ui.list.OnTransactionClickListener
import com.example.roznamcha.ui.list.TransactionAdapter
import com.example.roznamcha.ui.list.TransactionListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CustomerDetailFragment : Fragment(), OnTransactionClickListener {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    private val args: CustomerDetailFragmentArgs by navArgs()
    private val viewModel: CustomerDetailViewModel by viewModels {
        CustomerDetailViewModelFactory(requireActivity().application, args.customerId)
    }
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactionList: List<Transaction> = emptyList()
    private var currencySymbol: String = "AFN"

    // --- Lifecycle Methods ---
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewCustomerTransactions.adapter = null
        _binding = null
    }

    // --- Setup Functions ---
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.customer_detail_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_customer -> { navigateToAddEditCustomer(args.customerId); true }
                    R.id.action_share_statement -> { generateAndShareInvoice(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(this, currencySymbol)
        binding.recyclerViewCustomerTransactions.adapter = transactionAdapter
    }

    private fun setupObservers() {
        viewModel.customer.observe(viewLifecycleOwner) { customer ->
            customer?.let {
                requireActivity().title = it.name
                binding.tvCustomerDetailName.text = it.name
                binding.tvCustomerDetailContact.isVisible = !it.contactInfo.isNullOrBlank()
                binding.tvCustomerDetailContact.text = it.contactInfo
            }
        }
        viewModel.transactionsForCustomer.observe(viewLifecycleOwner) {
            this.transactionList = it
        }
        viewModel.transactionHistoryWithHeaders.observe(viewLifecycleOwner) { historyWithHeaders ->
            transactionAdapter.submitList(historyWithHeaders)
            val firstTransactionItem = historyWithHeaders.firstOrNull { item -> item is TransactionListItem.TransactionItem }
            if (firstTransactionItem is TransactionListItem.TransactionItem) {
                val lastTransaction = firstTransactionItem.transaction
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
        viewModel.allCustomers.observe(viewLifecycleOwner) { customers ->
            transactionAdapter.updateCustomers(customers)
        }
        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            val formattedBalance = String.format(Locale.US, "%,.2f %s", abs(balance), currencySymbol)
            val balanceType = if (balance >= 0) "طلب" else "بدهی"
            binding.tvCustomerDetailBalance.text = "$formattedBalance $balanceType"
            val colorRes = if (balance >= 0) R.color.positive_green else R.color.negative_red
            binding.tvCustomerDetailBalance.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
        viewModel.totalSalesToCustomer.observe(viewLifecycleOwner) { totalSales ->
            val formattedSales = String.format(Locale.US, "%,.2f %s", totalSales, currencySymbol)
            binding.tvTotalSalesToCustomer.text = "مجموع فروشات: $formattedSales"
        }
        viewModel.repaymentInsight.observe(viewLifecycleOwner) { insight ->
            binding.tvCustomerInsight.isVisible = !insight.isNullOrBlank()
            binding.tvCustomerInsight.text = if (insight != null) "💡 تحلیل: $insight" else ""
        }
        viewModel.nextPurchaseInsight.observe(viewLifecycleOwner) { insight ->
            binding.tvNextPurchaseInsight.isVisible = !insight.isNullOrBlank()
            binding.tvNextPurchaseInsight.text = insight
        }
        viewModel.customerValueTag.observe(viewLifecycleOwner) { tag ->
            binding.tvCustomerValueTag.isVisible = !tag.isNullOrBlank()
            binding.tvCustomerValueTag.text = tag
        }
    }

    // --- OnTransactionClickListener Implementation ---
    override fun onTransactionClick(transaction: Transaction) {
        val action = CustomerDetailFragmentDirections.actionGlobalAddEditTransactionFragment(
            category = transaction.category,
            transactionId = transaction.id
        )
        findNavController().navigate(action)
    }

    override fun onTransactionLongClick(transaction: Transaction): Boolean {
        // Implement share/delete options here if desired
        return true
    }

    override fun onSettleClick(transaction: Transaction) {
        Toast.makeText(context, "برای تصفیه حساب به لیست عمومی طلب/بدهی بروید.", Toast.LENGTH_SHORT).show()
    }

    override fun onShareClick(transaction: Transaction) {
        shareSingleTransactionAsText(transaction)
    }

    // --- Helper Functions ---
    private fun shareSingleTransactionAsText(transaction: Transaction) {
        val context = this.context ?: return
        val receiptBuilder = StringBuilder()
        val categoryEnum = try { TransactionCategory.valueOf(transaction.category) } catch (e: Exception) { null }
        val title = when (categoryEnum) {
            TransactionCategory.SALE, TransactionCategory.RECEIVABLE -> "رسید فروش"
            TransactionCategory.PURCHASE, TransactionCategory.DEBT -> "سند خرید"
            else -> "سند معامله"
        }
        receiptBuilder.appendLine("*$title - روزنامچه*")
        receiptBuilder.appendLine("-----------------------------------")
        receiptBuilder.appendLine("تاریخ: ${SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(transaction.dateMillis))}")
        viewModel.customer.value?.let { receiptBuilder.appendLine("مشتری: ${it.name}") }
        receiptBuilder.appendLine("شرح: ${transaction.description}")
        if (transaction.quantity != null && transaction.quantity > 0) {
            receiptBuilder.appendLine("مقدار: ${transaction.quantity} ${transaction.quantityUnit ?: ""}")
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
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptBuilder.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "اشتراک گذاری رسید"))
    }

    private fun generateAndShareInvoice() {
        val context = this.context ?: return
        val customer = viewModel.customer.value ?: return
        val balance = viewModel.currentBalance.value ?: 0.0
        binding.progressBar.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val invoiceView = LayoutInflater.from(context).inflate(R.layout.invoice_template, null, false)
            populateInvoiceView(invoiceView, customer, balance)
            val bitmap = createBitmapFromView(invoiceView)
            val imageFile = saveBitmapToCache(bitmap)
            withContext(Dispatchers.Main) {
                binding.progressBar.isVisible = false
                if (imageFile != null) {
                    shareImageFile(imageFile)
                } else {
                    Toast.makeText(context, "Could not create statement image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateInvoiceView(view: View, customer: Customer, balance: Double) {
        val context = view.context
        val tvShopName = view.findViewById<TextView>(R.id.tvShopName)
        val tvShopAddress = view.findViewById<TextView>(R.id.tvShopAddress)
        val imgLogo = view.findViewById<ImageView>(R.id.imgShopLogo)
        val tvCustomerName = view.findViewById<TextView>(R.id.tvCustomerName)
        val tvStatementDate = view.findViewById<TextView>(R.id.tvStatementDate)
        val tvTotalBalance = view.findViewById<TextView>(R.id.tvTotalBalance)
        val layoutTransactionRows = view.findViewById<LinearLayout>(R.id.layoutTransactionRows)

        tvShopName.text = SettingsManager.getShopName(context) ?: getString(R.string.app_name)
        val shopAddress = SettingsManager.getShopAddress(context)
        tvShopAddress.isVisible = !shopAddress.isNullOrBlank()
        tvShopAddress.text = shopAddress
        tvCustomerName.text = customer.name
        tvStatementDate.text = "تاریخ: ${SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())}"
        tvTotalBalance.text = String.format(Locale.US, "%,.2f %s", abs(balance), currencySymbol)

        val logoFile = File(context.filesDir, "shop_logo.png")
        if (logoFile.exists()) {
            try {
                BitmapFactory.decodeFile(logoFile.absolutePath)?.let {
                    imgLogo.setImageBitmap(it)
                    imgLogo.isVisible = true
                }
            } catch (e: Exception) {
                Log.e("PopulateInvoice", "Error loading shop logo", e)
                imgLogo.isVisible = false
            }
        } else {
            imgLogo.isVisible = false
        }

        layoutTransactionRows.removeAllViews()
        val unsettledTransactions = transactionList.filter { !it.isSettled }.take(15)
        if (unsettledTransactions.isEmpty()) {
            val tv = TextView(context).apply {
                text = "معامله باقی مانده وجود ندارد."
                setTextColor(Color.DKGRAY)
            }
            layoutTransactionRows.addView(tv)
        } else {
            unsettledTransactions.forEach { transaction ->
                val row = LayoutInflater.from(context).inflate(R.layout.list_item_invoice_row, layoutTransactionRows, false)
                row.findViewById<TextView>(R.id.tvRowDate).text = SimpleDateFormat("yy/MM/dd", Locale.US).format(Date(transaction.dateMillis))
                row.findViewById<TextView>(R.id.tvRowDescription).text = transaction.description
                val amountToFormat = transaction.remainingAmount ?: transaction.amount ?: 0.0
                val formattedAmount = String.format(Locale.US, "%,.2f", amountToFormat)
                row.findViewById<TextView>(R.id.tvRowAmount).text = formattedAmount
                layoutTransactionRows.addView(row)
            }
        }
    }

    private fun createBitmapFromView(view: View): Bitmap {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToCache(bitmap: Bitmap): File? {
        val context = this.context ?: return null
        val cachePath = File(context.cacheDir, "invoices")
        cachePath.mkdirs()
        return try {
            val file = File(cachePath, "invoice_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            stream.close()
            file
        } catch (e: Exception) {
            Log.e("CustomerDetailFragment", "Failed to save bitmap to cache", e)
            null
        }
    }

    private fun shareImageFile(file: File) {
        val context = this.context ?: return
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "ارسال صورت حساب"))
    }

    private fun navigateToAddEditCustomer(customerId: Long) {
        try {
            val action = CustomerDetailFragmentDirections.actionCustomerDetailFragmentToAddEditCustomerFragment(customerId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("CustomerDetailFragment", "Navigation to Add/Edit Customer failed", e)
        }
    }
}