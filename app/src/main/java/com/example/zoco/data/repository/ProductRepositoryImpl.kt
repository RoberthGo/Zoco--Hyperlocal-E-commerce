package com.example.zoco.data.repository

import com.example.zoco.data.local.ProductDao
import com.example.zoco.data.local.ProductEntity
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import com.example.zoco.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProductsFiltered(
        query: String,
        category: ProductCategory,
        expressOnly: Boolean
    ): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }.filter { product ->
                val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) || product.description.contains(query, ignoreCase = true)
                val matchesCategory = category == ProductCategory.ALL || product.category == category
                val matchesExpress = !expressOnly || product.isExpressDelivery
                matchesQuery && matchesCategory && matchesExpress
            }
        }
    }

    override suspend fun insertProduct(product: Product): Long {
        return productDao.insertProduct(ProductEntity.fromDomain(product))
    }

    override suspend fun populateInitialDataIfEmpty() {
        if (productDao.getProductCount() == 0) {
            val initialProducts = listOf(
                ProductEntity(
                    name = "Pan Artesanal de Masa Madre",
                    description = "Elaborado localmente con fermentación lenta de 24 horas.",
                    price = 45.0,
                    category = ProductCategory.BAKERY.name,
                    producerName = "Panadería Don Pedro",
                    isExpressDelivery = true,
                    inStock = true
                ),
                ProductEntity(
                    name = "Miel de Abeja Pura 500g",
                    description = "Cosechada de apiarios sustentables de la región.",
                    price = 110.0,
                    category = ProductCategory.PRODUCE.name,
                    producerName = "Apícola del Valle",
                    isExpressDelivery = true,
                    inStock = true
                ),
                ProductEntity(
                    name = "Queso Fresco de Rancho",
                    description = "100% leche de vaca natural sin conservadores.",
                    price = 85.0,
                    category = ProductCategory.DAIRY.name,
                    producerName = "Lácteos Santa Rosa",
                    isExpressDelivery = false,
                    inStock = true
                ),
                ProductEntity(
                    name = "Canasta Tejida a Mano",
                    description = "Hecha de palma natural por artesanos de la comunidad.",
                    price = 180.0,
                    category = ProductCategory.CRAFTS.name,
                    producerName = "Manos Creativas",
                    isExpressDelivery = false,
                    inStock = true
                ),
                ProductEntity(
                    name = "Manzanas Orgánicas (1 kg)",
                    description = "Frescas, sin pesticidas, recién cortadas del huerto.",
                    price = 40.0,
                    category = ProductCategory.PRODUCE.name,
                    producerName = "Huerta Los Nogales",
                    isExpressDelivery = true,
                    inStock = true
                )
            )
            productDao.insertProducts(initialProducts)
        }
    }
}
