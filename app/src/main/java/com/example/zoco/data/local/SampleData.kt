package com.example.zoco.data.local

import com.example.zoco.domain.model.ProductCategory

/**
 * Datos de ejemplo que se cargan solo la primera vez que se abre la app (cuando la base local está vacía).
 * Sirven para que el catálogo no se vea vacío mientras no hay conexión con el servidor.
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

    /**
     * Foto de cada producto de ejemplo (id del producto -> dirección de la imagen).
     * Las imágenes están dentro de la app (carpeta res/drawable-nodpi), así que se ven sin internet.
     * Autores y licencias: docs/CREDITOS_FOTOS.md.
     */
    val productPhotos: Map<Long, String> = mapOf(
        101L to photo("sample_pan"),
        102L to photo("sample_miel"),
        103L to photo("sample_queso"),
        104L to photo("sample_canasta"),
        105L to photo("sample_naranjas")
    )

    private fun photo(drawableName: String) = "android.resource://com.example.zoco/drawable/$drawableName"
}
