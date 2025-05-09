package com.example.roznamcha.ui.addedit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import com.example.roznamcha.R
import com.example.roznamcha.TransactionCategory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * This is the BottomSheet menu for adding a new transaction.
 * This version uses the classic findViewById to avoid View Binding issues.
 */
class AddTransactionMenuFragment : BottomSheetDialogFragment() {

    // We no longer use a binding variable.

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout directly. This returns a View.
        return inflater.inflate(R.layout.fragment_add_transaction_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use findViewById to get references to the TextViews.
        // The 'view' parameter is the root LinearLayout of your fragment's layout.
        val menuAddSale = view.findViewById<TextView>(R.id.menu_add_sale)
        val menuAddPurchase = view.findViewById<TextView>(R.id.menu_add_purchase)
        val menuAddDebt = view.findViewById<TextView>(R.id.menu_add_debt)
        val menuAddReceivable = view.findViewById<TextView>(R.id.menu_add_receivable)
        val menuAddExpense = view.findViewById<TextView>(R.id.menu_add_expense)

        // Set click listeners on these views.
        menuAddSale.setOnClickListener { navigateToAddEdit(TransactionCategory.SALE) }
        menuAddPurchase.setOnClickListener { navigateToAddEdit(TransactionCategory.PURCHASE) }
        menuAddDebt.setOnClickListener { navigateToAddEdit(TransactionCategory.DEBT) }
        menuAddReceivable.setOnClickListener { navigateToAddEdit(TransactionCategory.RECEIVABLE) }
        menuAddExpense.setOnClickListener { navigateToAddEdit(TransactionCategory.OTHER_EXPENSE) }
    }

    /**
     * Navigates to the main Add/Edit screen with the chosen category.
     */

    private fun navigateToAddEdit(category: TransactionCategory) {
        // Dismiss the current bottom sheet
        dismiss()
        try {
            val bundle = Bundle().apply {
                putString("category", category.name)
                putLong("transactionId", -1L)
            }
            // <<< CORRECTED ID HERE >>>
            // Use the ID that is actually in your activity_main.xml
            requireActivity().findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_global_addEditTransactionFragment, bundle)
        } catch (e: Exception) {
            Log.e("AddMenuFragment", "Navigation failed!", e)
        }
    }

    // No onDestroyView needed as we are not managing a binding object.
}