package com.example.zoco

import android.app.Application
import com.example.zoco.data.local.ZocoDatabase
import com.example.zoco.data.remote.RetrofitClient
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.domain.repository.ProductRepository

class ZocoApplication : Application() {

    val database: ZocoDatabase by lazy {
        ZocoDatabase.getInstance(this)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao(),
            productApiService = RetrofitClient.productApiService
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
