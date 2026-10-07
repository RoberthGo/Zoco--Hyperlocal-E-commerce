package com.example.zoco.domain.repository

import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun getProductsFiltered(query: String, category: ProductCategory, expressOnly: Boolean): Flow<List<Product>>
    suspend fun insertProduct(product: Product): Long
    suspend fun populateInitialDataIfEmpty()
}
