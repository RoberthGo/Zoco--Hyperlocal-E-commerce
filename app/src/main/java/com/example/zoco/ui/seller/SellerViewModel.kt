package com.example.zoco.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.Order
import com.example.zoco.domain.model.PredefinedUsers
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.model.UserSession
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellerUiState(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val stock: String = "10",
    val category: ProductCategory = ProductCategory.BAKERY,
    val producerName: String = "",
    val isExpressDelivery: Boolean = false,
    val imageUri: String? = null,
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null,
    val myProducts: List<Product> = emptyList(),
    val receivedOrders: List<Order> = emptyList(),
    val currentSession: UserSession = PredefinedUsers.SELLER.session,
    val actionMessage: String? = null
)

class SellerViewModel(
    private val repository: ProductRepository,
    private val orderRepository: OrderRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerUiState())
    val uiState: StateFlow<SellerUiState> = _uiState.asStateFlow()

    init {
        if (authRepository != null) {
            viewModelScope.launch {
                combine(
                    authRepository.sessionFlow,
                    repository.getProducts()
                ) { session, allProducts ->
                    session to allProducts
                }.collect { (session, allProducts) ->
                    val own = allProducts.filter { it.isOwnedBy(session) }
                    _uiState.update { current ->
                        current.copy(
                            currentSession = session,
                            myProducts = own,
                            producerName = if (current.producerName.isBlank()) {
                                session.producerName.ifBlank { session.fullName }
                            } else {
                                current.producerName
                            }
                        )
                    }
                }
            }
        }
        if (orderRepository != null) {
            viewModelScope.launch {
                orderRepository.getOrders().collect { orders ->
                    _uiState.update { it.copy(receivedOrders = orders) }
                }
            }
        }
    }

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value, isSavedSuccess = false) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onPriceChanged(value: String) = _uiState.update { it.copy(price = value) }
    fun onStockChanged(value: String) = _uiState.update { it.copy(stock = value) }
    fun onCategoryChanged(category: ProductCategory) = _uiState.update { it.copy(category = category) }
    fun onProducerNameChanged(value: String) = _uiState.update { it.copy(producerName = value) }
    fun onExpressChanged(value: Boolean) = _uiState.update { it.copy(isExpressDelivery = value) }
    fun onImageUriChanged(uri: String?) = _uiState.update { it.copy(imageUri = uri) }

    fun saveProduct() {
        val state = _uiState.value
        val parsedPrice = state.price.toDoubleOrNull()
        val parsedStock = state.stock.toIntOrNull()

        if (state.name.isBlank() || state.producerName.isBlank() || parsedPrice == null || parsedPrice <= 0 || parsedStock == null || parsedStock <= 0) {
            _uiState.update {
                it.copy(errorMessage = "Por favor completa todos los campos requeridos correctamente (el precio y stock deben ser mayores a 0).")
            }
            return
        }

        val session = state.currentSession
        viewModelScope.launch {
            val newProduct = Product(
                name = state.name.trim(),
                description = state.description.trim(),
                price = parsedPrice,
                stock = parsedStock,
                category = state.category,
                producerName = state.producerName.trim(),
                isExpressDelivery = state.isExpressDelivery,
                inStock = parsedStock > 0,
                imageUrl = state.imageUri,
                isOwnedByUser = true,
                sellerId = if (authRepository != null) session.userId else null,
                sellerUsername = if (authRepository != null) session.username else null
            )
            repository.insertProductWithMedia(newProduct, state.imageUri)
            _uiState.update { current ->
                SellerUiState(
                    isSavedSuccess = true,
                    myProducts = current.myProducts,
                    receivedOrders = current.receivedOrders,
                    currentSession = current.currentSession,
                    producerName = if (authRepository != null) session.producerName.ifBlank { session.fullName } else ""
                )
            }
            kotlinx.coroutines.delay(2000L)
            _uiState.update {
                it.copy(isSavedSuccess = false)
            }
        }
    }

    fun deleteOwnProduct(product: Product) {
        val session = _uiState.value.currentSession
        if (!product.isOwnedBy(session)) {
            _uiState.update { it.copy(actionMessage = "Solo puedes eliminar tus propios productos.") }
            return
        }
        viewModelScope.launch {
            val deleted = repository.deleteProduct(product)
            _uiState.update {
                it.copy(
                    actionMessage = if (deleted) {
                        "Producto eliminado: ${product.name}"
                    } else {
                        "No se pudo eliminar \"${product.name}\". Verifica tu conexión."
                    }
                )
            }
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: String) {
        viewModelScope.launch {
            val updated = orderRepository?.updateOrderStatus(orderId, newStatus) ?: false
            _uiState.update {
                it.copy(
                    actionMessage = if (updated) {
                        "Pedido #$orderId actualizado a $newStatus"
                    } else {
                        "No se pudo actualizar el pedido #$orderId"
                    }
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
