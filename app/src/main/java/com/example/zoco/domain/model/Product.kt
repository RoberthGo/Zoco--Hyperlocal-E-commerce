package com.example.zoco.domain.model

enum class ProductCategory {
    ALL,
    BAKERY,
    PRODUCE,
    DAIRY,
    CRAFTS
}

data class Product(
    val id: Long = 0,
    val name: String,
    val description: String,
    val price: Double,
    val category: ProductCategory,
    val producerName: String,
    val isExpressDelivery: Boolean = false,
    val inStock: Boolean = true
)
