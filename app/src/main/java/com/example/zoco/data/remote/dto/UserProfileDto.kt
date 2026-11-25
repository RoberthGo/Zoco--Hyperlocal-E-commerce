package com.example.zoco.data.remote.dto

import com.example.zoco.domain.model.UserProfile
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: Long = 1,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String = "Consumidor y Productor Local",
    @Json(name = "photoUrl") val photoUrl: String? = null
) {
    fun toDomain(): UserProfile {
        return UserProfile(
            id = id,
            name = name,
            role = role,
            photoUri = photoUrl
        )
    }

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileDto {
            return UserProfileDto(
                id = profile.id,
                name = profile.name,
                role = profile.role,
                photoUrl = profile.photoUri
            )
        }
    }
}
