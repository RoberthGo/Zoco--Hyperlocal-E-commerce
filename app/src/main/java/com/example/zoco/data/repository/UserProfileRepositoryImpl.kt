package com.example.zoco.data.repository

import com.example.zoco.data.local.UserProfileDao
import com.example.zoco.data.local.UserProfileEntity
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.dto.UserProfileDto
import com.example.zoco.domain.model.UserProfile
import com.example.zoco.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepositoryImpl(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override fun getUserProfile(): Flow<UserProfile> {
        return userProfileDao.getUserProfile().map { entity ->
            entity?.toDomain() ?: UserProfile()
        }
    }

    override suspend fun updateProfile(name: String, photoUri: String?): Boolean {
        val profile = UserProfile(id = 1L, name = name, role = "Consumidor y Productor Local", photoUri = photoUri)
        userProfileDao.insertOrUpdate(UserProfileEntity.fromDomain(profile))

        return try {
            RetrofitClient.userProfileApiService.updateProfile(UserProfileDto.fromDomain(profile))
            true
        } catch (_: Exception) {
            true
        }
    }

    override suspend fun syncProfileFromRemote(): Boolean {
        return try {
            val remoteDto = RetrofitClient.userProfileApiService.getProfile()
            userProfileDao.insertOrUpdate(UserProfileEntity.fromDomain(remoteDto.toDomain()))
            true
        } catch (_: Exception) {
            false
        }
    }
}
