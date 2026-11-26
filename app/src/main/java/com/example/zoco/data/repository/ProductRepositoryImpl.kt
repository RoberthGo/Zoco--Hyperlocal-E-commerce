package com.example.zoco.data.repository

import android.util.Log
import com.example.zoco.data.local.ImageStore
import com.example.zoco.data.local.MediaDao
import com.example.zoco.data.local.SampleData
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Implementación de [ProductRepository]. Funciona "primero local":
 *  1. La pantalla siempre lee de la base local (Room), así el catálogo carga al instante y sirve sin internet.
 *  2. Al crear o editar un producto se guarda en Room marcado como "pendiente" y se intenta subir al servidor.
 *     Si no hay red, [SyncPendingWorker] lo reintenta solo cuando vuelva la conexión.
 *  3. Las fotos se guardan como archivo local y se suben aparte ([uploadPendingImage]); al terminar,
 *     la ruta local se reemplaza por la URL del servidor para que todos las vean.
 *  4. Al sincronizar, los productos del servidor reemplazan a los que vinieron de él antes
 *     (pero nunca a los que tienen cambios sin subir). Si el servidor no responde, esos productos
 *     "de servidor" se quitan de la lista para no mostrar datos que podrían estar desactualizados.
 */
class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val customApiService: ProductApiService? = null,
    private val mediaDao: MediaDao? = null,
    private val syncScheduler: SyncScheduler? = null,
    private val imageStore: ImageStore? = null
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

        // 2. Guardar multimedia en MediaDao si se proporcionó URI (copia propia, reducida, lista para subir)
        if (!mediaUri.isNullOrBlank() && mediaDao != null) {
            try {
                val storedUri = imageStore?.saveLocalCopy(mediaUri) ?: mediaUri
                mediaDao.insertMedia(
                    MediaEntity(
                        productId = localId,
                        uri = storedUri,
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
        uploadOrRetryLater(localId)

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
                val storedUri = imageStore?.saveLocalCopy(product.imageUrl) ?: product.imageUrl
                mediaDao.deleteMediaByProductId(product.id)
                mediaDao.insertMedia(
                    MediaEntity(
                        productId = product.id,
                        uri = storedUri,
                        mediaType = "IMAGE",
                        description = product.name
                    )
                )
            } catch (_: Exception) {
            // No se pudo limpiar la multimedia local: no es grave, se limpiará en la siguiente sincronización
        }
        }

        // 2. CONEXIÓN WEB API: PUT (o POST si nunca se había subido); si falla, queda en la cola
        if (rowsAffected > 0) {
            uploadOrRetryLater(product.id)
        }

        return rowsAffected > 0
    }

    /** Intenta subir un producto ahora mismo; si no se puede, agenda el reintento en segundo plano. */
    private suspend fun uploadOrRetryLater(productId: Long) {
        val entity = productDao.getProductById(productId) ?: return
        try {
            uploadProduct(entity)
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
    private suspend fun uploadProduct(entity: ProductEntity) {
        val dto = ProductDto.fromDomain(entity.toDomain())
        if (entity.isRemote) {
            productApiService.updateProduct(entity.id, dto)
            // Si falla la subida de la foto se lanza la excepción y el producto sigue pendiente
            uploadPendingImage(entity.id)
            productDao.insertProduct(entity.copy(pendingSync = false))
            Log.d(TAG, "Edición subida a la Web API: id=${entity.id}")
        } else {
            // Se manda el id local tal como hacía el código original (comportamiento ya probado
            // con el backend); la fila local se reemplaza por la que devuelve el servidor.
            val created = productApiService.createProduct(dto)
            val serverEntity = ProductEntity.fromDomain(created.toDomain())
                .copy(isRemote = true, pendingSync = false, isOwnedByUser = entity.isOwnedByUser)
            productDao.deleteProductById(entity.id)
            productDao.insertProduct(serverEntity)
            if (serverEntity.id != entity.id) {
                try {
                    mediaDao?.reassignMedia(entity.id, serverEntity.id)
                } catch (_: Exception) {
            // No se pudo limpiar la multimedia local: no es grave, se limpiará en la siguiente sincronización
        }
            }
            try {
                uploadPendingImage(serverEntity.id)
            } catch (e: Exception) {
                // El producto ya existe en el servidor, pero falta la foto: queda pendiente para reintentar
                productDao.insertProduct(serverEntity.copy(pendingSync = true))
                throw e
            }
            Log.d(TAG, "Producto subido a la Web API: id local=${entity.id} -> id servidor=${serverEntity.id}")
        }
    }

    /**
     * Sube al servidor la foto local del producto (si la hay) y la reemplaza por la URL del servidor,
     * para que todos los usuarios la vean. Lanza excepción si falla la red o el servidor.
     */
    private suspend fun uploadPendingImage(productId: Long) {
        val dao = mediaDao ?: return
        val store = imageStore ?: return
        val local = dao.getMediaByProductId(productId).first()
            .firstOrNull { !ImageStore.isRemote(it.uri) } ?: return

        val bytes = store.readBytes(local.uri)
        if (bytes == null) {
            // El archivo ya no existe: se descarta para no reintentar eternamente
            dao.deleteMedia(local)
            return
        }
        val part = MultipartBody.Part.createFormData(
            "image", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
        )
        val updated = productApiService.uploadImage(productId, part)
        dao.deleteMediaByProductId(productId)
        val remoteUrl = updated.imageUrl
        if (!remoteUrl.isNullOrBlank()) {
            dao.insertMedia(
                MediaEntity(productId = productId, uri = remoteUrl, mediaType = "IMAGE", description = local.description)
            )
        }
        Log.d(TAG, "Foto subida al servidor para producto id=$productId")
    }

    /** Guarda en local las URLs de fotos que vienen del servidor (salvo productos con cambios sin subir). */
    private suspend fun syncRemoteImages(remoteProducts: List<ProductDto>, skipIds: Set<Long>) {
        val dao = mediaDao ?: return
        try {
            val existing = dao.getAllMedia().first()
            for (dto in remoteProducts) {
                if (dto.id in skipIds) continue
                val url = dto.imageUrl
                val current = existing.filter { it.productId == dto.id }
                if (current.size == 1 && current[0].uri == url) continue
                if (current.isEmpty() && url.isNullOrBlank()) continue
                dao.deleteMediaByProductId(dto.id)
                if (!url.isNullOrBlank()) {
                    dao.insertMedia(MediaEntity(productId = dto.id, uri = url, mediaType = "IMAGE", description = dto.name))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron sincronizar las fotos: ${e.message}")
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
                uploadProduct(entity)
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
        } catch (_: Exception) {
            // No se pudo limpiar la multimedia local: no es grave, se limpiará en la siguiente sincronización
        }

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
        if (downloadProducts()) {
            return true
        }

        // 2. Si falló la conexión (ej. cambio de red Wi-Fi o IP), probar candidatos automáticamente
        if (customApiService == null) {
            for (candidate in RetrofitClient.CANDIDATE_URLS) {
                if (candidate != RetrofitClient.currentBaseUrl) {
                    RetrofitClient.updateBaseUrl(candidate)
                    if (downloadProducts()) {
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

    private suspend fun downloadProducts(): Boolean {
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
                syncRemoteImages(remoteProducts, pendingIds)
                Log.d(TAG, "Sincronización remota completada: ${entities.size} productos descargados desde ${RetrofitClient.currentBaseUrl}")
            }
            // El servidor respondió: aprovechar la conexión para subir lo que quedó pendiente offline
            pushPending()
            remoteProducts.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /** Si la base local está vacía, carga los productos de ejemplo (ver [SampleData]) para que el catálogo no se vea vacío. */
    override suspend fun populateInitialDataIfEmpty() {
        if (productDao.getProductCount() == 0) {
            productDao.insertProducts(SampleData.products)
        }
        addSamplePhotos()
    }

    /** Pone la foto de ejemplo a los productos de ejemplo que todavía no tienen una (también en instalaciones anteriores). */
    private suspend fun addSamplePhotos() {
        val media = mediaDao ?: return
        SampleData.productPhotos.forEach { (productId, uri) ->
            val isInCatalog = productDao.getProductById(productId) != null
            val hasPhoto = media.getMediaByProductId(productId).first().isNotEmpty()
            if (isInCatalog && !hasPhoto) {
                media.insertMedia(MediaEntity(productId = productId, uri = uri, mediaType = "IMAGE", description = "Foto de ejemplo"))
            }
        }
    }
}
