package com.example.zoco.domain.model

data class Order(
    val id: Long = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val scheduledTime: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "PENDING",
    val isExpress: Boolean = false,
    val itemSummary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRemote: Boolean = false
)
