package com.example.genshin_shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserCartAdapter(
    private var cartItems: MutableList<OrderItem>,
    private val onQuantityChanged: ((String, Int) -> Unit)? = null,
    private val onSelectionChanged: ((List<OrderItem>) -> Unit)? = null, // New callback
    private val readOnly: Boolean = false
) : RecyclerView.Adapter<UserCartAdapter.CartViewHolder>() {

    private val selectedItems = mutableSetOf<String>() // Track selected item IDs

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val selectItemCheckBox: CheckBox? = itemView.findViewById(R.id.checkbox_select_item)
        private val productName: TextView = itemView.findViewById(R.id.productNameTextView)
        private val productPrice: TextView = itemView.findViewById(R.id.productPriceTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val increaseQuantityButton: ImageView? = itemView.findViewById(R.id.increaseQuantityButton)
        private val decreaseQuantityButton: ImageView? = itemView.findViewById(R.id.decreaseQuantityButton)

        fun bind(cartItem: OrderItem) {
            productName.text = cartItem.name
            quantityTextView.text = "Qty: ${cartItem.quantity}"
            productPrice.text = String.format("RM%.2f", cartItem.totalPrice)

            if (!readOnly) {
                increaseQuantityButton?.setOnClickListener {
                    onQuantityChanged?.invoke(cartItem.itemId, cartItem.quantity + 1)
                }
                decreaseQuantityButton?.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        onQuantityChanged?.invoke(cartItem.itemId, cartItem.quantity - 1)
                    }
                }
            } else {
                // Hide buttons if in read-only mode
                increaseQuantityButton?.visibility = View.GONE
                decreaseQuantityButton?.visibility = View.GONE
            }

            // Handle item selection (if checkbox is used)
            selectItemCheckBox?.apply {
                visibility = if (readOnly) View.GONE else View.VISIBLE
                isChecked = selectedItems.contains(cartItem.itemId)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedItems.add(cartItem.itemId)
                    } else {
                        selectedItems.remove(cartItem.itemId)
                    }
                    // Notify selection change
                    onSelectionChanged?.invoke(getSelectedItems())
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val layout = if (readOnly) R.layout.item_summary_cart else R.layout.item_cart
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount() = cartItems.size

    fun updateItems(newItems: List<OrderItem>) {
        cartItems.clear()
        cartItems.addAll(newItems)
        notifyDataSetChanged()
    }

    // Retrieve selected items for checkout
    fun getSelectedItems(): List<OrderItem> {
        return cartItems.filter { selectedItems.contains(it.itemId) }
    }
}
