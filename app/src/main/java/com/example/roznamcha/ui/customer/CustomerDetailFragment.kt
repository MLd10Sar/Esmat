package com.example.roznamcha.ui.customer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlin.math.abs
import androidx.core.content.ContextCompat
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roznamcha.*
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction
import com.example.roznamcha.databinding.FragmentCustomerDetailBinding
import com.example.roznamcha.ui.list.OnTransactionClickListener
import com.example.roznamcha.ui.list.TransactionAdapter
import com.example.roznamcha.ui.list.TransactionListItem
import com.example.roznamcha.utils.DateUtils
import com.example.roznamcha.utils.StatementGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.glxn.qrgen.android.QRCode
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

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
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                    R.id.action_edit_customer -> {
                        navigateToAddEditCustomer(args.customerId)
                        true
                    }

                    R.id.action_share_statement -> {
                        shareCustomerStatementAsImage()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(this, currencySymbol)
        binding.recyclerViewCustomerTransactions.adapter = transactionAdapter
        binding.recyclerViewCustomerTransactions.layoutManager =
            LinearLayoutManager(requireContext())
    }


    private fun setupObservers() {
        viewModel.customer.observe(viewLifecycleOwner) { customer ->
            customer?.let {
                requireActivity().title = it.name
                binding.tvCustomerDetailName.text = it.name
                binding.tvCustomerDetailContact.text = it.contactInfo
            }
            viewModel.transactionsForCustomer.observe(viewLifecycleOwner) {
                this.transactionList = it
            }
        }

        viewModel.transactionHistoryWithHeaders.observe(viewLifecycleOwner) { historyWithHeaders ->
            transactionAdapter.submitList(historyWithHeaders)
            val firstTransactionItem =
                historyWithHeaders.firstOrNull { it is TransactionListItem.TransactionItem }
            if (firstTransactionItem != null) {
                val lastTransaction =
                    (firstTransactionItem as TransactionListItem.TransactionItem).transaction
                val daysAgo =
                    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastTransaction.dateMillis)
                binding.tvLastSeen.text = when {
                    daysAgo == 0L -> "آخرین معامله: امروز"
                    daysAgo == 1L -> "آخرین معامله: دیروز"
                    else -> "آخرین معامله: $daysAgo روز قبل"
                }
            } else {
                binding.tvLastSeen.text = "هیچ معامله ثبت نشده"
            }
        }

        // <<< CORRECTED: Observe 'currentBalance' from ViewModel >>>
        viewModel.currentBalance.observe(viewLifecycleOwner) { balance ->
            val formattedBalance =
                String.format(Locale.US, "%,.2f %s", abs(balance), currencySymbol)
            val balanceType = if (balance >= 0) "طلب" else "بدهی"
            binding.tvCustomerDetailBalance.text = "$formattedBalance $balanceType"
            val colorRes = if (balance >= 0) R.color.negative_red else R.color.positive_green
            binding.tvCustomerDetailBalance.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    colorRes
                )
            )
        }

        viewModel.totalSalesToCustomer.observe(viewLifecycleOwner) { totalSales ->
            val formattedSales = String.format(Locale.US, "%,.2f %s", totalSales, currencySymbol)
            binding.tvTotalSalesToCustomer.text = "مجموع فروشات: $formattedSales"
        }

        viewModel.repaymentInsight.observe(viewLifecycleOwner) { insight ->
            binding.tvCustomerInsight.isVisible = !insight.isNullOrBlank()
            binding.tvCustomerInsight.text = if (insight != null) "💡 تحلیل: $insight" else ""
        }

        viewModel.transactionsForCustomer.observe(viewLifecycleOwner) {
            this.transactionList = it
        }
        // <<< ADD THESE NEW OBSERVERS >>>
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
        return true
    }

    override fun onSettleClick(transaction: Transaction) { /* Can be implemented later if needed */
    }

    override fun onShareClick(transaction: Transaction) {
        shareSingleTransactionAsText(transaction)
    }

    // --- Helper Functions ---
    // --- COMPLETE HELPER FUNCTIONS ---
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


    private fun shareCustomerStatementAsImage() {
        val context = this.context ?: return
        val customer = viewModel.customer.value ?: run {
            Toast.makeText(context, "Customer data not loaded yet.", Toast.LENGTH_SHORT).show()
            return
        }
        val balance = viewModel.currentBalance.value ?: 0.0
        val currency = SettingsManager.getCurrency(context) ?: "AFN"

        // --- STEP 1: Inflate the layout ONCE ---
        val statementView = LayoutInflater.from(context).inflate(R.layout.layout_customer_statement, null, false)

        // --- STEP 2: Find all views within THAT layout instance ---
        val tvShopName = statementView.findViewById<TextView>(R.id.tvShopNameStatement)
        val tvShopAddress = statementView.findViewById<TextView>(R.id.tvShopAddressStatement)
        val imgLogo = statementView.findViewById<ImageView>(R.id.imgShopLogo)
        val tvReceiptTitle = statementView.findViewById<TextView>(R.id.tvReceiptTitle)
        val tvCustomerName = statementView.findViewById<TextView>(R.id.tvCustomerNameStatement)
        val tvStatementDate = statementView.findViewById<TextView>(R.id.tvStatementDate)
        val tvTotalBalance = statementView.findViewById<TextView>(R.id.tvTotalBalanceStatement)
        val layoutRecentTransactions = statementView.findViewById<LinearLayout>(R.id.layoutRecentTransactions)
        val tvFooter = statementView.findViewById<TextView>(R.id.tvFooterText)

        // --- STEP 3: Populate all the views with REAL data ---
        tvShopName.text = SettingsManager.getShopName(context) ?: getString(R.string.app_name)
        val shopAddress = SettingsManager.getShopAddress(context)
        tvShopAddress.isVisible = !shopAddress.isNullOrBlank()
        tvShopAddress.text = shopAddress

        // Load Logo
        val logoFile = File(context.filesDir, "shop_logo.png")
        val shopName = SettingsManager.getShopName(context)
        tvShopName.text = if (!shopName.isNullOrBlank()) shopName else context.getString(R.string.app_name)

        tvShopAddress.isVisible = !shopAddress.isNullOrBlank()
        tvShopAddress.text = shopAddress

        if (logoFile.exists()) {
            BitmapFactory.decodeFile(logoFile.absolutePath)?.let {
                imgLogo.setImageBitmap(it)
                imgLogo.isVisible = true
            }
        }



        tvCustomerName.text = customer.name
        tvStatementDate.text = DateUtils.formatMillis(context, System.currentTimeMillis())
        tvTotalBalance.text = String.format(Locale.US, "%,.2f %s", abs(balance), currency)

        // Dynamically add transaction rows
        layoutRecentTransactions.removeAllViews()
        val recentUnsettled = transactionList.filter { !it.isSettled }.take(10)
        if (recentUnsettled.isEmpty()) {
            val tv = TextView(context).apply { text = "معامله باقی مانده وجود ندارد."; setTextColor(Color.DKGRAY) }
            layoutRecentTransactions.addView(tv)
        } else {
            recentUnsettled.forEach { transaction ->
                val row = LayoutInflater.from(context).inflate(R.layout.list_item_statement_row, layoutRecentTransactions, false)
                row.findViewById<TextView>(R.id.tvRowDate).text = DateUtils.formatMillis(context, transaction.dateMillis)
                row.findViewById<TextView>(R.id.tvRowDescription).text = transaction.description
                row.findViewById<TextView>(R.id.tvRowAmount).text = String.format(Locale.US, "%,.2f", transaction.remainingAmount)
                layoutRecentTransactions.addView(row)
            }
        }

        val shopPhone = SettingsManager.getShopPhone(context)
        tvFooter.text = if (!shopPhone.isNullOrBlank()) getString(R.string.receipt_footer_with_phone, shopPhone) else getString(R.string.receipt_footer_simple)


        // --- STEP 4: Convert THIS populated view to a Bitmap ---
        val bitmap = createBitmapFromView(statementView)

        // --- STEP 5: Save and Share ---
        val imageFile = saveBitmapToCache(bitmap) ?: return
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "اشتراک صورت حساب"))
    }


    //////////////



    private fun shareImageFile(file: File) {
        val context = this.context ?: return
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "اشتراک صورت حساب"))
    }

    // --- NEW HELPER to keep the sharing logic clean ---
    private fun populateStatementView(view: View, customer: Customer, balance: Double) {
        val context = view.context


        // Find all views
        val tvShopName = view.findViewById<TextView>(R.id.tvShopNameStatement)
        val tvShopAddress = view.findViewById<TextView>(R.id.tvShopAddressStatement)
        val tvCustomerName = view.findViewById<TextView>(R.id.tvCustomerNameStatement)
        val tvStatementDate = view.findViewById<TextView>(R.id.tvStatementDate)
        val tvTotalBalance = view.findViewById<TextView>(R.id.tvTotalBalanceStatement)
        val layoutRecentTransactions =
            view.findViewById<LinearLayout>(R.id.layoutRecentTransactions)

        // Populate Header and Balance
        tvShopName.text = SettingsManager.getShopName(context) ?: getString(R.string.app_name)
        val shopAddress = SettingsManager.getShopAddress(context)
        tvShopAddress.isVisible = !shopAddress.isNullOrBlank()
        tvShopAddress.text = shopAddress
        tvCustomerName.text = customer.name
        tvStatementDate.text = DateUtils.formatMillis(context, System.currentTimeMillis())
        tvTotalBalance.text = String.format(Locale.US, "%,.2f %s", abs(balance), currencySymbol)

        // Dynamically populate the list of recent transactions
        layoutRecentTransactions.removeAllViews()
        val recentUnsettled = transactionList.filter { !it.isSettled }.take(10) // Show up to 10

        if (recentUnsettled.isEmpty()) {
            val tv = TextView(context).apply {
                text = "معامله باقی مانده وجود ندارد."; setTextColor(Color.DKGRAY); setPadding(
                0,
                8,
                0,
                8
            )
            }
            layoutRecentTransactions.addView(tv)
        } else {
            recentUnsettled.forEach { transaction ->
                val row = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_statement_row, layoutRecentTransactions, false)
                row.findViewById<TextView>(R.id.tvRowDate).text =
                    DateUtils.formatMillis(context, transaction.dateMillis)
                row.findViewById<TextView>(R.id.tvRowDescription).text = transaction.description
                row.findViewById<TextView>(R.id.tvRowAmount).text =
                    String.format(Locale.US, "%,.2f", transaction.remainingAmount)
                layoutRecentTransactions.addView(row)
            }
        }
    }

    private fun shareSingleTransactionAsText(transaction: Transaction) {
        val context = this.context ?: return
        val customer = viewModel.customer.value // Get the customer for this screen
        val receiptBuilder = StringBuilder()

        val categoryEnum = TransactionCategory.valueOf(transaction.category)
        val title = when (categoryEnum) {
            TransactionCategory.SALE, TransactionCategory.RECEIVABLE -> "رسید فروش"
            TransactionCategory.PURCHASE, TransactionCategory.DEBT -> "سند خرید"
            else -> "سند مصرف"
        }

        receiptBuilder.appendLine("*$title - روزنامچه*")
        receiptBuilder.appendLine("-----------------------------------")
        receiptBuilder.appendLine(
            "تاریخ: ${
                DateUtils.formatMillis(
                    context,
                    transaction.dateMillis
                )
            }"
        )

        customer?.let {
            receiptBuilder.appendLine("مشتری: ${it.name}")
        }

        receiptBuilder.appendLine("شرح: ${transaction.description}")

        if (transaction.quantity != null && transaction.unitPrice != null) {
            receiptBuilder.appendLine("مقدار: ${transaction.quantity} ${transaction.quantityUnit ?: ""}")
            receiptBuilder.appendLine(
                "قیمت واحد: ${
                    String.format(
                        Locale.US,
                        "%,.2f",
                        transaction.unitPrice
                    )
                }"
            )
        }

        val originalAmount = transaction.originalAmount ?: transaction.amount
        receiptBuilder.appendLine(
            "*مبلغ مجموعی: ${
                String.format(
                    Locale.US,
                    "%,.2f %s",
                    originalAmount,
                    currencySymbol
                )
            }*"
        )

        if (transaction.isSettled) {
            receiptBuilder.appendLine("وضعیت: پرداخت شده")
        } else {
            val remaining = transaction.remainingAmount ?: transaction.amount
            receiptBuilder.appendLine("وضعیت: باقی")
            receiptBuilder.appendLine(
                "*مبلغ باقی مانده: ${
                    String.format(
                        Locale.US,
                        "%,.2f %s",
                        remaining,
                        currencySymbol
                    )
                }*"
            )
        }
        receiptBuilder.appendLine("-----------------------------------")
        receiptBuilder.appendLine("تشکر از همکاری شما!")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptBuilder.toString())
            type = "text/plain"
        }

        try {
            startActivity(Intent.createChooser(sendIntent, "اشتراک گذاری رسید"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to share.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val widthInDp = 450
        val widthInPixels = (widthInDp * requireContext().resources.displayMetrics.density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthInPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap =
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    private fun saveBitmapToCache(bitmap: Bitmap): File? {
        val cachePath = File(requireContext().cacheDir, "images")
        cachePath.mkdirs()
        return try {
            val file = File(cachePath, "statement_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}