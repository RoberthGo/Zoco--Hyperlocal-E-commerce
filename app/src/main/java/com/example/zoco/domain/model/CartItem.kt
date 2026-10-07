package com.example.zoco.domain.model

data class CartItem(
    val product: Product,
    val quantity: Int = 1
)
