package com.example.zoco.ui.profile

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoco.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ProfileUiState(
    val selectedLanguage: String = "es",
    val apiUrl: String = RetrofitClient.currentBaseUrl,
    val isCheckingConnection: Boolean = false,
    val connectionStatus: String? = "Listo para probar conexión con Django REST API",
    val isOnline: Boolean? = null
)

class ProfileViewModel(
    private val sharedPreferences: SharedPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            selectedLanguage = sharedPreferences?.getString(KEY_LANGUAGE, null)
                ?: if (Locale.getDefault().language == "en") "en" else "es"
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
