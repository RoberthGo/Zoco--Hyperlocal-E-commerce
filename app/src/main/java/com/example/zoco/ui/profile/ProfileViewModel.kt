package com.example.zoco.ui.profile

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.data.remote.RetrofitClient
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
    val isOnline: Boolean? = null
)

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository? = null,
    private val sharedPreferences: SharedPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            selectedLanguage = sharedPreferences?.getString(KEY_LANGUAGE, null)
                ?: if (Locale.getDefault().language == "en") "en" else "es"
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        if (userProfileRepository != null) {
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

    fun updateProfile(name: String, photoUri: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(userName = name, photoUri = photoUri) }
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
