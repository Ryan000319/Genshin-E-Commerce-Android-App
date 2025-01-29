package com.example.genshin_shop

import android.os.Parcelable
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Order(
    var id: String = "",            // Order ID
    var userId: String = "",        // User ID who placed the order
    var totalAmount: Double = 0.0,  // Total amount of the order
    var status: String = "",        // Status of the order
    var createdAt: Long = System.currentTimeMillis(), // Timestamp for order creation
    var address: String = "",       // Delivery address
    var items: Map<String, OrderItem> = emptyMap(), // Map of items in the order
    var isRateable: Boolean = false, // Indicates if the order has unrated items
    var coinsApplied: Double = 0.0, // Coins applied for discount
    var deliveryFee: Double = 10.0  // Delivery fee
) : Parcelable {
    fun parseItems(itemsSnapshot: DataSnapshot): Map<String, OrderItem> {
        val itemsMap = mutableMapOf<String, OrderItem>()

        if (itemsSnapshot.exists()) {
            try {
                if (itemsSnapshot.value is List<*>) {
                    // Handle ArrayList format
                    itemsSnapshot.children.forEachIndexed { index, itemSnapshot ->
                        val itemValue = itemSnapshot.getValue(OrderItem::class.java)
                        if (itemValue != null) {
                            itemsMap[index.toString()] = itemValue
                        }
                    }
                } else if (itemsSnapshot.value is Map<*, *>) {
                    // Handle Map format
                    itemsSnapshot.children.forEach { itemSnapshot ->
                        val itemKey = itemSnapshot.key.orEmpty()
                        val itemValue = itemSnapshot.getValue(OrderItem::class.java)
                        if (itemValue != null) {
                            itemsMap[itemKey] = itemValue
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error parsing items: ${e.message}")
            }
        }
        return itemsMap
    }

}
