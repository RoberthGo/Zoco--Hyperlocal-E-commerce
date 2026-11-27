package com.example.zoco.data.repository

import android.util.Log
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.api.NotificationApiService
import com.example.zoco.domain.model.NotificationItem
import com.example.zoco.domain.repository.NotificationRepository

/** Lee y marca como leídas las notificaciones usando la API del servidor. */
class NotificationRepositoryImpl(
    private val customApiService: NotificationApiService? = null
) : NotificationRepository {

    private val api: NotificationApiService
        get() = customApiService ?: RetrofitClient.notificationApiService

    override suspend fun fetch(): Result<List<NotificationItem>> = try {
        Result.success(api.getNotifications().map { it.toDomain() }.sortedByDescending { it.timestamp })
    } catch (e: Exception) {
        Log.w(TAG, "No se pudieron cargar las notificaciones: ${e.message}")
        Result.failure(e)
    }

    override suspend fun markRead(id: Long): Boolean = try {
        api.markAsRead(id)
        true
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo marcar la notificación $id: ${e.message}")
        false
    }

    override suspend fun markAllRead(): Boolean = try {
        api.markAllAsRead()
        true
    } catch (e: Exception) {
        Log.w(TAG, "No se pudieron marcar las notificaciones: ${e.message}")
        false
    }

    private companion object {
        const val TAG = "NotificationRepository"
    }
}
