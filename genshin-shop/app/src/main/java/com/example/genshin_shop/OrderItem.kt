package com.example.genshin_shop

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
@Parcelize
data class OrderItem(
    var itemId: String = "",    // Unique identifier for the product/item
    var name: String = "",       // Name of the product/item
    var quantity: Int = 0,       // Quantity of the item ordered
    var price: Double = 0.0,     // Price per unit of the item
    var rated: Boolean = false   // Whether this item has been rated
) : Parcelable {
    // Computed property for the total price of this item
    val totalPrice: Double
        get() = quantity * price

    constructor() : this("", "", 0, 0.0, false)
}
