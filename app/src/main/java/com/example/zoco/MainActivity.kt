package com.example.zoco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zoco.data.local.ZocoDatabase
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.ui.cart.CartScreen
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogScreen
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.common.ViewModelFactory
import com.example.zoco.ui.navigation.Screen
import com.example.zoco.ui.profile.ProfileScreen
import com.example.zoco.ui.seller.SellerScreen
import com.example.zoco.ui.seller.SellerViewModel
import com.example.zoco.ui.theme.ZocoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ZocoDatabase.getInstance(applicationContext)
        val repository = ProductRepositoryImpl(database.productDao())
        val viewModelFactory = ViewModelFactory(repository)

        setContent {
            ZocoTheme {
                ZocoApp(viewModelFactory = viewModelFactory)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZocoApp(
    viewModelFactory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    val catalogViewModel: CatalogViewModel = viewModel(factory = viewModelFactory)
    val cartViewModel: CartViewModel = viewModel()
    val sellerViewModel: SellerViewModel = viewModel(factory = viewModelFactory)

    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val sellerState by sellerViewModel.uiState.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(Screen.CATALOG) }
    val snackbarHostState = remember { SnackbarHostState() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    Screen.values().forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = {
                                if (screen == Screen.CART && cartState.items.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(cartState.items.size.toString()) } }) {
                                        Icon(screen.icon, contentDescription = stringResource(screen.titleResId))
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = stringResource(screen.titleResId))
                                }
                            },
                            label = { Text(stringResource(screen.titleResId)) }
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.app_name) + " - " + stringResource(currentScreen.titleResId),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    ScreenContent(
                        currentScreen = currentScreen,
                        catalogViewModel = catalogViewModel,
                        catalogState = catalogState,
                        cartViewModel = cartViewModel,
                        cartState = cartState,
                        sellerViewModel = sellerViewModel,
                        sellerState = sellerState,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                bottomBar = {
                    NavigationBar {
                        Screen.values().forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = {
                                    if (screen == Screen.CART && cartState.items.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text(cartState.items.size.toString()) } }) {
                                            Icon(screen.icon, contentDescription = stringResource(screen.titleResId))
                                        }
                                    } else {
                                        Icon(screen.icon, contentDescription = stringResource(screen.titleResId))
                                    }
                                },
                                label = { Text(stringResource(screen.titleResId)) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                ScreenContent(
                    currentScreen = currentScreen,
                    catalogViewModel = catalogViewModel,
                    catalogState = catalogState,
                    cartViewModel = cartViewModel,
                    cartState = cartState,
                    sellerViewModel = sellerViewModel,
                    sellerState = sellerState,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    currentScreen: Screen,
    catalogViewModel: CatalogViewModel,
    catalogState: com.example.zoco.ui.catalog.CatalogUiState,
    cartViewModel: CartViewModel,
    cartState: com.example.zoco.ui.cart.CartUiState,
    sellerViewModel: SellerViewModel,
    sellerState: com.example.zoco.ui.seller.SellerUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    when (currentScreen) {
        Screen.CATALOG -> {
            CatalogScreen(
                uiState = catalogState,
                onSearchChange = catalogViewModel::onSearchQueryChanged,
                onCategoryChange = catalogViewModel::onCategorySelected,
                onExpressFilterChange = catalogViewModel::onExpressFilterToggled,
                onAddToCart = cartViewModel::addProduct,
                snackbarHostState = snackbarHostState,
                modifier = modifier
            )
        }
        Screen.CART -> {
            CartScreen(
                uiState = cartState,
                onRemoveProduct = cartViewModel::removeProduct,
                onTimeSelected = cartViewModel::setScheduledTime,
                onConfirmOrder = cartViewModel::confirmOrder,
                modifier = modifier
            )
        }
        Screen.SELLER -> {
            SellerScreen(
                uiState = sellerState,
                onNameChange = sellerViewModel::onNameChanged,
                onDescriptionChange = sellerViewModel::onDescriptionChanged,
                onPriceChange = sellerViewModel::onPriceChanged,
                onCategoryChange = sellerViewModel::onCategoryChanged,
                onProducerNameChange = sellerViewModel::onProducerNameChanged,
                onExpressChange = sellerViewModel::onExpressChanged,
                onSaveProduct = sellerViewModel::saveProduct,
                modifier = modifier
            )
        }
        Screen.PROFILE -> {
            ProfileScreen(
                modifier = modifier
            )
        }
    }
}