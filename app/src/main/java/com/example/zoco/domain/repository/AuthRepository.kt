package com.example.zoco.domain.repository

import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Todo lo relacionado con la cuenta del usuario: login, registro, cerrar sesión, cambiar a vendedor,
 * foto de perfil y (para el admin) la lista de usuarios. La implementación habla con el servidor y
 * guarda la sesión en [SessionManager].
 */
interface AuthRepository {
    val sessionFlow: StateFlow<UserSession>
    val usersFlow: StateFlow<List<ManagedUser>>

    suspend fun login(username: String, password: String): Result<UserSession>

    /** Registro público: siempre crea una cuenta de comprador. */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String
    ): Result<UserSession>

    suspend fun becomeSeller(producerName: String? = null): Result<UserSession>
    suspend fun logout()

    /** Cambia la contraseña de la cuenta. Pide la actual; el servidor entrega una llave (token) nueva. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /** Cambia la foto de perfil: se muestra al instante y se sube al servidor. */
    suspend fun updateAvatar(photoUri: String): Result<UserSession>
    suspend fun refreshUsers(): Boolean
    suspend fun updateUserRole(userId: Long, newRole: UserRole): Boolean
    fun updateLocalProfile(fullName: String? = null, photoUri: String? = null)
}
