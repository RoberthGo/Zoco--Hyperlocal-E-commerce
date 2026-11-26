package com.example.zoco.ui.common

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zoco.data.remote.NetworkConnectivityChecker
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import com.example.zoco.domain.repository.UserProfileRepository
import com.example.zoco.ui.admin.AdminViewModel
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.profile.ProfileViewModel
import com.example.zoco.ui.seller.SellerViewModel

class ViewModelFactory(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val userProfileRepository: UserProfileRepository? = null,
    private val networkConnectivityChecker: NetworkConnectivityChecker? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val authRepository: AuthRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CatalogViewModel::class.java) -> {
                CatalogViewModel(productRepository, networkConnectivityChecker, authRepository) as T
            }
            modelClass.isAssignableFrom(SellerViewModel::class.java) -> {
                SellerViewModel(productRepository, orderRepository, authRepository) as T
            }
            modelClass.isAssignableFrom(CartViewModel::class.java) -> {
                CartViewModel(orderRepository, authRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(userProfileRepository, sharedPreferences, authRepository) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                val auth = authRepository
                    ?: throw IllegalStateException("AuthRepository requerido para AdminViewModel")
                AdminViewModel(auth, productRepository, orderRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
