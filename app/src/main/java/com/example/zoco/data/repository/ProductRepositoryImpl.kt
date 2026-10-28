package com.example.zoco.data.repository

import android.util.Log
import com.example.zoco.data.local.ProductDao
import com.example.zoco.data.local.ProductEntity
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.api.ProductApiService
import com.example.zoco.data.remote.dto.ProductDto
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio híbrido (Single Source of Truth con Room + Retrofit).
 * La base de datos local SQLite (Room) es la fuente inmediata de la verdad para la UI.
 * Las operaciones intentan sincronizar con la API remota de forma protegida para no
 * interrumpir la ejecución si el servidor aún no está desplegado o no hay conexión.
 */
class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val productApiService: ProductApiService = RetrofitClient.productApiService
) : ProductRepository {

    companion object {
        private const val TAG = "ProductRepository"
    }

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProductsFiltered(
        query: String,
        category: ProductCategory,
        expressOnly: Boolean
    ): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }.filter { product ->
                val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) || product.description.contains(query, ignoreCase = true)
                val matchesCategory = category == ProductCategory.ALL || product.category == category
                val matchesExpress = !expressOnly || product.isExpressDelivery
                matchesQuery && matchesCategory && matchesExpress
            }
        }
    }

    override suspend fun getProductById(id: Long): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    override suspend fun insertProduct(product: Product): Long {
        // 1. Guardar primero en la base de datos local (Room)
        val localId = productDao.insertProduct(ProductEntity.fromDomain(product))

        // 2. CONEXIÓN WEB API: Enviar sincronización POST a la API remota de manera segura
        try {
            val productToSend = product.copy(id = localId)
            val remoteResult = productApiService.createProduct(ProductDto.fromDomain(productToSend))
            Log.d(TAG, "Producto sincronizado con éxito en Web API: id=${remoteResult.id}")
        } catch (e: Exception) {
            // Si la API remota no está encendida aún o no hay internet, registramos advertencia sin tronar la app
            Log.w(TAG, "No se pudo sincronizar producto con API remota (modo offline activo): ${e.message}")
        }

        return localId
    }

    override suspend fun updateProduct(product: Product): Boolean {
        // 1. Actualizar en Room localmente
        val rowsAffected = productDao.updateProduct(ProductEntity.fromDomain(product))

        // 2. CONEXIÓN WEB API: Enviar sincronización PUT al servidor remoto
        if (rowsAffected > 0) {
            try {
                productApiService.updateProduct(product.id, ProductDto.fromDomain(product))
                Log.d(TAG, "Actualización enviada con éxito a Web API: id=${product.id}")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar producto en API remota (se conserva cambio local): ${e.message}")
            }
        }

        return rowsAffected > 0
    }

    override suspend fun deleteProduct(product: Product): Boolean {
        return deleteProductById(product.id)
    }

    override suspend fun deleteProductById(id: Long): Boolean {
        // 1. Eliminar de Room localmente
        val rowsDeleted = productDao.deleteProductById(id)

        // 2. CONEXIÓN WEB API: Enviar sincronización DELETE al servidor remoto
        if (rowsDeleted > 0) {
            try {
                productApiService.deleteProduct(id)
                Log.d(TAG, "Baja enviada con éxito a Web API: id=$id")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo eliminar producto en API remota: ${e.message}")
            }
        }

        return rowsDeleted > 0
    }

    /**
     * CONEXIÓN WEB API: Método para sincronizar productos remotos hacia la base de datos local.
     * Consulta GET /products en el backend y guarda/actualiza los registros en Room.
     */
    override suspend fun syncProductsFromRemote(): Boolean {
        return try {
            val remoteProducts = productApiService.getProducts()
            if (remoteProducts.isNotEmpty()) {
                val entities = remoteProducts.map { ProductEntity.fromDomain(it.toDomain()) }
                productDao.insertProducts(entities)
                Log.d(TAG, "Sincronización remota completada: ${entities.size} productos descargados.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sincronización remota no disponible por ahora: ${e.message}")
            false
        }
    }

    override suspend fun populateInitialDataIfEmpty() {
        if (productDao.getProductCount() == 0) {
            val initialProducts = listOf(
                ProductEntity(
                    name = "Pan Artesanal de Masa Madre",
                    description = "Elaborado localmente con fermentación lenta de 24 horas.",
                    price = 45.0,
                    category = ProductCategory.BAKERY.name,
                    producerName = "Panadería Don Pedro",
                    isExpressDelivery = true,
                    inStock = true
                ),
                ProductEntity(
                    name = "Miel de Abeja Pura 500g",
                    description = "Cosechada de apiarios sustentables de la región.",
                    price = 110.0,
                    category = ProductCategory.PRODUCE.name,
                    producerName = "Apícola del Valle",
                    isExpressDelivery = true,
                    inStock = true
                ),
                ProductEntity(
                    name = "Queso Fresco de Rancho",
                    description = "100% leche de vaca natural sin conservadores.",
                    price = 85.0,
                    category = ProductCategory.DAIRY.name,
                    producerName = "Lácteos Santa Rosa",
                    isExpressDelivery = false,
                    inStock = true
                ),
                ProductEntity(
                    name = "Canasta Tejida a Mano",
                    description = "Hecha de palma natural por artesanos de la comunidad.",
                    price = 180.0,
                    category = ProductCategory.CRAFTS.name,
                    producerName = "Manos Creativas",
                    isExpressDelivery = false,
                    inStock = true
                ),
                ProductEntity(
                    name = "Manzanas Orgánicas (1 kg)",
                    description = "Frescas, sin pesticidas, recién cortadas del huerto.",
                    price = 40.0,
                    category = ProductCategory.PRODUCE.name,
                    producerName = "Huerta Los Nogales",
                    isExpressDelivery = true,
                    inStock = true
                )
            )
            productDao.insertProducts(initialProducts)
        }
    }
}
