package com.example.zoco.domain.model

data class UserProfile(
    val id: Long = 1L,
    val name: String = "Comunidad Zoco",
    val role: String = "Consumidor y Productor Local",
    val photoUri: String? = null
)
