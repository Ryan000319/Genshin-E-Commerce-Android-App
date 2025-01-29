package com.example.genshin_shop

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    var id: String = "",
    val name: String = "",
    val price: String = "",       // Current discounted price
    val originalPrice: String = "", // Original price for calculating discount percentage
    val description: String = "",
    val mainImage: String = "",
    var quantity: Int = 0,
    val category: String = "",
    var available: Boolean = true,
    var wishlistCount: Int = 0,
    var isWishlisted: Boolean = false,
    var images: List<String> = emptyList(),
    var productReviews: List<ProductRating> = emptyList()
) : Parcelable
