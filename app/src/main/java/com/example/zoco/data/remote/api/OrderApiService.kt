package com.example.zoco.data.remote.api

import com.example.zoco.data.remote.dto.OrderDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OrderApiService {

    @GET("orders")
    suspend fun getOrders(): List<OrderDto>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: Long): OrderDto

    @POST("orders")
    suspend fun createOrder(@Body order: OrderDto): OrderDto

    @POST("orders/{id}/status/")
    suspend fun updateOrderStatus(
        @Path("id") id: Long,
        @Body statusPayload: Map<String, String>
    ): OrderDto

    @DELETE("orders/{id}")
    suspend fun cancelOrder(@Path("id") id: Long): Response<Unit>
}
