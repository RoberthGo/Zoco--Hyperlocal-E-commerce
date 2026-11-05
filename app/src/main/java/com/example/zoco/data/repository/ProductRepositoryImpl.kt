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
    private val customApiService: ProductApiService? = null
) : ProductRepository {

    private val productApiService: ProductApiService
        get() = customApiService ?: RetrofitClient.productApiService

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
     * Si la Web API no está activa o falla la conexión, elimina los productos remotos
     * para asegurar que no se muestren cosas de la API.
     * Incluye detección y fallback automático entre IPs candidatas (Wi-Fi, Localhost, Emulador).
     */
    override suspend fun syncProductsFromRemote(): Boolean {
        // 1. Probar con la URL actualmente configurada
        if (executeSync()) {
            return true
        }

        // 2. Si falló la conexión (ej. cambio de red Wi-Fi o IP), probar candidatos automáticamente
        if (customApiService == null) {
            for (candidate in RetrofitClient.CANDIDATE_URLS) {
                if (candidate != RetrofitClient.currentBaseUrl) {
                    RetrofitClient.updateBaseUrl(candidate)
                    if (executeSync()) {
                        Log.i(TAG, "Reconexión automática exitosa con URL candidata: $candidate")
                        return true
                    }
                }
            }
        }

        // 3. Si ninguna respondió, el servidor está realmente apagado o inaccesible
        Log.w(TAG, "Web API no activa en ninguna URL candidata. Eliminando productos remotos de Room para mostrar únicamente catálogo local.")
        productDao.deleteRemoteProducts()
        return false
    }

    private suspend fun executeSync(): Boolean {
        return try {
            val remoteProducts = productApiService.getProducts()
            if (remoteProducts.isNotEmpty()) {
                productDao.deleteRemoteProducts()
                val entities = remoteProducts.map { 
                    ProductEntity.fromDomain(it.toDomain()).copy(isRemote = true)
                }
                productDao.insertProducts(entities)
                Log.d(TAG, "Sincronización remota completada: ${entities.size} productos descargados desde ${RetrofitClient.currentBaseUrl}")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun populateInitialDataIfEmpty() {
        if (productDao.getProductCount() == 0) {
            val initialProducts = listOf(
                ProductEntity(
                    id = 101,
                    name = "[Local] Pan de Pueblo Horneado a la Leña",
                    description = "Elaborado localmente con fermentación tradicional en horno de leña comunitario.",
                    price = 40.0,
                    category = ProductCategory.BAKERY.name,
                    producerName = "Panadería San José [Local Offline]",
                    isExpressDelivery = true,
                    inStock = true,
                    isRemote = false
                ),
                ProductEntity(
                    id = 102,
                    name = "[Local] Miel Silvestre de Monte (500g)",
                    description = "Miel pura no pasteurizada, conservada en el dispositivo en modo offline.",
                    price = 95.0,
                    category = ProductCategory.PRODUCE.name,
                    producerName = "Apícola Los Sauces [Local Offline]",
                    isExpressDelivery = true,
                    inStock = true,
                    isRemote = false
                ),
                ProductEntity(
                    id = 103,
                    name = "[Local] Queso Fresco Ranchero de Hoja",
                    description = "Queso artesanal envuelto en hoja de plátano, 100% natural de establo local.",
                    price = 65.0,
                    category = ProductCategory.DAIRY.name,
                    producerName = "Rancho San Antonio [Local Offline]",
                    isExpressDelivery = false,
                    inStock = true,
                    isRemote = false
                ),
                ProductEntity(
                    id = 104,
                    name = "[Local] Canasta de Vara Tejida Tradicional",
                    description = "Artesanía de vara de sauz tejida a mano disponible en catálogo local.",
                    price = 110.0,
                    category = ProductCategory.CRAFTS.name,
                    producerName = "Artesanías El Roble [Local Offline]",
                    isExpressDelivery = false,
                    inStock = true,
                    isRemote = false
                ),
                ProductEntity(
                    id = 105,
                    name = "[Local] Naranjas Dulces de Huerto (2 kg)",
                    description = "Cosecha fresca local disponible sin requerir conexión a internet.",
                    price = 35.0,
                    category = ProductCategory.PRODUCE.name,
                    producerName = "Huerta La Cañada [Local Offline]",
                    isExpressDelivery = true,
                    inStock = true,
                    isRemote = false
                )
            )
            productDao.insertProducts(initialProducts)
        }
    }
}
