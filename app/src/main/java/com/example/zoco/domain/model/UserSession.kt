package com.example.zoco.domain.model

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
)

data class ManagedUser(
    val id: Long,
    val username: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val phone: String = ""
)

data class PresetAccount(
    val session: UserSession,
    val password: String
)

object PredefinedUsers {
    val BUYER = PresetAccount(
        session = UserSession(
            userId = 3L,
            username = "laura_compradora",
            fullName = "Laura Gómez",
            email = "laura@gmail.com",
            role = UserRole.BUYER,
            phone = "4459876543",
            address = "Av. Juárez #120, Col. San Miguel",
            producerName = ""
        ),
        password = "zoco123"
    )

    val SELLER = PresetAccount(
        session = UserSession(
            userId = 2L,
            username = "vendedor_juan",
            fullName = "Juan Pérez",
            email = "juan@panaderiazoco.com",
            role = UserRole.SELLER,
            phone = "4451234567",
            address = "Calle Hidalgo #45, Centro",
            producerName = "Panadería Don Juan"
        ),
        password = "zoco123"
    )

    val SECOND_SELLER = PresetAccount(
        session = UserSession(
            userId = 4L,
            username = "artesana_maria",
            fullName = "María López",
            email = "maria@artesaniazoco.com",
            role = UserRole.SELLER,
            phone = "4457778899",
            address = "Calle Zaragoza #18, Centro",
            producerName = "Artesanías La Esperanza"
        ),
        password = "zoco123"
    )

    val ADMIN = PresetAccount(
        session = UserSession(
            userId = 1L,
            username = "admin",
            fullName = "Admin Zoco",
            email = "admin@zoco.local",
            role = UserRole.ADMIN,
            phone = "4450000001",
            address = "Oficina Central Zoco",
            producerName = "Administración Zoco"
        ),
        password = "admin123"
    )

    fun forRole(role: UserRole): PresetAccount {
        return when (role) {
            UserRole.BUYER -> BUYER
            UserRole.SELLER -> SELLER
            UserRole.ADMIN -> ADMIN
        }
    }

    val ALL_MANAGED_USERS: List<ManagedUser> = listOf(BUYER, SELLER, SECOND_SELLER, ADMIN).map {
        ManagedUser(
            id = it.session.userId,
            username = it.session.username,
            fullName = it.session.fullName,
            email = it.session.email,
            role = it.session.role,
            phone = it.session.phone
        )
    }
}
