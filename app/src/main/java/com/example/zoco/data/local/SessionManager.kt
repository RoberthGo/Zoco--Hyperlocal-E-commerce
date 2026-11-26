package com.example.zoco.data.local

import android.content.SharedPreferences
import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Guarda quién ha iniciado sesión (usuario, rol, token, foto) y lo recuerda aunque se cierre la app.
 *
 * Cómo se usa:
 *  - [sessionFlow] es la sesión actual; las pantallas la observan y se actualizan solas.
 *  - Si nadie ha iniciado sesión, vale [UserSession.LOGGED_OUT] y la app muestra el login.
 *  - [activeToken] es una copia del token en una variable estática: [RetrofitClient] la lee en cada
 *    petición para identificarse ante el servidor. Se actualiza en [saveSession] y [clearSession].
 *  - [usersFlow] es la lista de usuarios que solo ve el administrador (viene del servidor).
 */
class SessionManager(
    private val prefs: SharedPreferences? = null
) {
    private val _sessionFlow = MutableStateFlow(loadInitialSession())
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    private val _usersFlow = MutableStateFlow<List<ManagedUser>>(emptyList())
    val usersFlow: StateFlow<List<ManagedUser>> = _usersFlow.asStateFlow()

    init {
        activeToken = _sessionFlow.value.token
    }

    private fun loadInitialSession(): UserSession {
        val savedUsername = prefs?.getString(KEY_USERNAME, null)
        val savedToken = prefs?.getString(KEY_TOKEN, null)
        if (savedUsername.isNullOrBlank() || savedToken.isNullOrBlank()) {
            return UserSession.LOGGED_OUT
        }
        return UserSession(
            userId = prefs.getLong(KEY_USER_ID, 0L),
            username = savedUsername,
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            role = UserRole.fromString(prefs.getString(KEY_ROLE, UserRole.BUYER.name)),
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            address = prefs.getString(KEY_ADDRESS, "") ?: "",
            producerName = prefs.getString(KEY_PRODUCER_NAME, "") ?: "",
            token = savedToken,
            photoUri = prefs.getString(KEY_PHOTO_URI, null)
        )
    }

    fun clearSession() {
        activeToken = null
        _sessionFlow.value = UserSession.LOGGED_OUT
        _usersFlow.value = emptyList()
        prefs?.edit()?.apply {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_FULL_NAME)
            remove(KEY_EMAIL)
            remove(KEY_ROLE)
            remove(KEY_PHONE)
            remove(KEY_ADDRESS)
            remove(KEY_PRODUCER_NAME)
            remove(KEY_TOKEN)
            remove(KEY_PHOTO_URI)
            apply()
        }
    }

    fun saveSession(session: UserSession) {
        activeToken = session.token
        _sessionFlow.value = session
        prefs?.edit()?.apply {
            putLong(KEY_USER_ID, session.userId)
            putString(KEY_USERNAME, session.username)
            putString(KEY_FULL_NAME, session.fullName)
            putString(KEY_EMAIL, session.email)
            putString(KEY_ROLE, session.role.name)
            putString(KEY_PHONE, session.phone)
            putString(KEY_ADDRESS, session.address)
            putString(KEY_PRODUCER_NAME, session.producerName)
            if (session.token != null) putString(KEY_TOKEN, session.token) else remove(KEY_TOKEN)
            if (session.photoUri != null) putString(KEY_PHOTO_URI, session.photoUri) else remove(KEY_PHOTO_URI)
            apply()
        }
        upsertManagedUser(
            ManagedUser(
                id = session.userId,
                username = session.username,
                fullName = session.fullName,
                email = session.email,
                role = session.role,
                phone = session.phone
            )
        )
    }

    fun updatePhoto(photoUri: String?) {
        saveSession(_sessionFlow.value.copy(photoUri = photoUri))
    }

    fun updateName(fullName: String) {
        saveSession(_sessionFlow.value.copy(fullName = fullName))
    }

    fun setManagedUsers(users: List<ManagedUser>) {
        if (users.isNotEmpty()) {
            _usersFlow.value = users
        }
    }

    fun upsertManagedUser(user: ManagedUser) {
        _usersFlow.update { current ->
            val index = current.indexOfFirst { it.username == user.username || it.id == user.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = user }
            } else {
                current + user
            }
        }
    }

    fun updateUserRoleLocally(userId: Long, newRole: UserRole) {
        _usersFlow.update { current ->
            current.map { user ->
                if (user.id == userId) user.copy(role = newRole) else user
            }
        }
        val currentSession = _sessionFlow.value
        if (currentSession.userId == userId) {
            saveSession(currentSession.copy(role = newRole))
        }
    }

    companion object {
        @Volatile
        var activeToken: String? = null

        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
        private const val KEY_PHONE = "phone"
        private const val KEY_ADDRESS = "address"
        private const val KEY_PRODUCER_NAME = "producer_name"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_PHOTO_URI = "photo_uri"
    }
}
