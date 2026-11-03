package com.example.zoco.data.remote.dto

import com.example.zoco.domain.model.NotificationItem
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "type") val type: String = "INFO",
    @Json(name = "isRead") val isRead: Boolean = false,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): NotificationItem {
        return NotificationItem(
            id = id,
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = isRead,
            type = type
        )
    }
}
