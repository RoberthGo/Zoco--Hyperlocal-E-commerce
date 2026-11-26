package com.example.zoco.domain.repository

import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionFlow: StateFlow<UserSession>
    val usersFlow: StateFlow<List<ManagedUser>>

    suspend fun quickSwitch(role: UserRole): Result<UserSession>
    suspend fun login(username: String, password: String): Result<UserSession>
    suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String,
        role: UserRole
    ): Result<UserSession>

    suspend fun becomeSeller(producerName: String? = null): Result<UserSession>
    suspend fun refreshUsers(): Boolean
    suspend fun updateUserRole(userId: Long, newRole: UserRole): Boolean
    fun updateLocalProfile(fullName: String? = null, photoUri: String? = null)
}
