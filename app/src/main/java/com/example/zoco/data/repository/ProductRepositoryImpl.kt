package com.example.zoco.data.repository

import android.util.Log
import com.example.zoco.data.local.MediaDao
import com.example.zoco.data.local.MediaEntity
import com.example.zoco.data.local.ProductDao
import com.example.zoco.data.local.ProductEntity
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.api.ProductApiService
import com.example.zoco.data.remote.dto.ProductDto
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import com.example.zoco.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repositorio híbrido (Single Source of Truth con Room + Retrofit + MediaDao).
 * La base de datos local SQLite (Room) es la fuente inmediata de la verdad para la UI.
 * Las operaciones intentan sincronizar con la API remota de forma protegida para no
 * interrumpir la ejecución si el servidor aún no está desplegado o no hay conexión.
 */
class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val customApiService: ProductApiService? = null,
    private val mediaDao: MediaDao? = null,
    private val syncScheduler: SyncScheduler? = null
) : ProductRepository {

    private val productApiService: ProductApiService
        get() = customApiService ?: RetrofitClient.productApiService

    companion object {
        private const val TAG = "ProductRepository"
    }

    override fun getProducts(): Flow<List<Product>> {
        return if (mediaDao != null) {
            combine(productDao.getAllProducts(), mediaDao.getAllMedia()) { products, mediaList ->
                products.map { entity ->
                    val media = mediaList.firstOrNull { it.productId == entity.id }
                    entity.toDomain(imageUrl = media?.uri)
                }
            }
        } else {
            productDao.getAllProducts().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getProductsFiltered(
        query: String,
        category: ProductCategory,
        expressOnly: Boolean
    ): Flow<List<Product>> {
        return getProducts().map { products ->
            products.filter { product ->
                val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) || product.description.contains(query, ignoreCase = true)
                val matchesCategory = category == ProductCategory.ALL || product.category == category
                val matchesExpress = !expressOnly || product.isExpressDelivery
                matchesQuery && matchesCategory && matchesExpress
            }
        }
    }

    override suspend fun getProductById(id: Long): Product? {
        val entity = productDao.getProductById(id) ?: return null
        val mediaUri = mediaDao?.getMediaById(id)?.uri
        return entity.toDomain(imageUrl = mediaUri)
    }

    override suspend fun insertProduct(product: Product): Long {
        return insertProductWithMedia(product, product.imageUrl)
    }

    override suspend fun insertProductWithMedia(product: Product, mediaUri: String?): Long {
        // 1. Guardar primero en Room, marcado como PENDIENTE de subir (cola offline-first)
        val localId = productDao.insertProduct(
            ProductEntity.fromDomain(product).copy(isRemote = false, pendingSync = true)
        )

        // 2. Guardar multimedia en MediaDao si se proporcionó URI
        if (!mediaUri.isNullOrBlank() && mediaDao != null) {
            try {
                mediaDao.insertMedia(
                    MediaEntity(
                        productId = localId,
                        uri = mediaUri,
                        mediaType = "IMAGE",
                        description = product.name
                    )
                )
                Log.d(TAG, "Multimedia guardada con éxito en MediaDao para producto id=$localId")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo guardar multimedia en MediaDao: ${e.message}")
            }
        }

        // 3. CONEXIÓN WEB API: intentar subirlo ya; si falla queda pendiente y WorkManager
        //    lo reintenta en cuanto haya red (ver SyncPendingWorker).
        pushNowOrSchedule(localId)

        return localId
    }

    override suspend fun getMediaForProduct(productId: Long): List<String> {
        return try {
            mediaDao?.getMediaByProductId(productId)?.first()?.map { it.uri } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun updateProduct(product: Product): Boolean {
        // 1. Actualizar en Room localmente, marcado como PENDIENTE de subir
        val rowsAffected = productDao.updateProduct(
            ProductEntity.fromDomain(product).copy(pendingSync = true)
        )

        if (!product.imageUrl.isNullOrBlank() && mediaDao != null) {
            try {
                mediaDao.deleteMediaByProductId(product.id)
                mediaDao.insertMedia(
                    MediaEntity(
                        productId = product.id,
                        uri = product.imageUrl,
                        mediaType = "IMAGE",
                        description = product.name
                    )
                )
            } catch (_: Exception) {}
        }

        // 2. CONEXIÓN WEB API: PUT (o POST si nunca se había subido); si falla, queda en la cola
        if (rowsAffected > 0) {
            pushNowOrSchedule(product.id)
        }

        return rowsAffected > 0
    }

    /** Intenta subir un producto ahora mismo; si no se puede, agenda el reintento en segundo plano. */
    private suspend fun pushNowOrSchedule(productId: Long) {
        val entity = productDao.getProductById(productId) ?: return
        try {
            pushOne(entity)
        } catch (e: Exception) {
            Log.w(TAG, "Sin conexión con la API; producto id=$productId queda pendiente: ${e.message}")
            syncScheduler?.scheduleSync()
        }
    }

    /**
     * Sube UN producto pendiente. Lanza excepción si falla (red o HTTP).
     * - No existe aún en el servidor (isRemote = false): POST y se reemplaza la fila local por la
     *   que devuelve el servidor (id del servidor, isRemote = true) re-asociando su multimedia.
     * - Ya existe en el servidor (isRemote = true): PUT y se limpia la marca de pendiente.
     */
    private suspend fun pushOne(entity: ProductEntity) {
        val dto = ProductDto.fromDomain(entity.toDomain())
        if (entity.isRemote) {
            productApiService.updateProduct(entity.id, dto)
            productDao.insertProduct(entity.copy(pendingSync = false))
            Log.d(TAG, "Edición subida a la Web API: id=${entity.id}")
        } else {
            val created = productApiService.createProduct(dto.copy(id = 0))
            val serverEntity = ProductEntity.fromDomain(created.toDomain())
                .copy(isRemote = true, pendingSync = false)
            productDao.deleteProductById(entity.id)
            productDao.insertProduct(serverEntity)
            if (serverEntity.id != entity.id) {
                try {
                    mediaDao?.reassignMedia(entity.id, serverEntity.id)
                } catch (_: Exception) {}
            }
            Log.d(TAG, "Producto subido a la Web API: id local=${entity.id} -> id servidor=${serverEntity.id}")
        }
    }

    /**
     * Vacía la cola: sube todos los productos con pendingSync = 1.
     * Se detiene al primer error de red (no tiene caso esperar N timeouts); un error HTTP de un
     * producto no bloquea a los demás. Devuelve true solo si no quedó nada pendiente.
     */
    override suspend fun pushPending(): Boolean {
        var allPushed = true
        for (entity in productDao.getPendingProducts().sortedBy { it.id }) {
            try {
                pushOne(entity)
            } catch (e: IOException) {
                Log.w(TAG, "Sin red al subir pendientes: ${e.message}")
                return false
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo subir producto id=${entity.id}: ${e.message}")
                allPushed = false
            }
        }
        return allPushed
    }

    override suspend fun deleteProduct(product: Product): Boolean {
        return deleteProductById(product.id)
    }

    override suspend fun deleteProductById(id: Long): Boolean {
        val entity = productDao.getProductById(id) ?: return false

        // Regla: ELIMINAR solo con internet. Si el producto existe en el servidor, primero se borra
        // allá; si no hay conexión (o el servidor rechaza) NO se toca nada local y se devuelve false.
        if (entity.isRemote) {
            try {
                val response = productApiService.deleteProduct(id)
                if (!response.isSuccessful && response.code() != 404) {
                    Log.w(TAG, "La Web API rechazó eliminar id=$id (HTTP ${response.code()})")
                    return false
                }
                Log.d(TAG, "Baja enviada con éxito a Web API: id=$id")
            } catch (e: Exception) {
                Log.w(TAG, "Sin conexión: no se puede eliminar producto id=$id: ${e.message}")
                return false
            }
        }

        // Producto solo local (nunca subido) o ya borrado en el servidor: limpiar Room y multimedia
        try {
            mediaDao?.deleteMediaByProductId(id)
        } catch (_: Exception) {}

        return productDao.deleteProductById(id) > 0
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
                // deleteRemoteProducts() ya respeta las filas pendientes; además no se pisa con la
                // versión del servidor ningún producto cuyo cambio local aún no se ha subido.
                productDao.deleteRemoteProducts()
                val pendingIds = productDao.getPendingProducts().map { it.id }.toSet()
                val entities = remoteProducts
                    .filter { it.id !in pendingIds }
                    .map { ProductEntity.fromDomain(it.toDomain()).copy(isRemote = true, pendingSync = false) }
                productDao.insertProducts(entities)
                Log.d(TAG, "Sincronización remota completada: ${entities.size} productos descargados desde ${RetrofitClient.currentBaseUrl}")
            }
            // El servidor respondió: aprovechar la conexión para subir lo que quedó pendiente offline
            pushPending()
            remoteProducts.isNotEmpty()
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
                    isRemote = false,
                    stock = 15
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
                    isRemote = false,
                    stock = 8
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
                    isRemote = false,
                    stock = 12
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
                    isRemote = false,
                    stock = 5
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
                    isRemote = false,
                    stock = 20
                )
            )
            productDao.insertProducts(initialProducts)
        }
    }
}
