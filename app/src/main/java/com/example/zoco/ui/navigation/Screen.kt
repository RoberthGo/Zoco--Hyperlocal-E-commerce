package com.example.zoco.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zoco.R

enum class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    CATALOG("catalog", R.string.nav_catalog, Icons.Default.ShoppingBag),
    CART("cart", R.string.nav_cart, Icons.Default.ShoppingCart),
    SELLER("seller", R.string.nav_seller, Icons.Default.Storefront),
    PROFILE("profile", R.string.nav_profile, Icons.Default.Person);

    companion object {
        fun fromRoute(route: String?): Screen {
            return entries.firstOrNull { it.route == route } ?: CATALOG
        }
    }
}
