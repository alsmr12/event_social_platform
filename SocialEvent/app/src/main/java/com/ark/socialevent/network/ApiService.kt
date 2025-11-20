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
    val user: UserProfile,      // Отправитель заявки
    val friend: UserProfile,    // Получатель заявки
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    val isIncoming: Boolean = false  // Добавьте это поле
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

// Запрос на обновление профиля
data class UpdateProfileRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val gender: String,
    val age: Int,
    val phone: String
)

// Ответ при обновлении профиля
data class UpdateProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserProfile? = null
)

// Модель статистики
data class UserStats(
    @SerializedName("friends_count") val friendsCount: Int,
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("following_count") val followingCount: Int,
    @SerializedName("events_count") val eventsCount: Int
)

// Ответ для статистики
data class UserStatsResponse(
    val success: Boolean,
    val stats: UserStats? = null,
    val message: String? = null
)

data class WallPost(
    val id: Int,
    val content: String,
    @SerializedName("author_id") val authorId: Int,
    @SerializedName("user_id") val userId: Int,
    val author: UserProfile,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)


data class UpdateWallPostRequest(
    val content: String
)

data class WallPostsResponse(
    val success: Boolean,
    val posts: List<WallPost>? = null,
    val message: String? = null
)

data class WallPostResponse(
    val success: Boolean,
    val post: WallPost? = null,
    val message: String? = null
)
data class OperationResponse(
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
    @PUT("/api/profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<UpdateProfileResponse>

    @GET("/api/profile/stats")
    fun getUserStats(): Call<UserStatsResponse>

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
    @POST("/api/friends/cancel/{id}")
    fun cancelFriendRequest(@Path("id") userId: Int): Call<FriendOperationResponse>


    @GET("/api/wall/posts/{user_id}")
    fun getUserWallPosts(@Path("user_id") userId: Int): Call<WallPostsResponse>

    @FormUrlEncoded
    @POST("/api/wall/posts")
    fun createWallPost(
        @Field("content") content: String,
        @Field("user_id") userId: Int
    ): Call<WallPostResponse>


    @PUT("/api/wall/posts/{id}")
    fun updateWallPost(@Path("id") postId: Int, @Body request: UpdateWallPostRequest): Call<WallPostResponse>

    @DELETE("/api/wall/posts/{id}")
    fun deleteWallPost(@Path("id") postId: Int): Call<OperationResponse>

}