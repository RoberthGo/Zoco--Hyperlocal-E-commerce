package com.example.zoco.data.remote.api

import com.example.zoco.data.remote.dto.TokenResponseDto
import com.example.zoco.data.remote.dto.ChangePasswordRequestDto
import com.example.zoco.data.remote.dto.AuthResponseDto
import com.example.zoco.data.remote.dto.AvatarResponseDto
import com.example.zoco.data.remote.dto.BecomeSellerRequestDto
import com.example.zoco.data.remote.dto.BecomeSellerResponseDto
import com.example.zoco.data.remote.dto.LoginRequestDto
import com.example.zoco.data.remote.dto.RegisterRequestDto
import com.example.zoco.data.remote.dto.RoleUpdateRequestDto
import com.example.zoco.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @POST("auth/logout/")
    suspend fun logout(): Response<Unit>

    @POST("auth/change-password/")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): TokenResponseDto

    @Multipart
    @POST("auth/me/avatar/")
    suspend fun uploadAvatar(@Part image: MultipartBody.Part): AvatarResponseDto

    @POST("auth/become-seller/")
    suspend fun becomeSeller(@Body request: BecomeSellerRequestDto): BecomeSellerResponseDto

    @GET("users")
    suspend fun getUsers(): List<UserDto>

    @POST("users/{id}/role/")
    suspend fun updateUserRole(
        @Path("id") userId: Long,
        @Body request: RoleUpdateRequestDto
    ): UserDto
}
