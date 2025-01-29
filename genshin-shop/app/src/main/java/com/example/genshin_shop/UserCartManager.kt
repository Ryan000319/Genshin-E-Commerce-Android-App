package com.example.genshin_shop

// CartManager.kt
object UserCartManager {
    private val cartItems = mutableListOf<OrderItem>()

    fun addToCart(orderItem: OrderItem) {
        val existingItem = cartItems.find { it.itemId == orderItem.itemId }
        if (existingItem != null) {
            existingItem.quantity += orderItem.quantity
        } else {
            cartItems.add(orderItem)
        }
    }

    fun updateQuantity(itemId: String, quantity: Int) {
        cartItems.find { it.itemId == itemId }?.quantity = quantity
    }

    fun getCartItems(): List<OrderItem> = cartItems

    fun clearCart() {
        cartItems.clear()
    }

}
