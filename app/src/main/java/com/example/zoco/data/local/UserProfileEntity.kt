package com.example.zoco.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zoco.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "Comunidad Zoco",
    val role: String = "Consumidor y Productor Local",
    val photoUri: String? = null
) {
    fun toDomain(): UserProfile {
        return UserProfile(
            id = id,
            name = name,
            role = role,
            photoUri = photoUri
        )
    }

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity {
            return UserProfileEntity(
                id = profile.id,
                name = profile.name,
                role = profile.role,
                photoUri = profile.photoUri
            )
        }
    }
}
