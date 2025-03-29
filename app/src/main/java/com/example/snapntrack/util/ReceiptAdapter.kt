package com.example.snapntrack.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snapntrack.R

class ReceiptAdapter (  private val items: MutableList<ReceiptItem>,
                        private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtItemName: TextView = view.findViewById(R.id.itemName)
        val txtItemCost: TextView = view.findViewById(R.id.price)

        init {
            view.setOnClickListener {
                onItemClick(adapterPosition) // Handle click event for item editing
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtItemName.text = item.name
        holder.txtItemCost.text = "£${item.cost}"
    }

    override fun getItemCount(): Int = items.size
}