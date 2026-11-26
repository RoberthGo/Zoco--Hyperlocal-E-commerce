package com.example.zoco.ui.admin

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
    val statusMessage: String? = null
)

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
            productRepository.syncProductsFromRemote()
            orderRepository.syncOrdersFromRemote()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    statusMessage = "Panel de administración actualizado."
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
                        "Rol actualizado a ${newRole.name}"
                    } else {
                        "No se pudo actualizar el rol."
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
                        "Pedido #$orderId actualizado a $newStatus"
                    } else {
                        "No se pudo actualizar el pedido #$orderId"
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
                        "Producto moderado/eliminado: ${product.name}"
                    } else {
                        "No se pudo eliminar \"${product.name}\". Verifica la conexión."
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
