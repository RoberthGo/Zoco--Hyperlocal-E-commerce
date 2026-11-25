package com.example.zoco

import android.app.Application
import com.example.zoco.data.local.ZocoDatabase
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.repository.OrderRepositoryImpl
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.data.repository.UserProfileRepositoryImpl
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import com.example.zoco.domain.repository.UserProfileRepository

class ZocoApplication : Application() {

    val database: ZocoDatabase by lazy {
        ZocoDatabase.getInstance(this)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao(),
            mediaDao = database.mediaDao()
        )
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(
            orderDao = database.orderDao()
        )
    }

    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepositoryImpl(
            userProfileDao = database.userProfileDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
