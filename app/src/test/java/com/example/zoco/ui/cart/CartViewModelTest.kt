package com.example.zoco.ui.cart

import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartViewModelTest {

    private val sampleProduct = Product(
        id = 1,
        name = "Pan Artesanal",
        description = "Horneado con masa madre",
        price = 35.0,
        category = ProductCategory.BAKERY,
        producerName = "Panadería Don Juan",
        isExpressDelivery = true
    )

    @Test
    fun addProduct_incrementsQuantityAndCalculatesTotal() {
        val viewModel = CartViewModel()

        viewModel.addProduct(sampleProduct)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(35.0, viewModel.uiState.value.totalAmount, 0.001)

        viewModel.addProduct(sampleProduct)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(2, viewModel.uiState.value.items.first().quantity)
        assertEquals(70.0, viewModel.uiState.value.totalAmount, 0.001)
    }

    @Test
    fun removeProduct_clearsCart() {
        val viewModel = CartViewModel()
        viewModel.addProduct(sampleProduct)
        assertEquals(1, viewModel.uiState.value.items.size)

        viewModel.removeProduct(sampleProduct)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(0.0, viewModel.uiState.value.totalAmount, 0.001)
    }

    @Test
    fun setScheduledTime_formatsProperly() {
        val viewModel = CartViewModel()
        viewModel.setScheduledTime(9, 5)
        assertEquals("09:05", viewModel.uiState.value.scheduledDeliveryTime)

        viewModel.setScheduledTime(18, 30)
        assertEquals("18:30", viewModel.uiState.value.scheduledDeliveryTime)
    }

    @Test
    fun customerFields_updateState() {
        val viewModel = CartViewModel()
        viewModel.onCustomerNameChanged("Carlos Pérez")
        viewModel.onDeliveryAddressChanged("Av. Hidalgo #100")
        viewModel.onExpressChanged(true)

        val state = viewModel.uiState.value
        assertEquals("Carlos Pérez", state.customerName)
        assertEquals("Av. Hidalgo #100", state.deliveryAddress)
        assertTrue(state.isExpress)
    }
}
