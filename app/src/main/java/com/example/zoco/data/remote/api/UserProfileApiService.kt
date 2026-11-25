package com.example.zoco.data.remote.api

import com.example.zoco.data.remote.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserProfileApiService {
    @GET("profile")
    suspend fun getProfile(): UserProfileDto

    @PUT("profile")
    suspend fun updateProfile(@Body profile: UserProfileDto): UserProfileDto
}
