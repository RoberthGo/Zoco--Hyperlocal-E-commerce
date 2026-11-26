package com.example.zoco.domain.model

/**
 * Qué tipo de cuenta es. Comprador (por defecto), vendedor (puede publicar productos) o administrador
 * (solo se crea desde el servidor; nadie puede registrarse como admin).
 */
enum class UserRole {
    BUYER,
    SELLER,
    ADMIN;

    val canAccessSeller: Boolean
        get() = this == SELLER || this == ADMIN

    val canAccessAdmin: Boolean
        get() = this == ADMIN

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: BUYER
        }
    }
}

/**
 * Los datos de la persona que tiene la sesión iniciada. [token] es la "llave" que entrega el servidor
 * al hacer login; sin token ([isLoggedIn] = false) no hay sesión.
 */
data class UserSession(
    val userId: Long,
    val username: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val phone: String = "",
    val address: String = "",
    val producerName: String = "",
    val token: String? = null,
    val photoUri: String? = null
) {
    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    companion object {
        val LOGGED_OUT = UserSession(
            userId = 0L,
            username = "",
            fullName = "",
            email = "",
            role = UserRole.BUYER
        )
    }
}

/**
 * Un usuario tal como lo ve el administrador en su lista (sin token ni datos de sesión).
 */
data class ManagedUser(
    val id: Long,
    val username: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val phone: String = ""
)
