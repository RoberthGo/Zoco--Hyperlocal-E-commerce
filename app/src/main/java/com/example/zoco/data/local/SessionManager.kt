package com.example.zoco.data.local

import android.content.SharedPreferences
import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.PredefinedUsers
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionManager(
    private val prefs: SharedPreferences? = null
) {
    private val _sessionFlow = MutableStateFlow(loadInitialSession())
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    private val _usersFlow = MutableStateFlow(PredefinedUsers.ALL_MANAGED_USERS)
    val usersFlow: StateFlow<List<ManagedUser>> = _usersFlow.asStateFlow()

    init {
        activeToken = _sessionFlow.value.token
    }

    private fun loadInitialSession(): UserSession {
        val savedUsername = prefs?.getString(KEY_USERNAME, null)
        if (savedUsername.isNullOrBlank()) {
            return PredefinedUsers.BUYER.session
        }
        return UserSession(
            userId = prefs.getLong(KEY_USER_ID, PredefinedUsers.BUYER.session.userId),
            username = savedUsername,
            fullName = prefs.getString(KEY_FULL_NAME, PredefinedUsers.BUYER.session.fullName)
                ?: PredefinedUsers.BUYER.session.fullName,
            email = prefs.getString(KEY_EMAIL, PredefinedUsers.BUYER.session.email)
                ?: PredefinedUsers.BUYER.session.email,
            role = UserRole.fromString(prefs.getString(KEY_ROLE, UserRole.BUYER.name)),
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            address = prefs.getString(KEY_ADDRESS, "") ?: "",
            producerName = prefs.getString(KEY_PRODUCER_NAME, "") ?: "",
            token = prefs.getString(KEY_TOKEN, null),
            photoUri = prefs.getString(KEY_PHOTO_URI, null)
        )
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
