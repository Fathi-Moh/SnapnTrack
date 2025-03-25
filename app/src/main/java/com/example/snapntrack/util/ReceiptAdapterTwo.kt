package com.example.snapntrack.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snapntrack.R

class ReceiptAdapterTwo (  private val items: MutableList<ReceiptItem>
) : RecyclerView.Adapter<ReceiptAdapterTwo.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtItemName: TextView = view.findViewById(R.id.itemName)
        val txtItemCost: TextView = view.findViewById(R.id.price)
        val txtItemQuan: TextView = view.findViewById(R.id.quan)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_two, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtItemName.text = item.name
        holder.txtItemCost.text = "$${item.cost}"
        holder.txtItemQuan.text = "${item.qua}"
    }

    override fun getItemCount(): Int = items.size
}