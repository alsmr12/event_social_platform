package com.ark.socialevent.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Запрос на регистрацию пользователя
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val age: Int,
    val phone: String
)

// Запрос на логин
data class LoginRequest(
    val email: String,
    val password: String
)

// Ответ от сервера
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

// Данные профиля (для /api/profile)
data class ProfileResponse(
    val id: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val age: Int,
    val phone: String
)

interface ApiService {

    @POST("/api/create-profile")
    fun register(@Body request: RegisterRequest): Call<ApiResponse<Unit>>

    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<Unit>>

    @GET("/api/profile")
    fun getProfile(): Call<ApiResponse<ProfileResponse>>
}
