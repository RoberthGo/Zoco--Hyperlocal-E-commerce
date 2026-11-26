package com.example.zoco.data.repository

import android.util.Log
import com.example.zoco.data.local.SessionManager
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.api.AuthApiService
import com.example.zoco.data.remote.dto.BecomeSellerRequestDto
import com.example.zoco.data.remote.dto.LoginRequestDto
import com.example.zoco.data.remote.dto.RegisterRequestDto
import com.example.zoco.data.remote.dto.RoleUpdateRequestDto
import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.PredefinedUsers
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import com.example.zoco.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class AuthRepositoryImpl(
    private val sessionManager: SessionManager,
    private val customAuthApiService: AuthApiService? = null
) : AuthRepository {

    private val authApiService: AuthApiService
        get() = customAuthApiService ?: RetrofitClient.authApiService

    override val sessionFlow: StateFlow<UserSession> = sessionManager.sessionFlow
    override val usersFlow: StateFlow<List<ManagedUser>> = sessionManager.usersFlow

    override suspend fun quickSwitch(role: UserRole): Result<UserSession> {
        val preset = PredefinedUsers.forRole(role)
        return try {
            val response = authApiService.login(
                LoginRequestDto(
                    username = preset.session.username,
                    password = preset.password
                )
            )
            val session = response.user.toSession(
                token = response.token,
                producerNameOverride = preset.session.producerName
            )
            sessionManager.saveSession(session)
            refreshUsers()
            Result.success(session)
        } catch (e: Exception) {
            Log.w(TAG, "API no disponible para cambio rápido; usando cuenta local precargada: ${e.message}")
            sessionManager.saveSession(preset.session)
            Result.success(preset.session)
        }
    }

    override suspend fun login(username: String, password: String): Result<UserSession> {
        val trimmedUser = username.trim()
        return try {
            val response = authApiService.login(LoginRequestDto(trimmedUser, password))
            val presetMatch = listOf(
                PredefinedUsers.BUYER,
                PredefinedUsers.SELLER,
                PredefinedUsers.SECOND_SELLER,
                PredefinedUsers.ADMIN
            ).firstOrNull { it.session.username == trimmedUser }

            val session = response.user.toSession(
                token = response.token,
                producerNameOverride = presetMatch?.session?.producerName ?: ""
            )
            sessionManager.saveSession(session)
            if (session.role == UserRole.ADMIN) {
                refreshUsers()
            }
            Result.success(session)
        } catch (e: Exception) {
            // Respaldo offline si coincide con alguno de los 3 usuarios precargados
            val presetMatch = listOf(
                PredefinedUsers.BUYER,
                PredefinedUsers.SELLER,
                PredefinedUsers.SECOND_SELLER,
                PredefinedUsers.ADMIN
            ).firstOrNull { it.session.username == trimmedUser && it.password == password }

            if (presetMatch != null) {
                sessionManager.saveSession(presetMatch.session)
                Result.success(presetMatch.session)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String,
        role: UserRole
    ): Result<UserSession> {
        val safeRole = if (role == UserRole.ADMIN) UserRole.BUYER else role
        val parts = fullName.trim().split(" ", limit = 2)
        val firstName = parts.getOrNull(0) ?: ""
        val lastName = parts.getOrNull(1) ?: ""

        return try {
            val response = authApiService.register(
                RegisterRequestDto(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    role = safeRole.name
                )
            )
            val session = response.user.toSession(
                token = response.token,
                producerNameOverride = fullName.trim()
            )
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.w(TAG, "Registro sin API (${e.message}); creando sesión local")
            val localSession = UserSession(
                userId = System.currentTimeMillis() % 100000L + 100L,
                username = username.trim(),
                fullName = fullName.trim().ifBlank { username.trim() },
                email = email.trim(),
                role = safeRole,
                producerName = if (safeRole == UserRole.SELLER) fullName.trim() else ""
            )
            sessionManager.saveSession(localSession)
            Result.success(localSession)
        }
    }

    override suspend fun becomeSeller(producerName: String?): Result<UserSession> {
        val current = sessionManager.sessionFlow.value
        val businessName = producerName?.trim()?.takeIf { it.isNotBlank() }
            ?: current.producerName.ifBlank { current.fullName }

        return try {
            if (!current.token.isNullOrBlank()) {
                val response = authApiService.becomeSeller(BecomeSellerRequestDto(producerName = businessName))
                val updated = response.user.toSession(
                    token = current.token,
                    producerNameOverride = businessName
                ).copy(photoUri = current.photoUri)
                sessionManager.saveSession(updated)
                Result.success(updated)
            } else {
                val updated = current.copy(role = UserRole.SELLER, producerName = businessName)
                sessionManager.saveSession(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar becomeSeller con la API; aplicando cambio local: ${e.message}")
            val updated = current.copy(role = UserRole.SELLER, producerName = businessName)
            sessionManager.saveSession(updated)
            Result.success(updated)
        }
    }

    override suspend fun refreshUsers(): Boolean {
        return try {
            val remoteUsers = authApiService.getUsers().map { it.toManagedUser() }
            sessionManager.setManagedUsers(remoteUsers)
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener lista de usuarios de la API: ${e.message}")
            false
        }
    }

    override suspend fun updateUserRole(userId: Long, newRole: UserRole): Boolean {
        sessionManager.updateUserRoleLocally(userId, newRole)
        return try {
            val updatedDto = authApiService.updateUserRole(userId, RoleUpdateRequestDto(role = newRole.name))
            sessionManager.upsertManagedUser(updatedDto.toManagedUser())
            true
        } catch (e: Exception) {
            Log.w(TAG, "Cambio de rol aplicado localmente (API offline/restringida): ${e.message}")
            true
        }
    }

    override fun updateLocalProfile(fullName: String?, photoUri: String?) {
        val current = sessionManager.sessionFlow.value
        sessionManager.saveSession(
            current.copy(
                fullName = fullName?.takeIf { it.isNotBlank() } ?: current.fullName,
                photoUri = photoUri ?: current.photoUri
            )
        )
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
