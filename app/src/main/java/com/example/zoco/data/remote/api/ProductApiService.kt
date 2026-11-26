package com.example.zoco.data.remote.api

import com.example.zoco.data.remote.dto.ProductDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProductApiService {

    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Long): ProductDto

    @POST("products")
    suspend fun createProduct(@Body product: ProductDto): ProductDto

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body product: ProductDto
    ): ProductDto

    @Multipart
    @POST("products/{id}/image")
    suspend fun uploadImage(
        @Path("id") id: Long,
        @Part image: MultipartBody.Part
    ): ProductDto

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): Response<Unit>
}
