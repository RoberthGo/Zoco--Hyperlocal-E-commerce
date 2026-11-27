package com.example.zoco.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.NotificationItem
import com.example.zoco.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
) {
    /** Cuántas notificaciones no se han leído (es el número que se ve en la campana). */
    val unreadCount: Int get() = items.count { !it.isRead }
}

/** Lista de notificaciones de la persona con sesión. [refresh] las vuelve a pedir al servidor. */
class NotificationsViewModel(
    private val repository: NotificationRepository?
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun refresh() {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repo.fetch()
            _uiState.update { current ->
                current.copy(isLoading = false, items = result.getOrNull() ?: current.items)
            }
        }
    }

    fun markRead(id: Long) {
        val repo = repository ?: return
        // Se ve como leída al instante; si el servidor falla, se corrige al refrescar
        _uiState.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(isRead = true) else it }) }
        viewModelScope.launch { repo.markRead(id) }
    }

    fun markAllRead() {
        val repo = repository ?: return
        _uiState.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch { repo.markAllRead() }
    }

    /** Al cerrar sesión no debe quedar nada de la cuenta anterior. */
    fun clear() {
        _uiState.value = NotificationsUiState()
    }
}
