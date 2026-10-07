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
import kotlinx.coroutines.launch

data class CatalogUiState(
    val searchQuery: String = "",
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val expressOnly: Boolean = false,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false
)

class CatalogViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    private val _expressOnly = MutableStateFlow(false)

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.getProducts(),
        _searchQuery,
        _selectedCategory,
        _expressOnly
    ) { products, query, category, expressOnly ->
        val filtered = products.filter { product ->
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
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CatalogUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
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
}
