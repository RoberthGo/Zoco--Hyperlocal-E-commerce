package com.example.zoco.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zoco.data.repository.ProductRepositoryImpl
import com.example.zoco.ui.catalog.CatalogViewModel
import com.example.zoco.ui.seller.SellerViewModel

class ViewModelFactory(
    private val repository: ProductRepositoryImpl
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CatalogViewModel::class.java) -> {
                CatalogViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SellerViewModel::class.java) -> {
                SellerViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
