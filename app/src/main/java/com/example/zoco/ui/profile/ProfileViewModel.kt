package com.example.zoco.ui.profile

import com.example.zoco.domain.repository.OrderRepository
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ProfileUiState(
    val userName: String = "Comunidad Zoco",
    val userRole: String = "Consumidor y Productor Local",
    val photoUri: String? = null,
    val selectedLanguage: String = "es",
    val currentSession: UserSession = UserSession.LOGGED_OUT,
    val isAuthLoading: Boolean = false,
    val authMessage: String? = null,
    val passwordChanged: Boolean = false
)

/**
 * ViewModel de la pestaña "Perfil": datos de la cuenta (nombre, foto, rol), cambio de idioma,
 * convertirse en vendedor y cerrar sesión. La sesión misma la guarda [AuthRepository].
 */
class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val authRepository: AuthRepository? = null,
    private val orderRepository: OrderRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            selectedLanguage = sharedPreferences?.getString(KEY_LANGUAGE, null)
                ?: if (Locale.getDefault().language == "en") "en" else "es"
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        if (authRepository != null) {
            viewModelScope.launch {
                authRepository.sessionFlow.collect { session ->
                    _uiState.update { current ->
                        current.copy(
                            currentSession = session,
                            userName = session.fullName,
                            userRole = when (session.role) {
                                UserRole.BUYER -> "Comprador"
                                UserRole.SELLER -> "Vendedor"
                                UserRole.ADMIN -> "Administrador"
                            },
                            photoUri = session.photoUri ?: current.photoUri
                        )
                    }
                }
            }
        } else if (userProfileRepository != null) {
            viewModelScope.launch {
                userProfileRepository.getUserProfile().collect { profile ->
                    _uiState.update { current ->
                        current.copy(
                            userName = profile.name,
                            userRole = profile.role,
                            photoUri = profile.photoUri
                        )
                    }
                }
            }
            viewModelScope.launch {
                userProfileRepository.syncProfileFromRemote()
            }
        }
    }

    fun becomeSeller(producerName: String?) {
        val repo = authRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authMessage = null) }
            val result = repo.becomeSeller(producerName)
            _uiState.update {
                it.copy(
                    isAuthLoading = false,
                    authMessage = if (result.isSuccess) {
                        "¡Ahora eres Vendedor! Pestaña Vender habilitada."
                    } else {
                        result.exceptionOrNull()?.message ?: "No se pudo actualizar el rol."
                    }
                )
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val repo = authRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authMessage = null) }
            val result = repo.changePassword(currentPassword, newPassword)
            _uiState.update {
                it.copy(
                    isAuthLoading = false,
                    passwordChanged = result.isSuccess,
                    authMessage = if (result.isSuccess) "Contraseña actualizada." else result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearPasswordChanged() {
        _uiState.update { it.copy(passwordChanged = false) }
    }

    fun logout() {
        val repo = authRepository ?: return
        viewModelScope.launch {
            // Antes de salir, intenta subir lo pendiente y borra los pedidos locales de esta cuenta
            orderRepository?.let {
                it.pushPending()
                it.clearLocalOrders()
            }
            repo.logout()
            _uiState.update { it.copy(authMessage = null) }
        }
    }

    fun clearAuthMessage() {
        _uiState.update { it.copy(authMessage = null) }
    }

    fun updateProfile(name: String, photoUri: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(userName = name, photoUri = photoUri) }
            authRepository?.updateLocalProfile(fullName = name, photoUri = photoUri)
            userProfileRepository?.updateProfile(name, photoUri)
        }
    }

    fun updatePhoto(photoUri: String?) {
        val repo = authRepository
        if (repo == null || photoUri == null) {
            updateProfile(_uiState.value.userName, photoUri)
            return
        }
        viewModelScope.launch {
            val result = repo.updateAvatar(photoUri)
            _uiState.update {
                it.copy(
                    authMessage = if (result.isSuccess) null
                    else "La foto se guardó en tu dispositivo, pero no se pudo subir. Inténtalo de nuevo con conexión."
                )
            }
        }
    }

    fun updateName(name: String) {
        val currentPhoto = _uiState.value.photoUri
        updateProfile(name, currentPhoto)
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
        try {
            Locale.setDefault(Locale.forLanguageTag(language))
        } catch (_: Exception) {
            // Idioma no válido: se conserva el actual
        }
        sharedPreferences?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
    }

    companion object {
        const val KEY_LANGUAGE = "selected_language"
    }
}
