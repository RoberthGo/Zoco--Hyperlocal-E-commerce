package com.example.zoco.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zoco.R

enum class Screen(
    val titleResId: Int,
    val icon: ImageVector
) {
    CATALOG(R.string.nav_catalog, Icons.Default.ShoppingBag),
    CART(R.string.nav_cart, Icons.Default.ShoppingCart),
    SELLER(R.string.nav_seller, Icons.Default.Storefront),
    PROFILE(R.string.nav_profile, Icons.Default.Person)
}
