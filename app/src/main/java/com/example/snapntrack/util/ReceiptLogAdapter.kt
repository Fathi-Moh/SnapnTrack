package com.example.snapntrack.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snapntrack.R


class ReceiptLogAdapter (
    private val receipts: List<ReceiptLogItem>,
    private val onItemClick: (ReceiptLogItem) -> Unit
) : RecyclerView.Adapter<ReceiptLogAdapter.ReceiptViewHolder>() {

    class ReceiptViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtShopName: TextView = view.findViewById(R.id.shopName)
        val txtDate: TextView = view.findViewById(R.id.date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_log, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val receipt = receipts[position]
        holder.txtShopName.text = receipt.shopName
        holder.txtDate.text = receipt.issueDate

        holder.itemView.setOnClickListener { onItemClick(receipt) }
    }

    override fun getItemCount(): Int = receipts.size
}