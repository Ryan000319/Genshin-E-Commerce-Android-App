package com.example.genshin_shop

data class Character(
    val id: String = "",         // Unique ID for Firebase
    val name: String = "",       // Character Name
    val videoUrl: String = "",   // URL to the video in Firebase Storage
    val keyword: String = "",    // Keyword for search functionality
    val stickerUrl: String = ""  // URL to the sticker image in Firebase Storage
)
