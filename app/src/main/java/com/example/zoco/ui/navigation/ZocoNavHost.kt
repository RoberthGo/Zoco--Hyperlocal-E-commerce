package com.example.zoco.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.zoco.ui.cart.CartScreen
import com.example.zoco.ui.cart.CartUiState
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogScreen
import com.example.zoco.ui.catalog.CatalogUiState
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.profile.ProfileScreen
import com.example.zoco.ui.seller.SellerScreen
import com.example.zoco.ui.seller.SellerUiState
import com.example.zoco.ui.seller.SellerViewModel

@Composable
fun ZocoNavHost(
    navController: NavHostController,
    catalogViewModel: CatalogViewModel,
    catalogState: CatalogUiState,
    cartViewModel: CartViewModel,
    cartState: CartUiState,
    sellerViewModel: SellerViewModel,
    sellerState: SellerUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CATALOG.route,
        modifier = modifier
    ) {
        composable(route = Screen.CATALOG.route) {
            CatalogScreen(
                uiState = catalogState,
                onSearchChange = catalogViewModel::onSearchQueryChanged,
                onCategoryChange = catalogViewModel::onCategorySelected,
                onExpressFilterChange = catalogViewModel::onExpressFilterToggled,
                onAddToCart = cartViewModel::addProduct,
                snackbarHostState = snackbarHostState
            )
        }

        composable(route = Screen.CART.route) {
            CartScreen(
                uiState = cartState,
                onRemoveProduct = cartViewModel::removeProduct,
                onTimeSelected = cartViewModel::setScheduledTime,
                onConfirmOrder = cartViewModel::confirmOrder
            )
        }

        composable(route = Screen.SELLER.route) {
            SellerScreen(
                uiState = sellerState,
                onNameChange = sellerViewModel::onNameChanged,
                onDescriptionChange = sellerViewModel::onDescriptionChanged,
                onPriceChange = sellerViewModel::onPriceChanged,
                onCategoryChange = sellerViewModel::onCategoryChanged,
                onProducerNameChange = sellerViewModel::onProducerNameChanged,
                onExpressChange = sellerViewModel::onExpressChanged,
                onSaveProduct = sellerViewModel::saveProduct
            )
        }

        composable(route = Screen.PROFILE.route) {
            ProfileScreen()
        }
    }
}
