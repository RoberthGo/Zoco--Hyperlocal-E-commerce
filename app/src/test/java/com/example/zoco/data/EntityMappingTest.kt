package com.example.zoco.data

import com.example.zoco.data.local.MediaEntity
import com.example.zoco.data.local.ProductEntity
import com.example.zoco.data.remote.dto.ProductDto
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.ProductCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappingTest {

    @Test
    fun productEntity_toDomain_preservesAllFieldsIncludingMedia() {
        val entity = ProductEntity(
            id = 5,
            name = "Queso Oaxaca",
            description = "Queso de hebra tradicional",
            price = 85.0,
            category = "DAIRY",
            producerName = "Lácteos del Sur",
            isExpressDelivery = true,
            inStock = true,
            isRemote = false
        )

        val domain = entity.toDomain(imageUrl = "content://media/123")
        assertEquals(5L, domain.id)
        assertEquals("Queso Oaxaca", domain.name)
        assertEquals("Queso de hebra tradicional", domain.description)
        assertEquals(85.0, domain.price, 0.001)
        assertEquals(ProductCategory.DAIRY, domain.category)
        assertEquals("Lácteos del Sur", domain.producerName)
        assertTrue(domain.isExpressDelivery)
        assertEquals("content://media/123", domain.imageUrl)
    }

    @Test
    fun productDto_roundTrip_preservesFields() {
        val product = Product(
            id = 10,
            name = "Café de Altura",
            description = "Café tostado artesanal",
            price = 150.0,
            category = ProductCategory.PRODUCE,
            producerName = "Finca La Esperanza",
            isExpressDelivery = true,
            inStock = true
        )

        val dto = ProductDto.fromDomain(product)
        val domainFromDto = dto.toDomain()

        assertEquals(product.id, domainFromDto.id)
        assertEquals(product.name, domainFromDto.name)
        assertEquals(product.price, domainFromDto.price, 0.001)
        assertEquals(product.category, domainFromDto.category)
    }

    @Test
    fun mediaEntity_toDomain_preservesFields() {
        val media = MediaEntity(
            id = 1,
            productId = 5,
            uri = "content://images/photo.jpg",
            mediaType = "IMAGE",
            description = "Foto del producto"
        )

        val domain = media.toDomain()
        assertEquals(1L, domain.id)
        assertEquals(5L, domain.productId)
        assertEquals("content://images/photo.jpg", domain.uri)
        assertEquals("IMAGE", domain.mediaType)
    }
}
