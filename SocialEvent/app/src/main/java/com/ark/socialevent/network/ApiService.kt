package com.ark.socialevent.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

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

data class ProfilesResponse(
    val success: Boolean,
    val data: List<UserProfile>? = null,
    val message: String? = null
)

// ========== FRIENDS MODELS ==========

// Модель друга
data class Friend(
    val id: Int,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    val gender: String,
    val age: Int,
    val phone: String
)

// Модель заявки в друзья
data class FriendRequest(
    val id: Int,
    val user: UserProfile,
    val friend: UserProfile,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

// Ответ для списка друзей
data class FriendsResponse(
    val success: Boolean,
    val friends: List<Friend>? = null,
    val message: String? = null
)

// Ответ для заявок в друзья
data class FriendRequestsResponse(
    val success: Boolean,
    val requests: List<FriendRequest>? = null,
    val message: String? = null
)

// Ответ для статуса дружбы
data class FriendshipStatusResponse(
    val success: Boolean,
    val status: String? = null,
    val message: String? = null
)

// Базовый ответ для операций с друзьями
data class FriendOperationResponse(
    val success: Boolean,
    val message: String? = null
)

interface ApiService {
    @POST("/api/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("/api/profile")
    fun getProfile(): Call<ProfileResponse>

    @GET("/api/profiles")
    fun getAllProfiles(): Call<ProfilesResponse>

    // ========== FRIENDS ENDPOINTS ==========

    @GET("/api/friends")
    fun getFriends(): Call<FriendsResponse>

    @GET("/api/friends/pending")
    fun getPendingRequests(): Call<FriendRequestsResponse>

    @GET("/api/friends/sent")
    fun getSentRequests(): Call<FriendRequestsResponse>

    @GET("/api/friends/status/{id}")
    fun getFriendshipStatus(@Path("id") userId: Int): Call<FriendshipStatusResponse>

    @POST("/api/friends/add/{id}")
    fun sendFriendRequest(@Path("id") userId: Int): Call<FriendOperationResponse>

    @POST("/api/friends/accept/{id}")
    fun acceptFriendRequest(@Path("id") userId: Int): Call<FriendOperationResponse>

    @POST("/api/friends/reject/{id}")
    fun rejectFriendRequest(@Path("id") userId: Int): Call<FriendOperationResponse>

    @POST("/api/friends/remove/{id}")
    fun removeFriend(@Path("id") userId: Int): Call<FriendOperationResponse>
}