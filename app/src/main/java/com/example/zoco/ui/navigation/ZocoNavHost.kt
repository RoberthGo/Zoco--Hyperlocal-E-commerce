package com.example.zoco.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.zoco.ui.admin.AdminScreen
import com.example.zoco.ui.admin.AdminUiState
import com.example.zoco.ui.admin.AdminViewModel
import com.example.zoco.ui.cart.CartScreen
import com.example.zoco.ui.cart.CartUiState
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogScreen
import com.example.zoco.ui.catalog.CatalogUiState
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.profile.ProfileScreen
import com.example.zoco.ui.profile.ProfileUiState
import com.example.zoco.ui.profile.ProfileViewModel
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
    adminViewModel: AdminViewModel,
    adminState: AdminUiState,
    profileViewModel: ProfileViewModel,
    profileState: ProfileUiState,
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
                onAddToCart = { product, quantity -> cartViewModel.addProduct(product, quantity) },
                onSyncWeb = catalogViewModel::refreshFromServer,
                onDeleteProduct = catalogViewModel::deleteProduct,
                snackbarHostState = snackbarHostState
            )
        }

        composable(route = Screen.CART.route) {
            CartScreen(
                uiState = cartState,
                onRemoveProduct = cartViewModel::removeProduct,
                onTimeSelected = cartViewModel::setScheduledTime,
                onNameChange = cartViewModel::onCustomerNameChanged,
                onPhoneChange = cartViewModel::onCustomerPhoneChanged,
                onAddressChange = cartViewModel::onDeliveryAddressChanged,
                onExpressChange = cartViewModel::onExpressChanged,
                onConfirmOrder = cartViewModel::confirmOrder,
                onSyncOrders = cartViewModel::refreshOrdersFromServer
            )
        }

        composable(route = Screen.SELLER.route) {
            SellerScreen(
                uiState = sellerState,
                onNameChange = sellerViewModel::onNameChanged,
                onDescriptionChange = sellerViewModel::onDescriptionChanged,
                onPriceChange = sellerViewModel::onPriceChanged,
                onStockChange = sellerViewModel::onStockChanged,
                onCategoryChange = sellerViewModel::onCategoryChanged,
                onProducerNameChange = sellerViewModel::onProducerNameChanged,
                onExpressChange = sellerViewModel::onExpressChanged,
                onImageUriChange = sellerViewModel::onImageUriChanged,
                onSaveProduct = sellerViewModel::saveProduct,
                onDeleteOwnProduct = sellerViewModel::deleteOwnProduct,
                onUpdateOrderStatus = sellerViewModel::updateOrderStatus
            )
        }

        composable(route = Screen.ADMIN.route) {
            AdminScreen(
                uiState = adminState,
                onRefresh = adminViewModel::refreshAll,
                onChangeUserRole = adminViewModel::changeUserRole,
                onChangeOrderStatus = adminViewModel::changeOrderStatus,
                onDeleteProduct = adminViewModel::deleteProductByModeration
            )
        }

        composable(route = Screen.PROFILE.route) {
            ProfileScreen(
                uiState = profileState,
                onUpdatePhoto = profileViewModel::updatePhoto,
                onUpdateName = profileViewModel::updateName,
                onLanguageChange = profileViewModel::setLanguage,
                onBecomeSeller = profileViewModel::becomeSeller,
                onLogout = profileViewModel::logout,
                onChangePassword = profileViewModel::changePassword,
                onPasswordDialogClosed = profileViewModel::clearPasswordChanged
            )
        }
    }
}
