package com.example.zoco.ui.auth

import com.example.zoco.domain.model.toUiMessage
import com.example.zoco.domain.model.UiMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: UiMessage? = null
)

/**
 * ViewModel de la pantalla de login/registro. Antes de cada intento busca el servidor ([findServer])
 * para que funcione sin configurar nada. Si sale bien, la sesión queda guardada y [ZocoApp] cambia
 * sola a la pantalla principal; si falla, [AuthUiState.errorMessage] trae el motivo.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val findServer: (suspend () -> Boolean)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        launchAuth { authRepository.login(username, password) }
    }

    /** El registro público siempre crea compradores (el rol de vendedor se pide después desde Perfil). */
    fun register(username: String, email: String, password: String, fullName: String) {
        launchAuth { authRepository.register(username, email, password, fullName) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun launchAuth(call: suspend () -> Result<*>) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Si el servidor no responde en la URL actual, busca otra antes de intentar.
            findServer?.invoke()
            val result = call()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.let { it.toUiMessage(UiMessage.raw(it.message.orEmpty())) }
                )
            }
        }
    }
}
