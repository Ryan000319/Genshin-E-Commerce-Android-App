package com.example.genshin_shop

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductRating(
    val userId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0L
) : Parcelable
