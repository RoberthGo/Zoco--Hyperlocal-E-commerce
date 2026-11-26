package com.example.zoco

import android.app.Application
import com.example.zoco.data.local.ZocoDatabase
import com.example.zoco.data.remote.AndroidNetworkConnectivityChecker
import com.example.zoco.data.remote.NetworkConnectivityChecker
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.repository.OrderRepositoryImpl
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.data.repository.UserProfileRepositoryImpl
import com.example.zoco.data.sync.WorkManagerSyncScheduler
import com.example.zoco.domain.sync.SyncScheduler
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import com.example.zoco.domain.repository.UserProfileRepository

class ZocoApplication : Application() {

    val database: ZocoDatabase by lazy {
        ZocoDatabase.getInstance(this)
    }

    val syncScheduler: SyncScheduler by lazy {
        WorkManagerSyncScheduler(this)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao(),
            mediaDao = database.mediaDao(),
            syncScheduler = syncScheduler
        )
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(
            orderDao = database.orderDao(),
            syncScheduler = syncScheduler
        )
    }

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
        // Al abrir la app, encolar un intento para subir lo que quedó pendiente en sesiones anteriores.
        // Si no hay nada pendiente el worker termina al instante.
        syncScheduler.scheduleSync()
    }
}
