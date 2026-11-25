package com.example.zoco

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.common.ViewModelFactory
import com.example.zoco.ui.navigation.Screen
import com.example.zoco.ui.navigation.ZocoNavHost
import com.example.zoco.ui.profile.ProfileViewModel
import com.example.zoco.ui.seller.SellerViewModel
import com.example.zoco.ui.theme.ZocoTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ZocoApplication
        val prefs = getSharedPreferences("zoco_prefs", Context.MODE_PRIVATE)
        val viewModelFactory = ViewModelFactory(
            productRepository = app.productRepository,
            orderRepository = app.orderRepository,
            userProfileRepository = app.userProfileRepository,
            sharedPreferences = prefs
        )

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
    val navController = rememberNavController()

    val catalogViewModel: CatalogViewModel = viewModel(factory = viewModelFactory)
    val cartViewModel: CartViewModel = viewModel(factory = viewModelFactory)
    val sellerViewModel: SellerViewModel = viewModel(factory = viewModelFactory)
    val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)

    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val sellerState by sellerViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentScreen = Screen.fromRoute(currentRoute)

    val snackbarHostState = remember { SnackbarHostState() }

    // Soporte para internacionalización dinámica en tiempo real (TASK-FINAL-03)
    val locale = remember(profileState.selectedLanguage) {
        Locale.forLanguageTag(profileState.selectedLanguage)
    }
    val currentConfig = LocalConfiguration.current
    val currentContext = LocalContext.current
    val localizedConfig = remember(locale, currentConfig) {
        Configuration(currentConfig).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localizedContext = remember(locale, currentContext) {
        currentContext.createConfigurationContext(localizedConfig)
    }

    LaunchedEffect(profileState.selectedLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                currentContext.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
                    LocaleList.forLanguageTags(profileState.selectedLanguage)
            } catch (_: Exception) {}
        }
    }

    val activityResultOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
        ?: (currentContext as? androidx.activity.result.ActivityResultRegistryOwner)

    val providers = mutableListOf<androidx.compose.runtime.ProvidedValue<*>>(
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext
    )
    if (activityResultOwner != null) {
        providers.add(androidx.activity.compose.LocalActivityResultRegistryOwner provides activityResultOwner)
    }

    CompositionLocalProvider(
        *providers.toTypedArray()
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        Screen.entries.forEach { screen ->
                            NavigationRailItem(
                                selected = currentScreen == screen,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
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
                        ZocoNavHost(
                            navController = navController,
                            catalogViewModel = catalogViewModel,
                            catalogState = catalogState,
                            cartViewModel = cartViewModel,
                            cartState = cartState,
                            sellerViewModel = sellerViewModel,
                            sellerState = sellerState,
                            profileViewModel = profileViewModel,
                            profileState = profileState,
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
                            Screen.entries.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
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
                    ZocoNavHost(
                        navController = navController,
                        catalogViewModel = catalogViewModel,
                        catalogState = catalogState,
                        cartViewModel = cartViewModel,
                        cartState = cartState,
                        sellerViewModel = sellerViewModel,
                        sellerState = sellerState,
                        profileViewModel = profileViewModel,
                        profileState = profileState,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
