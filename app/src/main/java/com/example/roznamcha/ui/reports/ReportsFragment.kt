package com.example.roznamcha.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.roznamcha.DateRange
import com.example.roznamcha.R
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.databinding.FragmentReportsBinding
import java.util.*

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels {
        ReportsViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.reports_title)

        setupDateFilterChips()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupDateFilterChips() {
        binding.chipThisMonthReports.isChecked = true
        binding.chipGroupDateFilterReports.setOnCheckedStateChangeListener { _, checkedIds ->
            val range = when (checkedIds.firstOrNull()) {
                R.id.chipTodayReports -> DateRange.TODAY
                R.id.chipThisWeekReports -> DateRange.THIS_WEEK
                R.id.chipThisMonthReports -> DateRange.THIS_MONTH
                R.id.chipAllTimeReports -> DateRange.ALL_TIME
                else -> return@setOnCheckedStateChangeListener
            }
            viewModel.setDateRange(range)
        }
    }

    private fun setupObservers() {
        val currencySymbol = SettingsManager.getCurrency(requireContext()) ?: "AFN"

        // Observer for Health Score
        viewModel.financialHealthScore.observe(viewLifecycleOwner) { (score, rating) ->
            binding.tvHealthScore.text = score.toString()
            binding.tvHealthRating.text = rating
        }

        // Observers for Top Lists
        viewModel.topExpenses.observe(viewLifecycleOwner) { expenses ->
            val formattedList = expenses.sortedByDescending { it.totalAmount }.map { categoryTotal ->
                Pair(
                    getCategoryDisplayName(categoryTotal.category),
                    String.format(Locale.US, "%,.2f %s", categoryTotal.totalAmount, currencySymbol)
                )
            }
            populateTopList(binding.layoutTopExpenses, formattedList)
        }

        viewModel.topSellingItems.observe(viewLifecycleOwner) { items ->
            // <<< CORRECTED: Now correctly accesses the properties of ItemSaleTotal >>>
            val formattedList = items.map { item ->
                Pair(item.description, String.format(Locale.US, "%,.1f", item.totalQuantity))
            }
            populateTopList(binding.layoutTopSellingItems, formattedList)
        }

        viewModel.topCustomers.observe(viewLifecycleOwner) { customers ->
            // <<< CORRECTED: Now correctly accesses the properties of CustomerSaleTotal >>>
            val formattedList = customers.map { customer ->
                Pair(
                    customer.customerName ?: "نا معلوم",
                    String.format(Locale.US, "%,.2f %s", customer.totalAmount, currencySymbol)
                )
            }
            populateTopList(binding.layoutTopCustomers, formattedList)
        }
    }

    // --- Helper Functions ---
    private fun populateTopList(layout: LinearLayout, items: List<Pair<String, String>>) {
        layout.removeAllViews()
        if (items.isEmpty()) {
            val tv = TextView(context).apply { text = "معلوماتی برای نمایش وجود ندارد." }
            layout.addView(tv)
        } else {
            items.forEachIndexed { index, pair ->
                val rowView = LayoutInflater.from(context).inflate(R.layout.list_item_report_row, layout, false)
                rowView.findViewById<TextView>(R.id.tvRank).text = "${index + 1}."
                rowView.findViewById<TextView>(R.id.tvName).text = pair.first
                rowView.findViewById<TextView>(R.id.tvValue).text = pair.second
                layout.addView(rowView)
            }
        }
    }

    private fun getCategoryDisplayName(categoryName: String): String {
        val categoryEnum = TransactionCategory.values().find { it.name == categoryName }
        return if (categoryEnum != null) {
            getString(categoryEnum.displayNameResId)
        } else {
            categoryName
        }
    }
}