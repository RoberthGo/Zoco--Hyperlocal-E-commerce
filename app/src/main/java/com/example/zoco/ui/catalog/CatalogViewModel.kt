package com.example.zoco.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.data.remote.NetworkConnectivityChecker
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.model.UserSession
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Todo lo que la pantalla de catálogo necesita para dibujarse (la pantalla solo lee esto).
 * [enforceRolePermissions] indica si los permisos de borrado dependen del rol de la sesión
 * (cuando hay login) o solo de si el producto es del propio usuario (modo sin login, usado en pruebas).
 */
data class CatalogUiState(
    val searchQuery: String = "",
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val expressOnly: Boolean = false,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isApiActive: Boolean = false,
    val isInternetAvailable: Boolean = true,
    val syncMessage: String? = null,
    val currentSession: UserSession = UserSession.LOGGED_OUT,
    val enforceRolePermissions: Boolean = false
) {
    fun canDelete(product: Product): Boolean {
        return if (enforceRolePermissions) {
            product.canBeDeletedBy(currentSession)
        } else {
            product.isOwnedByUser
        }
    }

    fun isOwnProduct(product: Product): Boolean {
        return if (enforceRolePermissions) {
            product.isOwnedBy(currentSession)
        } else {
            product.isOwnedByUser
        }
    }
}

/** Lo que el usuario eligió en la barra de búsqueda y los filtros. */
private data class CatalogFilters(
    val query: String,
    val category: ProductCategory,
    val expressOnly: Boolean
)

/** Estado de la conexión con el servidor. */
private data class ServerStatus(
    val isSyncing: Boolean,
    val message: String?,
    val isActive: Boolean
)

/**
 * ViewModel de la pestaña "Catálogo". Junta tres cosas en un solo [uiState]:
 * los productos guardados en el teléfono, los filtros del usuario y el estado del servidor.
 * Cada vez que cambia una de ellas, el estado se recalcula y la pantalla se redibuja sola.
 */
class CatalogViewModel(
    private val repository: ProductRepository,
    private val networkChecker: NetworkConnectivityChecker? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    private val _expressOnly = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _isApiActive = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val filters = combine(_searchQuery, _selectedCategory, _expressOnly, ::CatalogFilters)
    private val serverStatus = combine(_isSyncing, _syncMessage, _isApiActive, ::ServerStatus)

    val uiState: StateFlow<CatalogUiState> = combine(
        repository.getProducts(),
        filters,
        serverStatus,
        networkChecker?.observeConnectivity() ?: flowOf(true),
        authRepository?.sessionFlow ?: flowOf(UserSession.LOGGED_OUT)
    ) { allProducts, filters, server, isInternetAvailable, session ->
        // Si el servidor no responde, no se muestran los productos que vinieron de él (solo los locales)
        val availableProducts = if (server.isActive) allProducts else allProducts.filterNot { isServerItem(it) }

        CatalogUiState(
            searchQuery = filters.query,
            selectedCategory = filters.category,
            expressOnly = filters.expressOnly,
            products = availableProducts.filter { matches(it, filters) },
            isLoading = false,
            isSyncing = server.isSyncing,
            isApiActive = server.isActive,
            isInternetAvailable = isInternetAvailable,
            syncMessage = server.message,
            currentSession = session,
            enforceRolePermissions = authRepository != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CatalogUiState(isLoading = true)
    )

    /** ¿El producto vino del servidor? (incluye datos viejos marcados con etiquetas de texto). */
    private fun isServerItem(product: Product): Boolean =
        product.isRemote || product.name.startsWith("[En Línea]") || product.producerName.contains("[Web API]")

    /** ¿El producto cumple la búsqueda, la categoría y el filtro de entrega express? */
    private fun matches(product: Product, filters: CatalogFilters): Boolean {
        val matchesQuery = filters.query.isBlank() ||
            product.name.contains(filters.query, ignoreCase = true) ||
            product.description.contains(filters.query, ignoreCase = true)
        val matchesCategory = filters.category == ProductCategory.ALL || product.category == filters.category
        val matchesExpress = !filters.expressOnly || product.isExpressDelivery
        return matchesQuery && matchesCategory && matchesExpress
    }

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
            val synced = repository.syncProductsFromRemote()
            _isApiActive.value = synced
            _syncMessage.value = if (synced) {
                null
            } else {
                "Sin conexión con el servidor. Mostrando los productos guardados en tu dispositivo."
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
            _syncMessage.value = "Actualizando catálogo..."
            val success = repository.syncProductsFromRemote()
            _isApiActive.value = success
            _isSyncing.value = false
            _syncMessage.value = if (success) {
                "Catálogo actualizado."
            } else {
                "No se pudo conectar con el servidor. Mostrando los productos guardados en tu dispositivo."
            }
        }
    }

    fun deleteProduct(product: Product) {
        val state = uiState.value
        if (!state.canDelete(product)) {
            _syncMessage.value = "Solo puedes eliminar los productos que tú publicaste."
            return
        }
        if (networkChecker != null && !networkChecker.isInternetAvailable()) {
            _syncMessage.value = "Sin conexión a internet. Conéctate para poder eliminar productos."
            return
        }
        viewModelScope.launch {
            val deleted = repository.deleteProduct(product)
            _syncMessage.value = if (deleted) {
                "Producto eliminado: ${product.name}"
            } else {
                "No se pudo eliminar \"${product.name}\". Revisa tu conexión o tus permisos."
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    private companion object {
        /** Cuánto tiempo (ms) se sigue calculando el estado después de que la pantalla deja de verlo. */
        const val STOP_TIMEOUT_MS = 5000L
    }
}
