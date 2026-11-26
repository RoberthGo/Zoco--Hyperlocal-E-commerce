package com.example.zoco

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistryOwner
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
import androidx.compose.runtime.ProvidedValue
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
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zoco.domain.model.UserRole
import com.example.zoco.ui.admin.AdminViewModel
import com.example.zoco.ui.auth.AuthScreen
import com.example.zoco.ui.auth.AuthViewModel
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.common.ViewModelFactory
import com.example.zoco.ui.navigation.Screen
import com.example.zoco.ui.navigation.ZocoNavHost
import com.example.zoco.ui.profile.ProfileViewModel
import com.example.zoco.ui.seller.SellerViewModel
import com.example.zoco.ui.theme.ZocoTheme
import java.util.Locale

/** Ancho a partir del cual se usa la barra lateral (tablets) en vez de la barra inferior (celulares). */
private val WIDE_SCREEN_MIN_WIDTH = 600.dp

/** Única Activity de la app: arma las piezas compartidas y muestra [ZocoApp]. */
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
            networkConnectivityChecker = app.networkConnectivityChecker,
            sharedPreferences = prefs,
            authRepository = app.authRepository,
            findServer = app.serverDiscovery::findServer
        )

        setContent {
            ZocoTheme {
                ZocoApp(viewModelFactory = viewModelFactory)
            }
        }
    }
}

/**
 * Pantalla raíz. Decide qué se ve:
 *  - sin sesión iniciada  -> pantalla de login/registro
 *  - con sesión, pantalla ancha  -> barra lateral + contenido
 *  - con sesión, pantalla angosta -> contenido + barra inferior
 * Las pestañas visibles dependen del rol del usuario (ver [Screen.visibleFor]).
 */
@Composable
fun ZocoApp(
    viewModelFactory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- 1. Un ViewModel por pantalla y su estado actual ---
    val catalogViewModel: CatalogViewModel = viewModel(factory = viewModelFactory)
    val cartViewModel: CartViewModel = viewModel(factory = viewModelFactory)
    val sellerViewModel: SellerViewModel = viewModel(factory = viewModelFactory)
    val adminViewModel: AdminViewModel = viewModel(factory = viewModelFactory)
    val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)

    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val sellerState by sellerViewModel.uiState.collectAsStateWithLifecycle()
    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val session = profileState.currentSession
    val isLoggedIn = session.isLoggedIn
    val visibleScreens = remember(session.role) { Screen.visibleFor(session.role) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)

    // --- 2. Reglas automáticas ---
    // Si la pestaña actual ya no está permitida para el rol (por ejemplo, al cambiar de cuenta), volver al catálogo.
    LaunchedEffect(session.role, currentRoute) {
        if (currentScreen !in visibleScreens) {
            navController.navigate(Screen.CATALOG.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Al iniciar sesión (o cambiar de usuario/rol) se descargan los datos del servidor.
    LaunchedEffect(session.username, session.role) {
        if (!isLoggedIn) return@LaunchedEffect
        catalogViewModel.syncFromWeb()
        cartViewModel.syncOrdersFromWeb()
        if (session.role == UserRole.ADMIN) {
            adminViewModel.refreshAll()
        }
    }

    // --- 3. Contenido de las pestañas (se reutiliza en barra lateral y barra inferior) ---
    val tabsContent: @Composable (Modifier) -> Unit = { contentModifier ->
        ZocoNavHost(
            navController = navController,
            catalogViewModel = catalogViewModel,
            catalogState = catalogState,
            cartViewModel = cartViewModel,
            cartState = cartState,
            sellerViewModel = sellerViewModel,
            sellerState = sellerState,
            adminViewModel = adminViewModel,
            adminState = adminState,
            profileViewModel = profileViewModel,
            profileState = profileState,
            snackbarHostState = snackbarHostState,
            modifier = contentModifier
        )
    }

    // --- 4. Pantalla según idioma y estado de sesión ---
    LanguageProvider(languageTag = profileState.selectedLanguage) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= WIDE_SCREEN_MIN_WIDTH

            when {
                !isLoggedIn -> Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = { ZocoTopBar(title = stringResource(R.string.app_name)) }
                ) { innerPadding ->
                    AuthScreen(
                        uiState = authState,
                        onLogin = authViewModel::login,
                        onRegister = authViewModel::register,
                        onClearError = authViewModel::clearError,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                isWideScreen -> Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        visibleScreens.forEach { screen ->
                            NavigationRailItem(
                                selected = currentScreen == screen,
                                onClick = { navigateToTab(navController, screen, currentRoute) },
                                icon = { TabIcon(screen, cartItemCount = cartState.items.size) },
                                label = { Text(stringResource(screen.titleResId)) }
                            )
                        }
                    }
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            ZocoTopBar(
                                title = stringResource(R.string.app_name) + " - " + stringResource(currentScreen.titleResId)
                            )
                        }
                    ) { innerPadding ->
                        tabsContent(Modifier.padding(innerPadding))
                    }
                }

                else -> Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = { ZocoTopBar(title = stringResource(R.string.app_name)) },
                    bottomBar = {
                        NavigationBar {
                            visibleScreens.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { navigateToTab(navController, screen, currentRoute) },
                                    icon = { TabIcon(screen, cartItemCount = cartState.items.size) },
                                    label = { Text(stringResource(screen.titleResId)) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    tabsContent(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** Barra superior con el color principal de la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZocoTopBar(title: String) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/** Ícono de una pestaña; en el carrito muestra un globo con la cantidad de productos. */
@Composable
private fun TabIcon(screen: Screen, cartItemCount: Int) {
    val icon = @Composable {
        Icon(screen.icon, contentDescription = stringResource(screen.titleResId))
    }
    if (screen == Screen.CART && cartItemCount > 0) {
        BadgedBox(badge = { Badge { Text(cartItemCount.toString()) } }) { icon() }
    } else {
        icon()
    }
}

/** Cambia de pestaña conservando el estado de cada una (scroll, texto escrito, etc.). */
private fun navigateToTab(navController: NavController, screen: Screen, currentRoute: String?) {
    if (currentRoute == screen.route) return
    navController.navigate(screen.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Hace que toda la app use el idioma elegido en Perfil, sin reiniciar:
 * crea un contexto con ese idioma y lo ofrece a todos los textos de [content].
 */
@Composable
private fun LanguageProvider(languageTag: String, content: @Composable () -> Unit) {
    val locale = remember(languageTag) { Locale.forLanguageTag(languageTag) }
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

    // En Android 13+ también se avisa al sistema del idioma de la app.
    LaunchedEffect(languageTag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                currentContext.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
                    LocaleList.forLanguageTags(languageTag)
            } catch (_: Exception) {
                // Si el sistema no permite cambiar el idioma, la app sigue funcionando con su propio contexto
            }
        }
    }

    // El nuevo contexto pierde el "registro de resultados" que usan la cámara y la galería: se vuelve a ofrecer.
    val activityResultOwner = LocalActivityResultRegistryOwner.current
        ?: (currentContext as? ActivityResultRegistryOwner)

    val provided = mutableListOf<ProvidedValue<*>>(
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext
    )
    if (activityResultOwner != null) {
        provided.add(LocalActivityResultRegistryOwner provides activityResultOwner)
    }

    CompositionLocalProvider(*provided.toTypedArray(), content = content)
}
