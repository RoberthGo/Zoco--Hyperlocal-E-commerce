package com.example.zoco.ui.cart

import androidx.lifecycle.ViewModel
import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val scheduledDeliveryTime: String = "14:00",
    val orderCompletedMessage: String? = null
) {
    val totalAmount: Double
        get() = items.sumOf { it.product.price * it.quantity }
}

class CartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun addProduct(product: Product) {
        _uiState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.product.id == product.id }
            val updatedItems = if (existingIndex >= 0) {
                current.items.toMutableList().apply {
                    val existing = this[existingIndex]
                    this[existingIndex] = existing.copy(quantity = existing.quantity + 1)
                }
            } else {
                current.items + CartItem(product = product, quantity = 1)
            }
            current.copy(items = updatedItems, orderCompletedMessage = null)
        }
    }

    fun removeProduct(product: Product) {
        _uiState.update { current ->
            val updatedItems = current.items.filterNot { it.product.id == product.id }
            current.copy(items = updatedItems)
        }
    }

    fun setScheduledTime(hour: Int, minute: Int) {
        val formatted = String.format("%02d:%02d", hour, minute)
        _uiState.update { it.copy(scheduledDeliveryTime = formatted) }
    }

    fun confirmOrder() {
        if (_uiState.value.items.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    items = emptyList(),
                    orderCompletedMessage = "¡Pedido confirmado para entrega a las ${it.scheduledDeliveryTime}!"
                )
            }
        }
    }

    fun clearConfirmationMessage() {
        _uiState.update { it.copy(orderCompletedMessage = null) }
    }
}
