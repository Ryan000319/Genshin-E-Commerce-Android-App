package com.example.genshin_shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderDetailAdapter(
    private val items: List<OrderItem>,
    private val orderStatus: String,
    private val onRateItem: (OrderItem) -> Unit // Callback to handle rating action
) : RecyclerView.Adapter<OrderDetailAdapter.OrderItemViewHolder>() {

    inner class OrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.item_name)
        val itemQuantityTextView: TextView = itemView.findViewById(R.id.item_quantity)
        val itemPriceTextView: TextView = itemView.findViewById(R.id.item_price)
        val itemTotalTextView: TextView = itemView.findViewById(R.id.item_total)
        val rateButton: Button = itemView.findViewById(R.id.rate_button) // Button for rating
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_detail, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        val item = items[position]
        holder.itemNameTextView.text = item.name
        holder.itemQuantityTextView.text = "Quantity: ${item.quantity}"
        holder.itemPriceTextView.text = String.format("Price: RM%.2f", item.price)
        holder.itemTotalTextView.text = String.format("Total: RM%.2f", item.totalPrice)

        // Show or hide the "Rate" button based on item's rated status and order status
        if (item.rated || orderStatus != "Delivered") {
            // Hide the "Rate" button if the item is already rated or the order is not delivered
            holder.rateButton.visibility = View.GONE
        } else {
            // Show the "Rate" button for items that can be rated
            holder.rateButton.visibility = View.VISIBLE
            holder.rateButton.setOnClickListener { onRateItem(item) }
        }
    }

    override fun getItemCount(): Int = items.size
}

