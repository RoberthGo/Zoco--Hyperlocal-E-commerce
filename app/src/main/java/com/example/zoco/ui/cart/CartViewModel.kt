package com.example.zoco.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.CartItem
import com.example.zoco.domain.model.Order
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val scheduledDeliveryTime: String = "14:00",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val isExpress: Boolean = false,
    val orderCompletedMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isApiActive: Boolean = false,
    val recentOrders: List<Order> = emptyList()
) {
    val totalAmount: Double
        get() = items.sumOf { it.product.price * it.quantity }
}

/**
 * ViewModel de la pestaña "Carrito": guarda los productos que el usuario va a comprar, los datos de
 * entrega y la lista de pedidos recientes. Al confirmar, crea el pedido en la base local y lo
 * sube al servidor (si no hay conexión, queda pendiente y se sube después).
 */
class CartViewModel(
    private val orderRepository: OrderRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
    private val _isApiActive = MutableStateFlow(false)

    init {
        authRepository?.let { auth ->
            viewModelScope.launch {
                auth.sessionFlow.collect { session ->
                    _uiState.update { current ->
                        current.copy(
                            customerName = session.fullName.ifBlank { session.username },
                            customerPhone = session.phone.ifBlank { current.customerPhone },
                            deliveryAddress = session.address.ifBlank { current.deliveryAddress }
                        )
                    }
                }
            }
        }
        orderRepository?.let { repo ->
            viewModelScope.launch {
                repo.removeSampleOrders()
                val success = repo.syncOrdersFromRemote()
                _isApiActive.value = success
            }
            viewModelScope.launch {
                combine(repo.getOrders(), _isApiActive) { orders, isApiActive ->
                    // Si el servidor no responde, no se muestran los pedidos que vinieron de él (solo los locales)
                    if (isApiActive) {
                        orders
                    } else {
                        orders.filterNot { 
                            it.isRemote || it.customerName.startsWith("[En Línea]") || it.customerName.contains("(Web API)") 
                        }
                    }
                }.collect { visibleOrders ->
                    _uiState.update { it.copy(recentOrders = visibleOrders, isApiActive = _isApiActive.value) }
                }
            }
        }
    }

    fun syncOrdersFromWeb() {
        orderRepository?.let { repo ->
            viewModelScope.launch {
                val success = repo.syncOrdersFromRemote()
                _isApiActive.value = success
                _uiState.update { it.copy(isApiActive = success) }
            }
        }
    }

    fun addProduct(product: Product, quantityToAdd: Int = 1): Boolean {
        if (quantityToAdd <= 0) return false
        var success = true
        _uiState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.product.id == product.id }
            val currentQty = if (existingIndex >= 0) current.items[existingIndex].quantity else 0
            val targetQty = currentQty + quantityToAdd

            // Validación estricta: No permitir rebasar el stock disponible
            if (product.stock > 0 && targetQty > product.stock) {
                success = false
                return@update current
            }

            val updatedItems = if (existingIndex >= 0) {
                current.items.toMutableList().apply {
                    val existing = this[existingIndex]
                    this[existingIndex] = existing.copy(quantity = targetQty)
                }
            } else {
                current.items + CartItem(product = product, quantity = targetQty)
            }
            current.copy(items = updatedItems, orderCompletedMessage = null)
        }
        return success
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

    fun onCustomerNameChanged(name: String) = _uiState.update { it.copy(customerName = name) }
    fun onCustomerPhoneChanged(phone: String) = _uiState.update { it.copy(customerPhone = phone) }
    fun onDeliveryAddressChanged(address: String) = _uiState.update { it.copy(deliveryAddress = address) }
    fun onExpressChanged(express: Boolean) = _uiState.update { it.copy(isExpress = express) }

    fun confirmOrder() {
        val state = _uiState.value
        if (state.items.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val order = Order(
                customerName = state.customerName,
                customerPhone = state.customerPhone,
                deliveryAddress = state.deliveryAddress,
                scheduledTime = state.scheduledDeliveryTime,
                totalAmount = state.totalAmount,
                status = "PENDING",
                isExpress = state.isExpress
            )

            val orderId = orderRepository?.createOrder(order, state.items) ?: 0L

            _uiState.update {
                it.copy(
                    items = emptyList(),
                    isSubmitting = false,
                    orderCompletedMessage = "¡Pedido #${if (orderId > 0) orderId else "1"} registrado con éxito! Programado para las ${state.scheduledDeliveryTime}."
                )
            }
        }
    }

    fun clearConfirmationMessage() {
        _uiState.update { it.copy(orderCompletedMessage = null) }
    }
}
