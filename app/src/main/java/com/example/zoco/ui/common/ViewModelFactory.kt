package com.example.zoco.ui.common

import com.example.zoco.ui.notifications.NotificationsViewModel
import com.example.zoco.domain.repository.NotificationRepository
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zoco.data.remote.NetworkConnectivityChecker
import com.example.zoco.domain.repository.AuthRepository
import com.example.zoco.domain.repository.OrderRepository
import com.example.zoco.domain.repository.ProductRepository
import com.example.zoco.domain.repository.UserProfileRepository
import com.example.zoco.ui.admin.AdminViewModel
import com.example.zoco.ui.auth.AuthViewModel
import com.example.zoco.ui.cart.CartViewModel
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.profile.ProfileViewModel
import com.example.zoco.ui.seller.SellerViewModel

/**
 * Fábrica que crea los ViewModels de la app y les entrega lo que necesitan (repositorios, etc.).
 * Android no sabe crear un ViewModel con parámetros por sí solo, por eso existe esta clase.
 *
 * Casi todos los parámetros son opcionales (`= null`) para poder crear ViewModels de prueba sin
 * base de datos ni servidor. En la app real siempre se entregan todos (ver [MainActivity]).
 */
class ViewModelFactory(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val userProfileRepository: UserProfileRepository? = null,
    private val networkConnectivityChecker: NetworkConnectivityChecker? = null,
    private val sharedPreferences: SharedPreferences? = null,
    private val authRepository: AuthRepository? = null,
    private val findServer: (suspend () -> Boolean)? = null,
    private val notificationRepository: NotificationRepository? = null
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
                ProfileViewModel(userProfileRepository, sharedPreferences, authRepository, orderRepository) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                val auth = authRepository
                    ?: throw IllegalStateException("AuthRepository requerido para AuthViewModel")
                AuthViewModel(auth, findServer) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                val auth = authRepository
                    ?: throw IllegalStateException("AuthRepository requerido para AdminViewModel")
                AdminViewModel(auth, productRepository, orderRepository) as T
            }
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> {
                NotificationsViewModel(notificationRepository) as T
            }
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
    }
}
