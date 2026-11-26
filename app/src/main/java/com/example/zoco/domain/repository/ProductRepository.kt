package com.example.zoco.domain.repository

import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import kotlinx.coroutines.flow.Flow

/**
 * Productos del catálogo. La pantalla siempre lee de la base local (rápida y funciona sin internet);
 * el repositorio se encarga de descargar del servidor ([syncProductsFromRemote]) y de subir lo que
 * se creó sin conexión ([pushPending]).
 */
interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun getProductsFiltered(query: String, category: ProductCategory, expressOnly: Boolean): Flow<List<Product>>
    suspend fun getProductById(id: Long): Product?
    suspend fun insertProduct(product: Product): Long
    suspend fun insertProductWithMedia(product: Product, mediaUri: String?): Long
    suspend fun getMediaForProduct(productId: Long): List<String>
    suspend fun updateProduct(product: Product): Boolean
    suspend fun deleteProduct(product: Product): Boolean
    suspend fun deleteProductById(id: Long): Boolean
    suspend fun syncProductsFromRemote(): Boolean

    /** Sube a la Web API los productos creados/editados sin conexión. true si no quedó ninguno pendiente. */
    suspend fun pushPending(): Boolean
    suspend fun populateInitialDataIfEmpty()
}
