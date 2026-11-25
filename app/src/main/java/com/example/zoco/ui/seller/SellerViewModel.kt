package com.example.zoco.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val errorMessage: String? = null
)

class SellerViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerUiState())
    val uiState: StateFlow<SellerUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Por favor completa todos los campos requeridos correctamente (el precio y stock deben ser mayores a 0).") }
            return
        }

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
                imageUrl = state.imageUri
            )
            repository.insertProductWithMedia(newProduct, state.imageUri)
            _uiState.update {
                SellerUiState(isSavedSuccess = true)
            }
            kotlinx.coroutines.delay(2000L)
            _uiState.update {
                it.copy(isSavedSuccess = false)
            }
        }
    }
}
