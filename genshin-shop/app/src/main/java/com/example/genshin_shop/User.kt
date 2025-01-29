package com.example.genshin_shop

data class User(
    val username: String? = null,
    val email: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val coins: Int? = 0,
    val avatarUrl: String? = "avatar1"
)
