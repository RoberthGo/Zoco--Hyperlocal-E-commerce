package com.example.zoco.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val producerName: String,
    val isExpressDelivery: Boolean,
    val inStock: Boolean
) {
    fun toDomain(): Product {
        val cat = runCatching { ProductCategory.valueOf(category) }.getOrDefault(ProductCategory.ALL)
        return Product(
            id = id,
            name = name,
            description = description,
            price = price,
            category = cat,
            producerName = producerName,
            isExpressDelivery = isExpressDelivery,
            inStock = inStock
        )
    }

    companion object {
        fun fromDomain(product: Product): ProductEntity {
            return ProductEntity(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price,
                category = product.category.name,
                producerName = product.producerName,
                isExpressDelivery = product.isExpressDelivery,
                inStock = product.inStock
            )
        }
    }
}
