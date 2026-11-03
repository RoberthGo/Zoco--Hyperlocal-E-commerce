package com.example.zoco.data.remote.api

import com.example.zoco.data.remote.dto.NotificationDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApiService {

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("notifications/{id}/read/")
    suspend fun markAsRead(@Path("id") id: Long): NotificationDto

    @POST("notifications/mark-all-read/")
    suspend fun markAllAsRead(): Map<String, String>
}
