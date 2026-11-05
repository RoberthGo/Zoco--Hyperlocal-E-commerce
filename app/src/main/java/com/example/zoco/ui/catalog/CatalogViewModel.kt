package com.example.zoco.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val searchQuery: String = "",
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val expressOnly: Boolean = false,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isApiActive: Boolean = false,
    val syncMessage: String? = null
)

class CatalogViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    private val _expressOnly = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _isApiActive = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.getProducts(),
        _searchQuery,
        _selectedCategory,
        _expressOnly,
        _isSyncing,
        _syncMessage,
        _isApiActive
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allProducts = args[0] as List<Product>
        val query = args[1] as String
        val category = args[2] as ProductCategory
        val expressOnly = args[3] as Boolean
        val isSyncing = args[4] as Boolean
        val syncMessage = args[5] as? String
        val isApiActive = args[6] as Boolean

        // REGLA CRÍTICA: Cuando la API no esté activa, asegurarse de que no se muestren las cosas de la API
        val availableProducts = if (isApiActive) {
            allProducts
        } else {
            allProducts.filterNot { 
                it.isRemote || it.name.startsWith("[En Línea]") || it.producerName.contains("[Web API]") 
            }
        }

        val filtered = availableProducts.filter { product ->
            val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) || product.description.contains(query, ignoreCase = true)
            val matchesCategory = category == ProductCategory.ALL || product.category == category
            val matchesExpress = !expressOnly || product.isExpressDelivery
            matchesQuery && matchesCategory && matchesExpress
        }

        CatalogUiState(
            searchQuery = query,
            selectedCategory = category,
            expressOnly = expressOnly,
            products = filtered,
            isLoading = false,
            isSyncing = isSyncing,
            isApiActive = isApiActive,
            syncMessage = syncMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CatalogUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            // Asegurar que el catálogo local offline esté poblado en Room
            repository.populateInitialDataIfEmpty()
            // Sincronizar catálogo desde la Web API de Django
            val synced = repository.syncProductsFromRemote()
            _isApiActive.value = synced
            _syncMessage.value = if (synced) {
                "Modo En Línea: Conectado con Web API."
            } else {
                "Modo Local Offline: Web API no activa. Mostrando catálogo local."
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: ProductCategory) {
        _selectedCategory.value = category
    }

    fun onExpressFilterToggled(enabled: Boolean) {
        _expressOnly.value = enabled
    }

    fun syncFromWeb() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Conectando con Django REST API..."
            val success = repository.syncProductsFromRemote()
            _isApiActive.value = success
            _isSyncing.value = false
            _syncMessage.value = if (success) {
                "Catálogo Web API sincronizado exitosamente."
            } else {
                "Web API no activa. Mostrando únicamente catálogo local offline."
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _syncMessage.value = "Producto eliminado de Room y de la Web API: ${product.name}"
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
