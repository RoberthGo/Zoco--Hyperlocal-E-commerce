package com.example.zoco.domain.model

data class MediaItem(
    val id: Long = 0,
    val productId: Long? = null,
    val uri: String,
    val mediaType: String = "IMAGE",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
