package com.example.roznamcha.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roznamcha.data.db.entity.InventoryItem
import com.example.roznamcha.databinding.ListItemInventoryBinding
import java.util.*

// Interface for handling clicks on items
interface OnInventoryItemClickListener {
    fun onItemClick(item: InventoryItem)
    fun onItemLongClick(item: InventoryItem): Boolean
}

class InventoryAdapter(private val clickListener: OnInventoryItemClickListener) :
    ListAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        return InventoryViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, clickListener)
    }

    class InventoryViewHolder private constructor(private val binding: ListItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItem, clickListener: OnInventoryItemClickListener) {
            binding.tvItemName.text = item.name
            // Format quantity and unit together
            val quantityText = "${item.quantity} ${item.unit ?: ""}".trim()
            binding.tvItemQuantity.text = quantityText

            // Show remarks if they exist
            binding.tvItemRemarks.isVisible = !item.remarks.isNullOrBlank()
            binding.tvItemRemarks.text = item.remarks

            // Set click listeners
            binding.root.setOnClickListener { clickListener.onItemClick(item) }
            binding.root.setOnLongClickListener { clickListener.onItemLongClick(item) }
        }

        companion object {
            fun from(parent: ViewGroup): InventoryViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemInventoryBinding.inflate(layoutInflater, parent, false)
                return InventoryViewHolder(binding)
            }
        }
    }
}

class InventoryDiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
    override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
        return oldItem == newItem
    }
}