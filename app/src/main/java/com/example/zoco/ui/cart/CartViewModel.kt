package com.example.zoco.ui.cart

import com.example.zoco.R
import com.example.zoco.domain.model.UiMessage
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
    val orderCompletedMessage: UiMessage? = null,
    val isSubmitting: Boolean = false,
    val isServerReachable: Boolean = false,
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
    private val _isServerReachable = MutableStateFlow(false)

    init {
        authRepository?.let { auth ->
            viewModelScope.launch {
                var lastUserId: Long? = null
                auth.sessionFlow.collect { session ->
                    // Otra cuenta (o cerrar sesión) = carrito nuevo, sin productos ni mensajes de la cuenta anterior
                    val accountChanged = lastUserId != null && lastUserId != session.userId
                    lastUserId = session.userId
                    _uiState.update { base ->
                        val current = if (accountChanged) CartUiState() else base
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
                val success = repo.refreshOrders()
                _isServerReachable.value = success
            }
            viewModelScope.launch {
                combine(repo.getOrders(), _isServerReachable) { orders, isServerReachable ->
                    // Si el servidor no responde, no se muestran los pedidos que vinieron de él (solo los locales)
                    if (isServerReachable) {
                        orders
                    } else {
                        orders.filterNot { it.isRemote }
                    }
                }.collect { visibleOrders ->
                    _uiState.update { it.copy(recentOrders = visibleOrders, isServerReachable = _isServerReachable.value) }
                }
            }
        }
    }

    fun refreshOrdersFromServer() {
        orderRepository?.let { repo ->
            viewModelScope.launch {
                val success = repo.refreshOrders()
                _isServerReachable.value = success
                _uiState.update { it.copy(isServerReachable = success) }
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
                    orderCompletedMessage = UiMessage.of(R.string.msg_order_placed, if (orderId > 0) orderId else 1L, state.scheduledDeliveryTime)
                )
            }
        }
    }

    fun clearConfirmationMessage() {
        _uiState.update { it.copy(orderCompletedMessage = null) }
    }
}
