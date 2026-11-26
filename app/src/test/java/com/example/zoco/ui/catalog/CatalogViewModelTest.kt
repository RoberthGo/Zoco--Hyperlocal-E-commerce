package com.example.zoco.ui.catalog

import com.example.zoco.data.remote.NetworkConnectivityChecker
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeNetworkChecker(var online: Boolean) : NetworkConnectivityChecker {
        private val flow = MutableStateFlow(online)
        override fun isInternetAvailable(): Boolean = online
        override fun observeConnectivity(): Flow<Boolean> = flow
    }

    private class FakeProductRepository : ProductRepository {
        val deletedIds = mutableListOf<Long>()
        private val productsFlow = MutableStateFlow<List<Product>>(emptyList())

        override fun getProducts(): Flow<List<Product>> = productsFlow
        override fun getProductsFiltered(query: String, category: ProductCategory, expressOnly: Boolean): Flow<List<Product>> = flowOf(emptyList())
        override suspend fun getProductById(id: Long): Product? = null
        override suspend fun insertProduct(product: Product): Long = 1L
        override suspend fun insertProductWithMedia(product: Product, mediaUri: String?): Long = 1L
        override suspend fun getMediaForProduct(productId: Long): List<String> = emptyList()
        override suspend fun updateProduct(product: Product): Boolean = true
        override suspend fun deleteProduct(product: Product): Boolean = deleteProductById(product.id)
        override suspend fun deleteProductById(id: Long): Boolean {
            deletedIds.add(id)
            return true
        }
        override suspend fun syncProductsFromRemote(): Boolean = true
        override suspend fun pushPending(): Boolean = true
        override suspend fun populateInitialDataIfEmpty() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deleteProduct_blockedWhenNotOwnedByUser() = runTest {
        val repo = FakeProductRepository()
        val network = FakeNetworkChecker(online = true)
        val viewModel = CatalogViewModel(repo, network)
        testDispatcher.scheduler.advanceUntilIdle()

        val otherProduct = Product(
            id = 101L,
            name = "Pan de Otro Vendedor",
            price = 40.0,
            producerName = "Panadería Externa",
            isOwnedByUser = false
        )

        viewModel.deleteProduct(otherProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("No debe eliminar productos que no publicó el usuario", repo.deletedIds.isEmpty())
    }

    @Test
    fun deleteProduct_blockedWhenNoInternet() = runTest {
        val repo = FakeProductRepository()
        val network = FakeNetworkChecker(online = false)
        val viewModel = CatalogViewModel(repo, network)
        testDispatcher.scheduler.advanceUntilIdle()

        val userProduct = Product(
            id = 201L,
            name = "Mi Producto Local",
            price = 50.0,
            producerName = "Mi Negocio",
            isOwnedByUser = true
        )

        viewModel.deleteProduct(userProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("No debe eliminar productos sin conexión a internet", repo.deletedIds.isEmpty())
    }

    @Test
    fun deleteProduct_succeedsWhenOwnedByUserAndOnline() = runTest {
        val repo = FakeProductRepository()
        val network = FakeNetworkChecker(online = true)
        val viewModel = CatalogViewModel(repo, network)
        testDispatcher.scheduler.advanceUntilIdle()

        val userProduct = Product(
            id = 202L,
            name = "Mi Producto Publicado",
            price = 65.0,
            producerName = "Mi Negocio",
            isOwnedByUser = true
        )

        viewModel.deleteProduct(userProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(202L), repo.deletedIds)
    }
}
