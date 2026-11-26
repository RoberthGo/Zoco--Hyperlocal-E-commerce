package com.example.zoco.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zoco.R
import com.example.zoco.domain.model.UserRole

/**
 * Las pestañas de la barra de navegación. Cada una tiene una ruta (`route`), un título y un ícono.
 * [visibleFor] decide qué pestañas ve cada rol:
 *  - comprador: Catálogo, Carrito, Perfil
 *  - vendedor: las anteriores + Vender
 *  - administrador: todas + Admin
 * Solo oculta pestañas; la seguridad real (qué puede hacer cada rol) la aplica el servidor.
 */
enum class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    CATALOG("catalog", R.string.nav_catalog, Icons.Default.ShoppingBag),
    CART("cart", R.string.nav_cart, Icons.Default.ShoppingCart),
    SELLER("seller", R.string.nav_seller, Icons.Default.Storefront),
    ADMIN("admin", R.string.nav_admin, Icons.Default.AdminPanelSettings),
    PROFILE("profile", R.string.nav_profile, Icons.Default.Person);

    companion object {
        fun fromRoute(route: String?): Screen {
            return entries.firstOrNull { it.route == route } ?: CATALOG
        }

        fun visibleFor(role: UserRole): List<Screen> {
            return when (role) {
                UserRole.BUYER -> listOf(CATALOG, CART, PROFILE)
                UserRole.SELLER -> listOf(CATALOG, CART, SELLER, PROFILE)
                UserRole.ADMIN -> listOf(CATALOG, CART, SELLER, ADMIN, PROFILE)
            }
        }
    }
}
