package com.example.roznamcha.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.databinding.ListItemCustomerBinding

class CustomerAdapter(
    // The adapter will accept a "lambda" function as a click listener.
    // This is a modern, clean way to handle clicks without interfaces.
    private val onItemClicked: (Customer) -> Unit
) : ListAdapter<Customer, CustomerAdapter.CustomerViewHolder>(CustomerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        return CustomerViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClicked)
    }

    class CustomerViewHolder(private val binding: ListItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: Customer, onItemClicked: (Customer) -> Unit) {
            binding.tvCustomerName.text = customer.name
            binding.tvCustomerContact.text = customer.contactInfo

            // Set the click listener for the entire row
            itemView.setOnClickListener { onItemClicked(customer) }
        }

        companion object {
            fun from(parent: ViewGroup): CustomerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCustomerBinding.inflate(layoutInflater, parent, false)
                return CustomerViewHolder(binding)
            }
        }
    }
}

// DiffUtil helps the adapter efficiently update the list
class CustomerDiffCallback : DiffUtil.ItemCallback<Customer>() {
    override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
        return oldItem == newItem
    }
}