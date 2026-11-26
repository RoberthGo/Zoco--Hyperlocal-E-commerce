package com.example.zoco.domain.model

enum class ProductCategory {
    ALL,
    BAKERY,
    PRODUCE,
    DAIRY,
    CRAFTS
}

/**
 * Un producto del catálogo.
 *  - [isRemote]: viene del servidor (si es false, solo existe en este teléfono).
 *  - [pendingSync]: tiene cambios que aún no se han subido al servidor.
 *  - [sellerId] / [sellerUsername]: quién lo vende; se usa para saber quién puede editarlo o borrarlo.
 *  - [imageUrl]: foto (URL del servidor o archivo local si todavía no se ha subido).
 */
data class Product(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val price: Double,
    val category: ProductCategory = ProductCategory.ALL,
    val producerName: String,
    val isExpressDelivery: Boolean = false,
    val inStock: Boolean = true,
    val isRemote: Boolean = false,
    val imageUrl: String? = null,
    val stock: Int = 10,
    val pendingSync: Boolean = false,
    val isOwnedByUser: Boolean = false,
    val sellerId: Long? = null,
    val sellerUsername: String? = null
) {
    fun isOwnedBy(session: UserSession): Boolean {
        if (!sellerUsername.isNullOrBlank()) {
            return sellerUsername == session.username
        }
        if (sellerId != null && session.userId > 0L) {
            return sellerId == session.userId
        }
        return isOwnedByUser && session.role.canAccessSeller
    }

    fun canBeDeletedBy(session: UserSession): Boolean {
        return when (session.role) {
            UserRole.ADMIN -> true
            UserRole.SELLER -> isOwnedBy(session)
            UserRole.BUYER -> false
        }
    }
}
