package com.example.zoco.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "productId") val productId: Long? = null,
    @Json(name = "productName") val productName: String = "",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "unitPrice") val unitPrice: Double = 0.0,
    @Json(name = "subtotal") val subtotal: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "customerName") val customerName: String = "",
    @Json(name = "customerPhone") val customerPhone: String = "",
    @Json(name = "deliveryAddress") val deliveryAddress: String = "",
    @Json(name = "scheduledTime") val scheduledTime: String = "",
    @Json(name = "totalAmount") val totalAmount: Double = 0.0,
    @Json(name = "status") val status: String = "PENDING",
    @Json(name = "isExpress") val isExpress: Boolean = false,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "items") val items: List<OrderItemDto> = emptyList(),
    @Json(name = "createdAt") val createdAt: String? = null
)
