package com.example.zoco.data.local

import com.example.zoco.domain.model.ProductCategory

/**
 * Datos de ejemplo que se cargan solo la primera vez que se abre la app (cuando la base local está vacía).
 * Sirven para que el catálogo y los pedidos no se vean vacíos mientras no hay conexión con el servidor.
 * Para cambiar o quitar los ejemplos, edita únicamente este archivo.
 */
object SampleData {

    /** Productos de ejemplo del catálogo. */
    val products: List<ProductEntity> = listOf(
        ProductEntity(
            id = 101,
            name = "Pan de Pueblo Horneado a la Leña",
            description = "Elaborado localmente con fermentación tradicional en horno de leña comunitario.",
            price = 40.0,
            category = ProductCategory.BAKERY.name,
            producerName = "Panadería San José",
            isExpressDelivery = true,
            inStock = true,
            isRemote = false,
            stock = 15,
            sellerId = 2L,
            sellerUsername = "vendedor_juan"
        ),
        ProductEntity(
            id = 102,
            name = "Miel Silvestre de Monte (500g)",
            description = "Miel pura no pasteurizada, conservada con cuidado.",
            price = 95.0,
            category = ProductCategory.PRODUCE.name,
            producerName = "Apícola Los Sauces",
            isExpressDelivery = true,
            inStock = true,
            isRemote = false,
            stock = 8,
            sellerId = 2L,
            sellerUsername = "vendedor_juan"
        ),
        ProductEntity(
            id = 103,
            name = "Queso Fresco Ranchero de Hoja",
            description = "Queso artesanal envuelto en hoja de plátano, 100% natural de establo local.",
            price = 65.0,
            category = ProductCategory.DAIRY.name,
            producerName = "Rancho San Antonio",
            isExpressDelivery = false,
            inStock = true,
            isRemote = false,
            stock = 12,
            sellerId = 2L,
            sellerUsername = "vendedor_juan"
        ),
        ProductEntity(
            id = 104,
            name = "Canasta de Vara Tejida Tradicional",
            description = "Artesanía de vara de sauz tejida a mano disponible en catálogo local.",
            price = 110.0,
            category = ProductCategory.CRAFTS.name,
            producerName = "Artesanías El Roble",
            isExpressDelivery = false,
            inStock = true,
            isRemote = false,
            stock = 5,
            sellerId = 4L,
            sellerUsername = "artesana_maria"
        ),
        ProductEntity(
            id = 105,
            name = "Naranjas Dulces de Huerto (2 kg)",
            description = "Cosecha fresca local disponible sin requerir conexión a internet.",
            price = 35.0,
            category = ProductCategory.PRODUCE.name,
            producerName = "Huerta La Cañada",
            isExpressDelivery = true,
            inStock = true,
            isRemote = false,
            stock = 20,
            sellerId = 4L,
            sellerUsername = "artesana_maria"
        )
    )

    /** Pedidos de ejemplo. Es una función (no una lista fija) porque las fechas se calculan al momento de usarla. */
    fun orders(): List<OrderEntity> {
        return listOf(
            OrderEntity(
                id = 101,
                customerName = "Juan Comprador",
                customerPhone = "4451122334",
                deliveryAddress = "Calle Morelos #15, Centro Local",
                scheduledTime = "15:00",
                totalAmount = 80.0,
                status = "LOCAL_STORED",
                isExpress = false,
                itemSummary = "2x Pan de Pueblo Horneado a la Leña",
                createdAt = System.currentTimeMillis() - TWO_HOURS_MS,
                isRemote = false
            ),
            OrderEntity(
                id = 102,
                customerName = "María Local",
                customerPhone = "4459988776",
                deliveryAddress = "Barrio San Juan #4, Localidad",
                scheduledTime = "17:30",
                totalAmount = 95.0,
                status = "LOCAL_STORED",
                isExpress = true,
                itemSummary = "1x Miel Silvestre de Monte (500g)",
                createdAt = System.currentTimeMillis() - ONE_HOUR_MS,
                isRemote = false
            )
        )
    }

    private const val ONE_HOUR_MS = 3_600_000L
    private const val TWO_HOURS_MS = 7_200_000L
}
