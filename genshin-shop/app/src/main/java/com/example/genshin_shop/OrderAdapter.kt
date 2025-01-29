package com.example.genshin_shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderAdapter(
    private var orders: MutableList<Order>,
    private val isAdmin: Boolean,
    private val onUserAction: (Order, UserAction) -> Unit // Single lambda for handling user actions){}
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    enum class UserAction { VIEW_DETAILS, MODIFY_STATUS }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdTextView: TextView = itemView.findViewById(R.id.order_id)
        val orderTotalTextView: TextView = itemView.findViewById(R.id.order_total)
        val orderStatusTextView: TextView = itemView.findViewById(R.id.order_status)
        val viewDetailsPress: View = itemView.findViewById(R.id.order_card)
        val modifyStatusButton: Button = itemView.findViewById(R.id.modify_status_button) // Admin-specific button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        // Set order details
        holder.orderIdTextView.text = "Order: ${order.id.take(8)}"
        holder.orderTotalTextView.text = String.format("Total: RM%.2f", order.totalAmount)
        holder.orderStatusTextView.text = "Status: ${order.status}"

        if (isAdmin) {
            // Show Modify Status Button for Admin
            holder.modifyStatusButton.visibility = View.VISIBLE
            holder.modifyStatusButton.setOnClickListener {
                onUserAction(order, UserAction.MODIFY_STATUS) // Notify parent activity
            }
        } else {
            // Hide Modify Status Button for Users
            holder.modifyStatusButton.visibility = View.GONE
        }

        // Make the entire order card clickable to view details
        holder.viewDetailsPress.setOnClickListener {
            onUserAction(order, UserAction.VIEW_DETAILS)
        }
    }


    fun updateOrders(newOrders: List<Order>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = orders.size
}

