package com.example.zoco.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zoco.domain.model.NotificationItem

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "INFO"
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

    companion object {
        fun fromDomain(item: NotificationItem): NotificationEntity {
            return NotificationEntity(
                id = item.id,
                title = item.title,
                message = item.message,
                timestamp = item.timestamp,
                isRead = item.isRead,
                type = item.type
            )
        }
    }
}
