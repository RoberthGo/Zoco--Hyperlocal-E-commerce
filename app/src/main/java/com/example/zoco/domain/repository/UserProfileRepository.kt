package com.example.zoco.domain.repository

import com.example.zoco.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile>
    suspend fun updateProfile(name: String, photoUri: String?): Boolean
    suspend fun syncProfileFromRemote(): Boolean
}
