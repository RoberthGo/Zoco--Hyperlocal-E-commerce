package com.example.zoco.ui.profile

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.domain.model.PredefinedUsers
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
    val apiUrl: String = RetrofitClient.currentBaseUrl,
    val isCheckingConnection: Boolean = false,
    val connectionStatus: String? = "Listo para probar conexión con Django REST API",
    val isOnline: Boolean? = null,
    val currentSession: UserSession = PredefinedUsers.BUYER.session,
    val isAuthLoading: Boolean = false,
    val authMessage: String? = null
)

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val authRepository: AuthRepository? = null
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

    fun quickSwitchRole(role: UserRole) {
        val repo = authRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authMessage = null) }
            val result = repo.quickSwitch(role)
            _uiState.update {
                val session = result.getOrNull()
                it.copy(
                    isAuthLoading = false,
                    authMessage = if (session != null) {
                        "Sesión activa: ${session.fullName} (${session.role.name})"
                    } else {
                        "No se pudo cambiar de cuenta."
                    }
                )
            }
        }
    }

    fun login(username: String, password: String) {
        val repo = authRepository ?: return
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authMessage = "Ingresa usuario y contraseña.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authMessage = null) }
            val result = repo.login(username, password)
            _uiState.update {
                if (result.isSuccess) {
                    val session = result.getOrThrow()
                    it.copy(
                        isAuthLoading = false,
                        authMessage = "Bienvenido ${session.fullName} (${session.role.name})"
                    )
                } else {
                    it.copy(
                        isAuthLoading = false,
                        authMessage = "Credenciales incorrectas o servidor no disponible."
                    )
                }
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        fullName: String,
        role: UserRole
    ) {
        val repo = authRepository ?: return
        if (username.isBlank() || password.length < 6) {
            _uiState.update { it.copy(authMessage = "Usuario requerido y contraseña de al menos 6 caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authMessage = null) }
            val result = repo.register(username, email, password, fullName, role)
            _uiState.update {
                val session = result.getOrNull()
                it.copy(
                    isAuthLoading = false,
                    authMessage = if (session != null) {
                        "Cuenta creada: ${session.fullName} (${session.role.name})"
                    } else {
                        "No se pudo registrar la cuenta."
                    }
                )
            }
        }
    }

    fun becomeSeller(producerName: String?) {
        val repo = authRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authMessage = null) }
            val result = repo.becomeSeller(producerName)
            _uiState.update {
                val session = result.getOrNull()
                it.copy(
                    isAuthLoading = false,
                    authMessage = if (session != null) {
                        "¡Ahora eres Vendedor! Pestaña Vender habilitada."
                    } else {
                        "No se pudo actualizar el rol."
                    }
                )
            }
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
        val currentName = _uiState.value.userName
        updateProfile(currentName, photoUri)
    }

    fun updateName(name: String) {
        val currentPhoto = _uiState.value.photoUri
        updateProfile(name, currentPhoto)
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
        try {
            Locale.setDefault(Locale.forLanguageTag(language))
        } catch (_: Exception) {}
        sharedPreferences?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
    }

    fun updateApiUrl(url: String) {
        _uiState.update { it.copy(apiUrl = url) }
        RetrofitClient.updateBaseUrl(url)
    }

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingConnection = true,
                    connectionStatus = "Probando conexión con Django REST API..."
                )
            }
            val (ok, msg) = RetrofitClient.pingBackend()
            _uiState.update {
                it.copy(
                    isCheckingConnection = false,
                    isOnline = ok,
                    connectionStatus = msg
                )
            }
        }
    }

    companion object {
        const val KEY_LANGUAGE = "selected_language"
    }
}
