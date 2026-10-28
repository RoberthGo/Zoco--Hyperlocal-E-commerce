package com.example.zoco.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zoco.domain.model.MediaItem

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long? = null,
    val uri: String,
    val mediaType: String = "IMAGE",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MediaItem {
        return MediaItem(
            id = id,
            productId = productId,
            uri = uri,
            mediaType = mediaType,
            description = description,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(item: MediaItem): MediaEntity {
            return MediaEntity(
                id = item.id,
                productId = item.productId,
                uri = item.uri,
                mediaType = item.mediaType,
                description = item.description,
                createdAt = item.createdAt
            )
        }
    }
}
