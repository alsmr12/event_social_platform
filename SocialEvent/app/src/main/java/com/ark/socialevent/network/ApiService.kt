package com.ark.socialevent.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Запрос на регистрацию
data class RegisterRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    val password: String,
    val gender: String,
    val age: Int,
    val phone: String
)

// Запрос на логин
data class LoginRequest(
    val email: String,
    val password: String
)

// Ответ от сервера при регистрации
data class RegisterResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserProfile? = null
)

// Ответ от сервера при логине
data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserProfile? = null,
    val token: String? = null
)

// Профиль пользователя
data class UserProfile(
    val id: Int,
    val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val gender: String,
    val age: Int,
    val phone: String
)

// Ответ для получения профиля
data class ProfileResponse(
    val user: UserProfile,
    val posts: List<Any>? = null,
    @SerializedName("social_links") val socialLinks: List<Any>? = null,
    val followers: Int,
    val following: Int,
    @SerializedName("friends_count") val friendsCount: Int
)

interface ApiService {
    @POST("/api/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("/api/profile")
    fun getProfile(): Call<ProfileResponse>
}