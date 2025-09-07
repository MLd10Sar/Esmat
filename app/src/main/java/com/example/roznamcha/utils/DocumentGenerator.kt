package com.example.roznamcha.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.core.graphics.createBitmap

object DocumentGenerator {

    // --- PUBLIC FUNCTIONS to be called from Fragments ---

    fun createReceiptBitmap(context: Context, transaction: Transaction, customer: Customer?, currencySymbol: String): Bitmap? {
        val view = LayoutInflater.from(context).inflate(R.layout.document_template, null, false)
        populateReceiptView(context, view, transaction, customer, currencySymbol)
        return createBitmapFromView(view)
    }

    fun createStatementBitmap(context: Context, customer: Customer, transactions: List<Transaction>, balance: Double, currencySymbol: String): Bitmap? {
        val view = LayoutInflater.from(context).inflate(R.layout.document_template, null, false)
        populateStatementView(context, view, customer, transactions, balance, currencySymbol)
        return createBitmapFromView(view)
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileNamePrefix: String): File? {
        val cachePath = File(context.cacheDir, "documents")
        cachePath.mkdirs()
        return try {
            val file = File(cachePath, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            stream.close()
            file
        } catch (e: Exception) {
            Log.e("DocumentGenerator", "Failed to save bitmap to cache", e)
            null
        }
    }

    fun shareImageFile(context: Context, file: File, title: String) {
        val imageUri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            Log.e("DocumentGenerator", "Error creating FileProvider URI", e)
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }


    // --- PRIVATE HELPER FUNCTIONS ---

    private fun populateStatementView(context: Context, view: View, customer: Customer, transactions: List<Transaction>, balance: Double, currencySymbol: String) {
        // Find views
        val tvDocumentTitle = view.findViewById<TextView>(R.id.tvDocumentTitle)
        val tvTotalLabel = view.findViewById<TextView>(R.id.tvTotalLabel)
        val layoutTransactionRows = view.findViewById<LinearLayout>(R.id.layoutTransactionRows)

        // Populate static info
        populateHeader(context, view, customer)
        tvDocumentTitle.text = "صورت حساب"
        tvTotalLabel.text = if (balance >= 0) "مجموع طلب باقی مانده" else "مجموع بدهی باقی مانده"
        view.findViewById<TextView>(R.id.tvTotalAmount).text = String.format(Locale.US, "%,.2f %s", abs(balance), currencySymbol)

        // Dynamically add rows for unsettled transactions
        layoutTransactionRows.removeAllViews()
        val unsettledTransactions = transactions.filter { !it.isSettled }.take(20)
        if (unsettledTransactions.isEmpty()) {
            val tv = TextView(context).apply { text = "معامله باقی مانده وجود ندارد." }
            layoutTransactionRows.addView(tv)
        } else {
            unsettledTransactions.forEach { transaction ->
                val row = LayoutInflater.from(context).inflate(R.layout.list_item_invoice_row, layoutTransactionRows, false)
                // <<< USE THE CORRECTED addTransactionRow FUNCTION >>>
                addTransactionRow(context, row, transaction)
                layoutTransactionRows.addView(row)
            }
        }
    }

    private fun populateReceiptView(context: Context, view: View, transaction: Transaction, customer: Customer?, currencySymbol: String) {
        // Find views
        val tvDocumentTitle = view.findViewById<TextView>(R.id.tvDocumentTitle)
        val tvTotalLabel = view.findViewById<TextView>(R.id.tvTotalLabel)
        val layoutTransactionRows = view.findViewById<LinearLayout>(R.id.layoutTransactionRows)

        // Populate static info
        populateHeader(context, view, customer)
        tvDocumentTitle.text = "رسید معامله #${transaction.id}"
        tvTotalLabel.text = "مبلغ مجموعی"
        view.findViewById<TextView>(R.id.tvTotalAmount).text = String.format(Locale.US, "%,.2f %s", transaction.amount, currencySymbol)

        // Add the single transaction row
        layoutTransactionRows.removeAllViews()
        val row = LayoutInflater.from(context).inflate(R.layout.list_item_invoice_row, layoutTransactionRows, false)
        // <<< USE THE CORRECTED addTransactionRow FUNCTION >>>
        addTransactionRow(context, row, transaction)
        layoutTransactionRows.addView(row)
    }

    private fun addTransactionRow(context: Context, row: View, transaction: Transaction) {
        // <<< THE FIX IS HERE: USE DateUtils.formatMillis >>>
        row.findViewById<TextView>(R.id.tvRowDate).text = DateUtils.formatMillis(context, transaction.dateMillis)

        row.findViewById<TextView>(R.id.tvRowDescription).text = transaction.description
        val amountToFormat = transaction.remainingAmount ?: transaction.amount ?: 0.0
        val formattedAmount = String.format(Locale.US, "%,.2f", amountToFormat)
        row.findViewById<TextView>(R.id.tvRowAmount).text = formattedAmount
    }

    private fun populateHeader(context: Context, view: View, customer: Customer?) {
        val tvShopName = view.findViewById<TextView>(R.id.tvShopName)
        val tvShopAddress = view.findViewById<TextView>(R.id.tvShopAddress)
        val imgLogo = view.findViewById<ImageView>(R.id.imgShopLogo)
        val tvCustomerName = view.findViewById<TextView>(R.id.tvCustomerName)
        val tvDocumentDate = view.findViewById<TextView>(R.id.tvDocumentDate)
        val layoutCustomerInfo = view.findViewById<LinearLayout>(R.id.layoutCustomerInfo)

        tvShopName.text = SettingsManager.getShopName(context) ?: context.getString(R.string.app_name)
        val shopAddress = SettingsManager.getShopAddress(context)
        tvShopAddress.isVisible = !shopAddress.isNullOrBlank()
        tvShopAddress.text = shopAddress

        layoutCustomerInfo.isVisible = customer != null
        tvCustomerName.text = customer?.name ?: ""

        // <<< THE FIX IS HERE: USE DateUtils.formatMillis >>>
        tvDocumentDate.text = "تاریخ: ${DateUtils.formatMillis(context, System.currentTimeMillis())}"

        val logoFile = File(context.filesDir, "shop_logo.png")
        if (logoFile.exists()) {
            try {
                BitmapFactory.decodeFile(logoFile.absolutePath)?.let { imgLogo.setImageBitmap(it); imgLogo.isVisible = true }
            } catch (e: Exception) { imgLogo.isVisible = false }
        }
    }

    private fun createBitmapFromView(view: View): Bitmap? {
        return try {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), // Fixed width for consistency
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val bitmap = createBitmap(view.measuredWidth, view.measuredHeight)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e("DocumentGenerator", "Failed to create bitmap from view", e)
            null
        }
    }
}