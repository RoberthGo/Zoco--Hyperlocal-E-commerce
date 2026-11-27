package com.example.zoco

import com.example.zoco.domain.repository.NotificationRepository
import com.example.zoco.data.repository.NotificationRepositoryImpl
import android.app.Application
import android.content.Context
import com.example.zoco.data.local.ImageStore
import com.example.zoco.data.local.SessionManager
import com.example.zoco.data.local.ZocoDatabase
import com.example.zoco.data.remote.AndroidNetworkConnectivityChecker
import com.example.zoco.data.remote.NetworkConnectivityChecker
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.remote.ServerDiscovery
import com.example.zoco.data.repository.AuthRepositoryImpl
import com.example.zoco.data.repository.OrderRepositoryImpl
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.data.repository.UserProfileRepositoryImpl
import com.example.zoco.data.sync.WorkManagerSyncScheduler
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import com.example.zoco.domain.repository.UserProfileRepository
import com.example.zoco.domain.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Se crea una sola vez cuando arranca la app y guarda las piezas compartidas (base de datos,
 * sesión, repositorios, detección del servidor). Las pantallas las obtienen desde aquí a través de
 * [MainActivity]. Cada pieza es `by lazy`: se construye la primera vez que alguien la pide.
 */
class ZocoApplication : Application() {

    val database: ZocoDatabase by lazy {
        ZocoDatabase.getInstance(this)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(getSharedPreferences("zoco_auth_prefs", Context.MODE_PRIVATE))
    }

    val serverDiscovery: ServerDiscovery by lazy {
        ServerDiscovery(this, getSharedPreferences("zoco_prefs", Context.MODE_PRIVATE))
    }

    val imageStore: ImageStore by lazy { ImageStore(this) }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(sessionManager = sessionManager, imageStore = imageStore)
    }

    val syncScheduler: SyncScheduler by lazy {
        WorkManagerSyncScheduler(this)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao(),
            mediaDao = database.mediaDao(),
            syncScheduler = syncScheduler,
            imageStore = imageStore
        )
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(
            orderDao = database.orderDao(),
            syncScheduler = syncScheduler
        )
    }

    val notificationRepository: NotificationRepository by lazy { NotificationRepositoryImpl() }

    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepositoryImpl(
            userProfileDao = database.userProfileDao()
        )
    }

    val networkConnectivityChecker: NetworkConnectivityChecker by lazy {
        AndroidNetworkConnectivityChecker(this)
    }

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.onUnauthorized = { sessionManager.clearSession() }
        // Backend: recuerda la última URL y busca sola una que responda (emulador, USB o hotspot).
        serverDiscovery.restoreSavedUrl()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { serverDiscovery.findServer() }
        // Al abrir la app, encolar un intento para subir lo que quedó pendiente en sesiones anteriores.
        // Si no hay nada pendiente el worker termina al instante.
        syncScheduler.scheduleSync()
    }
}
