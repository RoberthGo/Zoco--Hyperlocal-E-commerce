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

// Menús de navegación de la app (barra inferior, columna de íconos y menú lateral) y su barra superior.

/** Qué menú de navegación se usa en cada tamaño de ventana. */
internal enum class NavigationStyle {
    /** Celular en vertical: barra abajo. */
    BOTTOM_BAR,

    /** Plegable, tablet pequeña o celular en horizontal: columna de íconos a la izquierda. */
    SIDE_RAIL,

    /** Tablet grande o escritorio: menú lateral con íconos y nombres. */
    SIDE_DRAWER
}

internal fun navigationStyleFor(size: WindowSize): NavigationStyle = when {
    size.isCompact -> NavigationStyle.BOTTOM_BAR
    size.widthClass == WidthClass.MEDIUM || size.isShort -> NavigationStyle.SIDE_RAIL
    else -> NavigationStyle.SIDE_DRAWER
}

/** Todo lo que necesita un menú de navegación para dibujarse. */
internal class MenuState(
    val screens: List<Screen>,
    val current: Screen,
    val cartItemCount: Int,
    val unreadNotifications: Int,
    val onOpenNotifications: () -> Unit,
    val onSelect: (Screen) -> Unit
)

/** Celular: contenido arriba y barra de pestañas abajo. */
@Composable
internal fun BottomBarLayout(
    menu: MenuState,
    snackbarHostState: SnackbarHostState,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ZocoTopBar(stringResource(R.string.app_name), menu.unreadNotifications, menu.onOpenNotifications) },
        bottomBar = {
            NavigationBar {
                menu.screens.forEach { screen ->
                    NavigationBarItem(
                        selected = menu.current == screen,
                        onClick = { menu.onSelect(screen) },
                        icon = { TabIcon(screen, menu.cartItemCount) },
                        label = { Text(stringResource(screen.titleResId)) }
                    )
                }
            }
        }
    ) { innerPadding -> content(Modifier.padding(innerPadding)) }
}

/** Plegable o tablet pequeña: columna de íconos a la izquierda y contenido a la derecha. */
@Composable
internal fun SideRailLayout(
    menu: MenuState,
    currentScreen: Screen,
    snackbarHostState: SnackbarHostState,
    content: @Composable (Modifier) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            menu.screens.forEach { screen ->
                NavigationRailItem(
                    selected = menu.current == screen,
                    onClick = { menu.onSelect(screen) },
                    icon = { TabIcon(screen, menu.cartItemCount) },
                    label = { Text(stringResource(screen.titleResId)) }
                )
            }
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ZocoTopBar(
                    title = stringResource(R.string.app_name) + " - " + stringResource(currentScreen.titleResId),
                    unreadNotifications = menu.unreadNotifications,
                    onOpenNotifications = menu.onOpenNotifications
                )
            }
        ) { innerPadding -> content(Modifier.padding(innerPadding)) }
    }
}

/** Tablet grande: menú lateral fijo con el nombre de la persona y su rol, y el contenido al lado. */
@Composable
internal fun SideDrawerLayout(
    menu: MenuState,
    session: UserSession,
    snackbarHostState: SnackbarHostState,
    content: @Composable (Modifier) -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(modifier = Modifier.width(DRAWER_WIDTH)) {
                DrawerHeader(session)
                menu.screens.forEach { screen ->
                    NavigationDrawerItem(
                        selected = menu.current == screen,
                        onClick = { menu.onSelect(screen) },
                        icon = { TabIcon(screen, menu.cartItemCount) },
                        label = { Text(stringResource(screen.titleResId)) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { ZocoTopBar(stringResource(menu.current.titleResId), menu.unreadNotifications, menu.onOpenNotifications) }
        ) { innerPadding -> content(Modifier.padding(innerPadding)) }
    }
}

/** Parte de arriba del menú lateral: nombre de la app, de la persona y su rol. */
@Composable
private fun DrawerHeader(session: UserSession) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = session.fullName.ifBlank { session.username },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
        Text(
            text = stringResource(
                when (session.role) {
                    UserRole.BUYER -> R.string.role_buyer
                    UserRole.SELLER -> R.string.role_seller
                    UserRole.ADMIN -> R.string.role_admin
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val DRAWER_WIDTH = 280.dp

/** Barra superior con el color principal de la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ZocoTopBar(title: String, unreadNotifications: Int = -1, onOpenNotifications: () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        actions = {
            // La campana solo aparece con sesión iniciada (unreadNotifications >= 0)
            if (unreadNotifications >= 0) {
                IconButton(onClick = onOpenNotifications) {
                    BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(unreadNotifications.toString()) } }) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.notifications_title))
                    }
                }
            }
        },
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
internal fun navigateToTab(navController: NavController, screen: Screen, currentRoute: String?) {
    if (currentRoute == screen.route) return
    navController.navigate(screen.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
