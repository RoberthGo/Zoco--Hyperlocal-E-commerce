package com.example.zoco

import com.example.zoco.ui.notifications.NotificationsViewModel
import com.example.zoco.ui.notifications.NotificationsDialog
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.Icons
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import com.example.zoco.domain.model.UserSession
import com.example.zoco.ui.common.Breakpoints
import com.example.zoco.ui.common.LocalWindowSize
import com.example.zoco.ui.common.WidthClass
import com.example.zoco.ui.common.WindowSize
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
import androidx.navigation.NavHostController
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
            findServer = app.serverDiscovery::findServer,
            notificationRepository = app.notificationRepository
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
 *  - con sesión: el menú cambia según el tamaño de la ventana (ver [navigationStyleFor]):
 *      celular en vertical -> barra inferior; plegable/tablet pequeña -> columna de íconos;
 *      tablet grande -> menú lateral con nombres y datos de la persona
 * Las pestañas visibles dependen del rol del usuario (ver [Screen.visibleFor]).
 */
@Composable
fun ZocoApp(
    viewModelFactory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val vms = rememberAppViewModels(viewModelFactory)

    val profileState by vms.profile.uiState.collectAsStateWithLifecycle()
    val notificationsState by vms.notifications.uiState.collectAsStateWithLifecycle()
    var showNotifications by remember { mutableStateOf(false) }

    val session = profileState.currentSession
    val visibleScreens = remember(session.role) { Screen.visibleFor(session.role) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)

    AutomaticRules(
        session = session,
        vms = vms,
        navController = navController,
        isCurrentTabAllowed = currentScreen in visibleScreens,
        currentRoute = currentRoute,
        onLoggedOut = { showNotifications = false }
    )

    if (showNotifications && session.isLoggedIn) {
        NotificationsDialog(
            state = notificationsState,
            onMarkRead = vms.notifications::markRead,
            onMarkAllRead = vms.notifications::markAllRead,
            onDismiss = { showNotifications = false }
        )
    }

    LanguageProvider(languageTag = profileState.selectedLanguage) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val windowSize = WindowSize(
                widthClass = Breakpoints.widthClassFor(maxWidth),
                isShort = maxHeight < Breakpoints.SHORT_HEIGHT
            )
            CompositionLocalProvider(LocalWindowSize provides windowSize) {
                if (!session.isLoggedIn) {
                    LoginScaffold(vms.auth, snackbarHostState)
                } else {
                    val cartState by vms.cart.uiState.collectAsStateWithLifecycle()
                    val menu = MenuState(
                        screens = visibleScreens,
                        current = currentScreen,
                        cartItemCount = cartState.items.size,
                        unreadNotifications = notificationsState.unreadCount,
                        onOpenNotifications = {
                            vms.notifications.refresh()
                            showNotifications = true
                        },
                        onSelect = { navigateToTab(navController, it, currentRoute) }
                    )
                    val tabsContent: @Composable (Modifier) -> Unit = { contentModifier ->
                        TabsContent(navController, vms, snackbarHostState, contentModifier)
                    }
                    when (navigationStyleFor(windowSize)) {
                        NavigationStyle.BOTTOM_BAR -> BottomBarLayout(menu, snackbarHostState, tabsContent)
                        NavigationStyle.SIDE_RAIL -> SideRailLayout(menu, currentScreen, snackbarHostState, tabsContent)
                        NavigationStyle.SIDE_DRAWER -> SideDrawerLayout(menu, session, snackbarHostState, tabsContent)
                    }
                }
            }
        }
    }
}

/** Los ViewModels de la app (uno por pestaña, más acceso y notificaciones). */
private class AppViewModels(
    val catalog: CatalogViewModel,
    val cart: CartViewModel,
    val seller: SellerViewModel,
    val admin: AdminViewModel,
    val profile: ProfileViewModel,
    val auth: AuthViewModel,
    val notifications: NotificationsViewModel
)

@Composable
private fun rememberAppViewModels(factory: ViewModelFactory) = AppViewModels(
    catalog = viewModel(factory = factory),
    cart = viewModel(factory = factory),
    seller = viewModel(factory = factory),
    admin = viewModel(factory = factory),
    profile = viewModel(factory = factory),
    auth = viewModel(factory = factory),
    notifications = viewModel(factory = factory)
)

/**
 * Cosas que pasan solas según la sesión:
 *  1. Si la pestaña actual ya no está permitida para el rol, se vuelve al catálogo.
 *  2. Al cerrar sesión se olvidan las notificaciones.
 *  3. Al iniciar sesión (o cambiar de usuario o rol) se descargan los datos del servidor.
 */
@Composable
private fun AutomaticRules(
    session: UserSession,
    vms: AppViewModels,
    navController: NavController,
    isCurrentTabAllowed: Boolean,
    currentRoute: String?,
    onLoggedOut: () -> Unit
) {
    LaunchedEffect(session.role, currentRoute) {
        if (!isCurrentTabAllowed) {
            navController.navigate(Screen.CATALOG.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(session.isLoggedIn) {
        if (!session.isLoggedIn) {
            vms.notifications.clear()
            onLoggedOut()
        }
    }

    LaunchedEffect(session.username, session.role) {
        if (!session.isLoggedIn) return@LaunchedEffect
        vms.notifications.refresh()
        vms.catalog.refreshFromServer()
        vms.cart.refreshOrdersFromServer()
        if (session.role == UserRole.ADMIN) {
            vms.admin.refreshAll()
        }
    }
}

/** Pantalla de acceso (todavía sin sesión), con su barra superior. */
@Composable
private fun LoginScaffold(authViewModel: AuthViewModel, snackbarHostState: SnackbarHostState) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
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
}

/** El contenido de las pestañas: el estado de cada pantalla y la navegación entre ellas. */
@Composable
private fun TabsContent(
    navController: NavController,
    vms: AppViewModels,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier
) {
    val catalogState by vms.catalog.uiState.collectAsStateWithLifecycle()
    val cartState by vms.cart.uiState.collectAsStateWithLifecycle()
    val sellerState by vms.seller.uiState.collectAsStateWithLifecycle()
    val adminState by vms.admin.uiState.collectAsStateWithLifecycle()
    val profileState by vms.profile.uiState.collectAsStateWithLifecycle()
    ZocoNavHost(
        navController = navController as NavHostController,
        catalogViewModel = vms.catalog,
        catalogState = catalogState,
        cartViewModel = vms.cart,
        cartState = cartState,
        sellerViewModel = vms.seller,
        sellerState = sellerState,
        adminViewModel = vms.admin,
        adminState = adminState,
        profileViewModel = vms.profile,
        profileState = profileState,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
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
