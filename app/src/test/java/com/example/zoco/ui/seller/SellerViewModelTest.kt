package com.example.zoco.ui.seller

import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SellerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeProductRepository : ProductRepository {
        val insertedProducts = mutableListOf<Product>()
        val insertedMedia = mutableMapOf<Long, String?>()

        override fun getProducts(): Flow<List<Product>> = flowOf(insertedProducts)
        override fun getProductsFiltered(query: String, category: ProductCategory, expressOnly: Boolean): Flow<List<Product>> = flowOf(insertedProducts)
        override suspend fun getProductById(id: Long): Product? = insertedProducts.firstOrNull { it.id == id }
        override suspend fun insertProduct(product: Product): Long {
            insertedProducts.add(product)
            return insertedProducts.size.toLong()
        }
        override suspend fun insertProductWithMedia(product: Product, mediaUri: String?): Long {
            val id = insertProduct(product)
            insertedMedia[id] = mediaUri
            return id
        }
        override suspend fun getMediaForProduct(productId: Long): List<String> = insertedMedia[productId]?.let { listOf(it) } ?: emptyList()
        override suspend fun updateProduct(product: Product): Boolean = true
        override suspend fun deleteProduct(product: Product): Boolean = true
        override suspend fun deleteProductById(id: Long): Boolean = true
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
    fun validation_failsWhenFieldsAreEmpty() {
        val fakeRepo = FakeProductRepository()
        val viewModel = SellerViewModel(fakeRepo)

        viewModel.saveProduct()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(fakeRepo.insertedProducts.isEmpty())
    }

    @Test
    fun saveProduct_savesWithMediaSuccessfully() = runTest {
        val fakeRepo = FakeProductRepository()
        val viewModel = SellerViewModel(fakeRepo)

        viewModel.onNameChanged("Miel Orgánica")
        viewModel.onProducerNameChanged("Apícola Valle")
        viewModel.onPriceChanged("120.50")
        viewModel.onCategoryChanged(ProductCategory.PRODUCE)
        viewModel.onDescriptionChanged("Miel 100% pura")
        viewModel.onExpressChanged(true)
        viewModel.onImageUriChanged("content://media/external/images/media/42")

        viewModel.saveProduct()
        testDispatcher.scheduler.advanceTimeBy(500)

        assertTrue(viewModel.uiState.value.isSavedSuccess)
        assertEquals(1, fakeRepo.insertedProducts.size)
        val saved = fakeRepo.insertedProducts.first()
        assertEquals("Miel Orgánica", saved.name)
        assertEquals("Apícola Valle", saved.producerName)
        assertEquals(120.50, saved.price, 0.001)
        assertEquals(ProductCategory.PRODUCE, saved.category)
        assertTrue(saved.isExpressDelivery)
        assertTrue(saved.isOwnedByUser)
        assertEquals("content://media/external/images/media/42", saved.imageUrl)

        // Luego de 2 segundos el mensaje de éxito se limpia automáticamente
        testDispatcher.scheduler.advanceTimeBy(2000)
        assertEquals(false, viewModel.uiState.value.isSavedSuccess)
    }
}
