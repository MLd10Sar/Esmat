package com.example.roznamcha.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction
import java.util.*
import kotlin.math.abs

object StatementGenerator {

    private const val PAGE_WIDTH_DP = 450
    private const val PADDING_DP = 24

    fun createStatementBitmap(
        context: Context,
        customer: Customer,
        transactions: List<Transaction>,
        balance: Double,
        currencySymbol: String
    ): Bitmap {
        // --- 1. CONVERT DP TO PIXELS ---
        val density = context.resources.displayMetrics.density
        val pageWidthPx = (PAGE_WIDTH_DP * density).toInt()
        val paddingPx = (PADDING_DP * density).toInt()

        // --- 2. SETUP PAINTS ---
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24 * density
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22 * density
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12 * density
            color = Color.DKGRAY
            textAlign = Paint.Align.LEFT
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14 * density
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }
        val balanceLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16 * density
            color = ContextCompat.getColor(context, R.color.negative_red)
        }
        val balanceValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28 * density
            color = ContextCompat.getColor(context, R.color.negative_red)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12 * density
            color = Color.BLACK
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        // --- 3. CALCULATE TOTAL HEIGHT ---
        var totalHeight = (paddingPx * 2)
        totalHeight += (80 * density).toInt() // Header
        totalHeight += (120 * density).toInt() // Balance card
        val unsettled = transactions.filter { !it.isSettled }.take(10)
        totalHeight += (unsettled.size * 25 * density).toInt()
        totalHeight += (60 * density).toInt() // Footer

        // --- 4. CREATE BITMAP & CANVAS ---
        val bitmap = Bitmap.createBitmap(pageWidthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // --- 5. DRAW THE CONTENT ---
        var yPos = paddingPx.toFloat()

        // Draw Header
        canvas.drawText("صورت حساب", (pageWidthPx - paddingPx).toFloat(), yPos + 30, titlePaint)
        val shopName = SettingsManager.getShopName(context) ?: "Roznamcha"
        canvas.drawText(shopName, paddingPx.toFloat(), yPos + 30, headerPaint)
        yPos += 70

        // Draw Customer Info
        canvas.drawLine(paddingPx.toFloat(), yPos, (pageWidthPx - paddingPx).toFloat(), yPos, linePaint)
        yPos += 30
        canvas.drawText("برای محترم: ${customer.name}", (pageWidthPx - paddingPx).toFloat(), yPos, bodyPaint)
        val dateText = "تاریخ: ${DateUtils.formatMillis(context, System.currentTimeMillis())}"
        canvas.drawText(dateText, paddingPx.toFloat() + bodyPaint.measureText(dateText), yPos, bodyPaint)
        yPos += 50

        // Draw Balance
        canvas.drawText("مجموعه باقی مانده", (pageWidthPx / 2f), yPos, balanceLabelPaint.apply { textAlign = Paint.Align.CENTER })
        yPos += 40
        val balanceString = String.format(Locale.US, "%,.2f %s", abs(balance), currencySymbol)
        canvas.drawText(balanceString, (pageWidthPx / 2f), yPos, balanceValuePaint.apply { textAlign = Paint.Align.CENTER })
        yPos += 50

        // Draw Transaction List
        canvas.drawText("آخرین معاملات باقی مانده:", (pageWidthPx - paddingPx).toFloat(), yPos, bodyPaint)
        yPos += 25
        unsettled.forEach { tx ->
            val date = DateUtils.formatMillis(context, tx.dateMillis)
            val desc = tx.description
            val amt = String.format(Locale.US, "%,.2f", tx.remainingAmount)
            canvas.drawText(date, (pageWidthPx - paddingPx).toFloat(), yPos, rowPaint.apply{ textAlign = Paint.Align.RIGHT })
            canvas.drawText(desc, (pageWidthPx / 2f), yPos, rowPaint.apply{ textAlign = Paint.Align.CENTER })
            canvas.drawText(amt, paddingPx.toFloat(), yPos, rowPaint.apply{ textAlign = Paint.Align.LEFT })
            yPos += 25
        }
        yPos += 20

        // Draw Footer
        canvas.drawLine(paddingPx.toFloat(), yPos, (pageWidthPx - paddingPx).toFloat(), yPos, linePaint)
        yPos += 30
        canvas.drawText("تشکر از همکاری شما!", (pageWidthPx / 2f), yPos, subHeaderPaint.apply { textAlign = Paint.Align.CENTER })

        return bitmap
    }
}