package com.example.zoco.data.remote.dto

import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "price") val price: Double,
    @Json(name = "category") val category: String = ProductCategory.ALL.name,
    @Json(name = "producerName") val producerName: String,
    @Json(name = "isExpressDelivery") val isExpressDelivery: Boolean = false,
    @Json(name = "inStock") val inStock: Boolean = true,
    @Json(name = "stock") val stock: Int = 10,
    @Json(name = "sellerId") val sellerId: Long? = null,
    @Json(name = "sellerUsername") val sellerUsername: String? = null,
    // Solo lectura: URL de la foto en el servidor. La foto se sube aparte (POST products/{id}/image).
    @Json(name = "imageUrl") val imageUrl: String? = null
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
            inStock = inStock,
            stock = stock,
            sellerId = sellerId,
            sellerUsername = sellerUsername,
            imageUrl = imageUrl
        )
    }

    companion object {
        fun fromDomain(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price,
                category = product.category.name,
                producerName = product.producerName,
                isExpressDelivery = product.isExpressDelivery,
                inStock = product.inStock,
                stock = product.stock,
                sellerId = product.sellerId,
                sellerUsername = product.sellerUsername
            )
        }
    }
}
