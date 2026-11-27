package com.example.zoco.ui.admin

import com.example.zoco.ui.common.orderStatusMessage
import com.example.zoco.ui.common.roleMessage
import com.example.zoco.R
import com.example.zoco.domain.model.UiMessage
import com.example.zoco.ui.common.roleText
import com.example.zoco.ui.common.orderStatusText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.Order
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val users: List<ManagedUser> = emptyList(),
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val isRefreshing: Boolean = false,
    val statusMessage: UiMessage? = null
)

/**
 * ViewModel de la pestaña "Admin" (solo administradores): lista de usuarios, pedidos y productos
 * de toda la plataforma, con acciones de moderación (cambiar roles, estados y borrar productos).
 */
class AdminViewModel(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                authRepository.usersFlow,
                orderRepository.getOrders(),
                productRepository.getProducts()
            ) { users, orders, products ->
                Triple(users, orders, products)
            }.collect { (users, orders, products) ->
                _uiState.update {
                    it.copy(
                        users = users,
                        orders = orders,
                        products = products
                    )
                }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            authRepository.refreshUsers()
            productRepository.refreshProducts()
            orderRepository.refreshOrders()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    statusMessage = UiMessage.of(R.string.msg_admin_refreshed)
                )
            }
        }
    }

    fun changeUserRole(userId: Long, newRole: UserRole) {
        viewModelScope.launch {
            val ok = authRepository.updateUserRole(userId, newRole)
            _uiState.update {
                it.copy(
                    statusMessage = if (ok) {
                        UiMessage.of(R.string.msg_role_updated, roleMessage(newRole))
                    } else {
                        UiMessage.of(R.string.msg_role_update_failed)
                    }
                )
            }
        }
    }

    fun changeOrderStatus(orderId: Long, newStatus: String) {
        viewModelScope.launch {
            val ok = orderRepository.updateOrderStatus(orderId, newStatus)
            _uiState.update {
                it.copy(
                    statusMessage = if (ok) {
                        UiMessage.of(R.string.msg_order_updated, orderId, orderStatusMessage(newStatus))
                    } else {
                        UiMessage.of(R.string.msg_order_update_failed, orderId)
                    }
                )
            }
        }
    }

    fun deleteProductByModeration(product: Product) {
        viewModelScope.launch {
            val ok = productRepository.deleteProduct(product)
            _uiState.update {
                it.copy(
                    statusMessage = if (ok) {
                        UiMessage.of(R.string.msg_product_deleted, product.name)
                    } else {
                        UiMessage.of(R.string.msg_product_delete_failed_connection, product.name)
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
