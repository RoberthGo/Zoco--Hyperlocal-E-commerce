package com.example.zoco.data.repository

import androidx.annotation.StringRes
import com.example.zoco.domain.model.UserFacingException
import com.example.zoco.domain.model.UiMessage
import com.example.zoco.R
import com.example.zoco.data.remote.dto.ChangePasswordRequestDto
import android.util.Log
import com.example.zoco.data.local.ImageStore
import com.example.zoco.data.local.SessionManager
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.api.AuthApiService
import com.example.zoco.data.remote.dto.BecomeSellerRequestDto
import com.example.zoco.data.remote.dto.LoginRequestDto
import com.example.zoco.data.remote.dto.RegisterRequestDto
import com.example.zoco.data.remote.dto.RoleUpdateRequestDto
import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import com.example.zoco.domain.repository.AuthRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementación de [AuthRepository]. Ya no hay cuentas de prueba ni modo sin servidor: login y
 * registro dependen del servidor. Si algo falla, devuelve un mensaje entendible para el usuario
 * (por ejemplo "No se pudo conectar con el servidor.") en vez del error técnico.
 */
class AuthRepositoryImpl(
    private val sessionManager: SessionManager,
    private val customAuthApiService: AuthApiService? = null,
    private val imageStore: ImageStore? = null
) : AuthRepository {

    private val authApiService: AuthApiService
        get() = customAuthApiService ?: RetrofitClient.authApiService

    override val sessionFlow: StateFlow<UserSession> = sessionManager.sessionFlow
    override val usersFlow: StateFlow<List<ManagedUser>> = sessionManager.usersFlow

    override suspend fun login(username: String, password: String): Result<UserSession> {
        return try {
            val response = authApiService.login(LoginRequestDto(username.trim(), password))
            val session = response.user.toSession(token = response.token)
            sessionManager.saveSession(session)
            if (session.role == UserRole.ADMIN) {
                refreshUsers()
            }
            Result.success(session)
        } catch (e: Exception) {
            Log.w(TAG, "Login fallido: ${e.message}")
            Result.failure(friendlyError(e, R.string.err_bad_credentials))
        }
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String
    ): Result<UserSession> {
        val parts = fullName.trim().split(" ", limit = 2)
        return try {
            val response = authApiService.register(
                RegisterRequestDto(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    firstName = parts.getOrNull(0) ?: "",
                    lastName = parts.getOrNull(1) ?: "",
                    // El registro público solo crea compradores; el servidor también lo exige.
                    role = UserRole.BUYER.name
                )
            )
            val session = response.user.toSession(token = response.token)
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.w(TAG, "Registro fallido: ${e.message}")
            Result.failure(friendlyError(e, R.string.err_register_failed))
        }
    }

    override suspend fun becomeSeller(producerName: String?): Result<UserSession> {
        val current = sessionManager.sessionFlow.value
        if (!current.isLoggedIn) {
            return Result.failure(UserFacingException(UiMessage.of(R.string.err_login_required)))
        }
        if (current.role != UserRole.BUYER) {
            return Result.failure(UserFacingException(UiMessage.of(R.string.err_buyers_only)))
        }
        val businessName = producerName?.trim()?.takeIf { it.isNotBlank() }
            ?: current.producerName.ifBlank { current.fullName }

        return try {
            val response = authApiService.becomeSeller(BecomeSellerRequestDto(producerName = businessName))
            val updated = response.user.toSession(
                token = current.token,
                producerNameOverride = businessName
            ).copy(photoUri = current.photoUri)
            sessionManager.saveSession(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.w(TAG, "becomeSeller fallido: ${e.message}")
            Result.failure(friendlyError(e, R.string.msg_role_update_failed))
        }
    }

    override suspend fun updateAvatar(photoUri: String): Result<UserSession> {
        val current = sessionManager.sessionFlow.value
        if (!current.isLoggedIn) return Result.failure(UserFacingException(UiMessage.of(R.string.err_login_required)))

        // 1. Mostrar la foto de inmediato con una copia local reducida
        val localUri = imageStore?.saveLocalCopy(photoUri) ?: photoUri
        sessionManager.saveSession(current.copy(photoUri = localUri))

        // 2. Subirla al servidor para que quede en la cuenta (y se vea en otros dispositivos)
        val bytes = imageStore?.readBytes(localUri)
            ?: return Result.failure(UserFacingException(UiMessage.of(R.string.err_photo_unreadable)))
        return try {
            val part = MultipartBody.Part.createFormData(
                "image", "avatar.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
            )
            val response = authApiService.uploadAvatar(part)
            val updated = response.user.toSession(
                token = current.token,
                producerNameOverride = current.producerName
            )
            sessionManager.saveSession(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo subir la foto de perfil: ${e.message}")
            Result.failure(friendlyError(e, R.string.err_photo_upload_failed))
        }
    }

    override suspend fun logout() {
        try {
            if (sessionManager.sessionFlow.value.isLoggedIn) {
                authApiService.logout()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Logout remoto fallido (se cierra la sesión local): ${e.message}")
        } finally {
            sessionManager.clearSession()
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val current = sessionManager.sessionFlow.value
        if (!current.isLoggedIn) return Result.failure(UserFacingException(UiMessage.of(R.string.err_login_required)))
        return try {
            val response = authApiService.changePassword(ChangePasswordRequestDto(currentPassword, newPassword))
            // El servidor invalida la llave anterior: se guarda la nueva para seguir con la sesión abierta
            sessionManager.saveSession(current.copy(token = response.token))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Cambio de contraseña fallido: ${e.message}")
            Result.failure(friendlyError(e, R.string.err_password_change_failed))
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
        return try {
            val updatedDto = authApiService.updateUserRole(userId, RoleUpdateRequestDto(role = newRole.name))
            sessionManager.upsertManagedUser(updatedDto.toManagedUser())
            val current = sessionManager.sessionFlow.value
            if (current.userId == userId) {
                sessionManager.saveSession(current.copy(role = newRole))
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo cambiar el rol en la API: ${e.message}")
            false
        }
    }

    override fun updateLocalProfile(fullName: String?, photoUri: String?) {
        val current = sessionManager.sessionFlow.value
        if (!current.isLoggedIn) return
        sessionManager.saveSession(
            current.copy(
                fullName = fullName?.takeIf { it.isNotBlank() } ?: current.fullName,
                photoUri = photoUri ?: current.photoUri
            )
        )
    }

    /** El error que verá la persona: lo que explica el servidor, "sin conexión" o el texto [fallbackRes]. */
    private fun friendlyError(e: Exception, @StringRes fallbackRes: Int): Exception {
        val message = when (e) {
            is HttpException -> serverMessage(e)?.let { UiMessage.raw(it) } ?: UiMessage.of(fallbackRes)
            is IOException -> UiMessage.of(R.string.err_no_connection)
            else -> UiMessage.of(fallbackRes)
        }
        return UserFacingException(message)
    }

    /** Extrae el primer mensaje de un cuerpo de error DRF (`{"campo": ["msg"]}` o `{"detail": ["msg"]}`). */
    private fun serverMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string().orEmpty()
            if (body.isBlank()) return null
            val map = Moshi.Builder().build().adapter(Map::class.java).fromJson(body) ?: return null
            map.entries.firstNotNullOfOrNull { (key, value) ->
                val text = when (value) {
                    is String -> value
                    is List<*> -> value.firstOrNull()?.toString()
                    else -> null
                }
                text?.let { if (key == "detail" || key == "error" || key == "non_field_errors") it else "$key: $it" }
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
