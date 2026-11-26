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
    val description: String = "",
    val price: Double,
    val category: ProductCategory = ProductCategory.ALL,
    val producerName: String,
    val isExpressDelivery: Boolean = false,
    val inStock: Boolean = true,
    val isRemote: Boolean = false,
    val imageUrl: String? = null,
    val stock: Int = 10,
    val pendingSync: Boolean = false
)
