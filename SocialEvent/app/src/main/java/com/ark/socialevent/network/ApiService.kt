package com.ark.socialevent.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*


data class Achievement(
    val id: Int,
    val name: String,
    val description: String,
    val icon: String,
    val points: Int,
    val type: String,
    val condition: Int,
    @SerializedName("created_at") val createdAt: String
)

// Модель достижения пользователя
data class UserAchievement(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("achievement_id") val achievementId: Int,
    val achievement: Achievement,
    val progress: Int,
    val completed: Boolean,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// Модель для рейтинга пользователей
data class UserRating(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val points: Int
)

// Ответы API
data class AchievementsResponse(
    val success: Boolean,
    val achievements: List<UserAchievement>? = null,
    val message: String? = null
)

data class RatingsResponse(
    val success: Boolean,
    val ratings: List<UserRating>? = null,
    val message: String? = null
)

data class TotalPointsResponse(
    val success: Boolean,
    val points: Int? = null,
    val message: String? = null
)

data class EventsRequest(
    @SerializedName("type") val type: String? = null,
    @SerializedName("date_from") val dateFrom: String? = null,
    @SerializedName("date_to") val dateTo: String? = null,
    @SerializedName("radius") val radius: Double? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)
data class Subscription(
    val id: Int,
    val follower: UserProfile,
    val following: UserProfile,
    @SerializedName("created_at") val createdAt: String
)

data class SubscriptionsResponse(
    val success: Boolean,
    val subscriptions: List<Subscription>? = null,
    val message: String? = null
)

data class SubscriptionStats(
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("following_count") val followingCount: Int
)

data class SubscriptionStatsResponse(
    val success: Boolean,
    val stats: SubscriptionStats? = null,
    val message: String? = null
)


// Модель для события в ленте новостей
data class NewsEvent(
    val id: Int,
    val title: String,
    val description: String,
    val type: String,
    @SerializedName("date_time") val dateTime: String,
    val location: String,
    @SerializedName("creator_id") val creatorId: Int,
    @SerializedName("is_private") val isPrivate: Boolean,
    @SerializedName("created_at") val createdAt: String
)

// Модель для элемента ленты новостей
data class NewsFeedItem(
    val id: Int,
    val type: String, // "post" или "event"
    val content: String,
    val author: UserProfile,
    @SerializedName("created_at") val createdAt: String,
    val event: NewsEvent? = null, // только для type = "event"
    val post: Any? = null // только для type = "post"
)

data class NewsFeedResponse(
    val success: Boolean,
    val posts: List<NewsFeedItem>? = null,
    val events: List<NewsFeedItem>? = null,
    val message: String? = null
)

// Модель для поста в ленте новостей
data class NewsPost(
    val id: Int,
    val content: String,
    @SerializedName("author_id") val authorId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// Запрос на регистрацию
data class RegisterRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    val password: String,
    val gender: String,
    @SerializedName("birth_date") val birthDate: String,
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
    val age: Int, // Оставляем age для отображения (будет вычисляться на сервере)
    @SerializedName("birth_date") val birthDate: String? = null, // Добавляем birth_date
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
    @SerializedName("birth_date") val birthDate: String? = null,
    val phone: String
)

// Модель заявки в друзья
data class FriendRequest(
    val id: Int,
    val user: UserProfile,      // Отправитель заявки
    val friend: UserProfile,    // Получатель заявки
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    val isIncoming: Boolean = false
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
    @SerializedName("birth_date") val birthDate: String,
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

    // Эндпоинты в ApiService interface:

    @GET("/api/profile/{id}/subscription-stats")
    fun getSubscriptionStats(@Path("id") userId: Int): Call<SubscriptionStatsResponse>


    @GET("/api/friends/subscriptions")
    fun getSubscriptions(): Call<SubscriptionsResponse>

    @GET("/api/friends/check-subscription/{id}")
    fun checkSubscription(@Path("id") userId: Int): Call<OperationResponse>

    @POST("/api/friends/subscribe/{id}")
    fun subscribe(@Path("id") userId: Int): Call<OperationResponse>

    @POST("/api/friends/unsubscribe/{id}")
    fun unsubscribe(@Path("id") userId: Int): Call<OperationResponse>

    @GET("/api/news")
    fun getNewsFeed(): Call<NewsFeedResponse>

    @GET("/api/events")
    fun getEventsWithFilters(
        @Query("type") type: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("radius") radius: Double? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("filter") timeFilter: String? = null
    ): Call<EventsResponse>

    @GET("/api/events/filtered")
    fun getEventsWithFilters(
        @Query("type") type: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("radius") radius: Double? = null,
        @Query("filter") timeFilter: String? = null
    ): Call<EventsResponse>

    @GET("/api/event/{id}")
    fun getEvent(@Path("id") eventId: Int): Call<EventResponse>

    @POST("/api/create-event")
    fun createEvent(@Body request: CreateEventRequest): Call<EventResponse>

    @POST("/api/event/{id}/subscribe")
    fun subscribeToEvent(@Path("id") eventId: Int): Call<EventSubscriptionResponse>

    @POST("/api/event/{id}/unsubscribe")
    fun unsubscribeFromEvent(@Path("id") eventId: Int): Call<EventSubscriptionResponse>

    // В интерфейс ApiService добавь:
    @GET("/api/events/feed")
    fun getEventsFeed(): Call<EventsResponse>

    @GET("/api/events/user/{user_id}")
    fun getUserEvents(@Path("user_id") userId: Int): Call<EventsResponse>

    @POST("/api/events/join-by-code")
    fun joinEventByCode(@Body request: JoinEventRequest): Call<JoinEventResponse>


    @POST("/api/events/{id}/update")
    fun updateEvent(@Path("id") eventId: Int, @Body request: CreateEventRequest): Call<EventResponse>

    @POST("/api/events/{id}/delete")
    fun deleteEvent(@Path("id") eventId: Int): Call<OperationResponse>

    @GET("/api/achievements/my")
    fun getMyAchievements(): Call<AchievementsResponse>

    // Получить рейтинг пользователей
    @GET("/api/achievements/ratings")
    fun getRatings(@Query("search") search: String? = null): Call<RatingsResponse>

    // Получить общее количество очков
    @GET("/api/achievements/total-points")
    fun getTotalPoints(): Call<TotalPointsResponse>
}
