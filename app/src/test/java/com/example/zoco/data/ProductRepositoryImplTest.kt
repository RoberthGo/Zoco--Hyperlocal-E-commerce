package com.example.zoco.data

import com.example.zoco.data.local.ProductDao
import com.example.zoco.data.local.ProductEntity
import com.example.zoco.data.remote.api.ProductApiService
import com.example.zoco.data.remote.dto.ProductDto
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ProductRepositoryImplTest {

    // ---------- Fakes en memoria ----------

    private class FakeProductDao : ProductDao {
        val rows = linkedMapOf<Long, ProductEntity>()
        private var nextId = 1L
        private val flow = MutableStateFlow<List<ProductEntity>>(emptyList())

        private fun publish() { flow.value = rows.values.toList() }

        override fun getAllProducts(): Flow<List<ProductEntity>> = flow
        override fun getLocalProducts(): Flow<List<ProductEntity>> = flow
        override suspend fun getProductById(id: Long): ProductEntity? = rows[id]
        override suspend fun getProductCount(): Int = rows.size
        override suspend fun insertProduct(product: ProductEntity): Long {
            val id = if (product.id == 0L) nextId++ else product.id
            if (id >= nextId) nextId = id + 1
            rows[id] = product.copy(id = id)
            publish()
            return id
        }
        override suspend fun insertProducts(products: List<ProductEntity>) { products.forEach { insertProduct(it) } }
        override suspend fun updateProduct(product: ProductEntity): Int {
            if (!rows.containsKey(product.id)) return 0
            rows[product.id] = product
            publish()
            return 1
        }
        override suspend fun deleteProduct(product: ProductEntity): Int = deleteProductById(product.id)
        override suspend fun deleteProductById(id: Long): Int {
            val removed = rows.remove(id) != null
            publish()
            return if (removed) 1 else 0
        }
        override suspend fun getPendingProducts(): List<ProductEntity> = rows.values.filter { it.pendingSync }
        override suspend fun deleteRemoteProducts(): Int {
            val ids = rows.values.filter { !it.pendingSync && it.isRemote }.map { it.id }
            ids.forEach { rows.remove(it) }
            publish()
            return ids.size
        }
    }

    private class FakeApi : ProductApiService {
        var failure: Exception? = null
        var nextServerId = 500L
        val created = mutableListOf<ProductDto>()
        val updated = mutableListOf<Pair<Long, ProductDto>>()
        val deleted = mutableListOf<Long>()

        private fun check() { failure?.let { throw it } }

        override suspend fun getProducts(): List<ProductDto> { check(); return emptyList() }
        override suspend fun getProductById(id: Long): ProductDto { check(); error("no usado") }
        override suspend fun createProduct(product: ProductDto): ProductDto {
            check()
            created += product
            return product.copy(id = nextServerId++)
        }
        override suspend fun updateProduct(id: Long, product: ProductDto): ProductDto {
            check()
            updated += id to product
            return product
        }
        override suspend fun deleteProduct(id: Long): Response<Unit> {
            check()
            deleted += id
            return Response.success(Unit)
        }
    }

    private class FakeScheduler : SyncScheduler {
        var calls = 0
        override fun scheduleSync() { calls++ }
    }

    private val dao = FakeProductDao()
    private val api = FakeApi()
    private val scheduler = FakeScheduler()
    private val repo = ProductRepositoryImpl(
        productDao = dao,
        customApiService = api,
        syncScheduler = scheduler
    )

    private fun newProduct() = Product(
        name = "Miel",
        description = "Miel pura",
        price = 90.0,
        category = ProductCategory.PRODUCE,
        producerName = "Apícola"
    )

    // ---------- Pruebas ----------

    @Test
    fun insertOffline_savesLocalAsPending_andSchedulesSync() = runTest {
        api.failure = IOException("sin red")

        val id = repo.insertProduct(newProduct())

        val row = dao.rows[id]
        assertNotNull(row)
        assertTrue(row!!.pendingSync)
        assertFalse(row.isRemote)
        assertEquals(1, scheduler.calls)
        assertTrue(api.created.isEmpty())
    }

    @Test
    fun insertOnline_replacesLocalRowWithServerRow() = runTest {
        val localId = repo.insertProduct(newProduct())

        assertNull(dao.rows[localId])                       // la fila local temporal desaparece
        val serverRow = dao.rows[500L]
        assertNotNull(serverRow)
        assertTrue(serverRow!!.isRemote)
        assertFalse(serverRow.pendingSync)
        assertEquals(0, scheduler.calls)
        assertEquals(0L, api.created.single().id)           // se crea sin mandar el id local
    }

    @Test
    fun pushPending_afterConnectionReturns_uploadsQueuedProduct() = runTest {
        api.failure = IOException("sin red")
        repo.insertProduct(newProduct())
        assertFalse(repo.pushPending())                     // sigue sin red -> queda pendiente
        assertEquals(1, dao.getPendingProducts().size)

        api.failure = null                                  // vuelve el internet
        assertTrue(repo.pushPending())

        assertTrue(dao.getPendingProducts().isEmpty())
        assertTrue(dao.rows.values.single().isRemote)
        assertEquals(1, api.created.size)
    }

    @Test
    fun editRemoteProductOffline_marksPending_thenPushesWithPut() = runTest {
        dao.insertProduct(
            ProductEntity(
                id = 7, name = "Pan", description = "d", price = 10.0, category = "BAKERY",
                producerName = "P", isExpressDelivery = false, inStock = true, isRemote = true
            )
        )
        api.failure = IOException("sin red")
        val edited = dao.rows[7L]!!.toDomain().copy(price = 25.0)

        assertTrue(repo.updateProduct(edited))
        assertTrue(dao.rows[7L]!!.pendingSync)
        assertEquals(25.0, dao.rows[7L]!!.price, 0.001)
        assertEquals(1, scheduler.calls)

        api.failure = null
        assertTrue(repo.pushPending())
        assertFalse(dao.rows[7L]!!.pendingSync)
        assertEquals(7L, api.updated.single().first)
        assertTrue(api.created.isEmpty())                   // era edición, no creación
    }

    @Test
    fun deleteRemoteProduct_withoutInternet_isRejectedAndKeepsRow() = runTest {
        dao.insertProduct(
            ProductEntity(
                id = 9, name = "Queso", description = "d", price = 10.0, category = "DAIRY",
                producerName = "P", isExpressDelivery = false, inStock = true, isRemote = true
            )
        )
        api.failure = IOException("sin red")

        assertFalse(repo.deleteProductById(9))
        assertNotNull(dao.rows[9L])
    }

    @Test
    fun deleteRemoteProduct_withInternet_removesLocalAndRemote() = runTest {
        dao.insertProduct(
            ProductEntity(
                id = 9, name = "Queso", description = "d", price = 10.0, category = "DAIRY",
                producerName = "P", isExpressDelivery = false, inStock = true, isRemote = true
            )
        )

        assertTrue(repo.deleteProductById(9))
        assertNull(dao.rows[9L])
        assertEquals(listOf(9L), api.deleted)
    }

    @Test
    fun deleteLocalOnlyProduct_withoutInternet_isAllowed() = runTest {
        api.failure = IOException("sin red")
        val id = repo.insertProduct(newProduct())          // pendiente, nunca subido

        assertTrue(repo.deleteProductById(id))
        assertNull(dao.rows[id])
    }
}
