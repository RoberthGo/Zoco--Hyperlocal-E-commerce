package com.example.zoco.data.remote.dto

import com.example.zoco.domain.model.ManagedUser
import com.example.zoco.domain.model.UserRole
import com.example.zoco.domain.model.UserSession
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "first_name") val firstName: String = "",
    @Json(name = "last_name") val lastName: String = "",
    @Json(name = "role") val role: String = "BUYER",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "address") val address: String = ""
)

@JsonClass(generateAdapter = true)
data class BecomeSellerRequestDto(
    @Json(name = "producerName") val producerName: String = ""
)

@JsonClass(generateAdapter = true)
data class RoleUpdateRequestDto(
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "username") val username: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "first_name") val firstName: String = "",
    @Json(name = "last_name") val lastName: String = "",
    @Json(name = "role") val role: String = "BUYER",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "address") val address: String = "",
    @Json(name = "preferredLanguage") val preferredLanguage: String = "es",
    @Json(name = "avatar") val avatar: String? = null
) {
    fun toSession(token: String? = null, producerNameOverride: String = ""): UserSession {
        val fullName = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { username }
        return UserSession(
            userId = id,
            username = username,
            fullName = fullName,
            email = email,
            role = UserRole.fromString(role),
            phone = phone,
            address = address,
            producerName = producerNameOverride.ifBlank { fullName },
            token = token,
            photoUri = avatar
        )
    }

    fun toManagedUser(): ManagedUser {
        val fullName = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { username }
        return ManagedUser(
            id = id,
            username = username,
            fullName = fullName,
            email = email,
            role = UserRole.fromString(role),
            phone = phone
        )
    }
}

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "token") val token: String = "",
    @Json(name = "user") val user: UserDto,
    @Json(name = "message") val message: String = ""
)

@JsonClass(generateAdapter = true)
data class BecomeSellerResponseDto(
    @Json(name = "user") val user: UserDto,
    @Json(name = "message") val message: String = ""
)

@JsonClass(generateAdapter = true)
data class AvatarResponseDto(
    @Json(name = "user") val user: UserDto,
    @Json(name = "message") val message: String = ""
)
