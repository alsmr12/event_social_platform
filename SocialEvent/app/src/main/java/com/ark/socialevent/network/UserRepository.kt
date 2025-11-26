package com.ark.socialevent.network

import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRepository(private val context: Context) {
    private val api: ApiService by lazy {
        // Инициализируем ApiClient и получаем apiService
        ApiClient.initialize(context)
        ApiClient.getApiService()
    }

    init {
        Log.d("UserRepository", "UserRepository initialized with context")
    }

    // Регистрация пользователя
    fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: String,
        age: Int,
        phone: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val request = RegisterRequest(firstName, lastName, email, password, gender, age, phone)
        api.register(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Register successful")
                        callback(true, body.message ?: "Регистрация успешна")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка регистрации"
                        Log.e("UserRepository", "Register failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        409 -> "Пользователь с таким email уже существует"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    Log.e("UserRepository", "Register HTTP error: ${response.code()}")
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("UserRepository", "Register network failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Логин пользователя - ОБНОВЛЕННЫЙ!
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepository", "=== LOGIN START ===")
        Log.d("UserRepository", "Email: $email")

        val request = LoginRequest(email, password)
        api.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                Log.d("UserRepository", "=== LOGIN RESPONSE ===")
                Log.d("UserRepository", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "✅ Login successful")

                        // ВАЖНО: Сохраняем токен и сразу проверяем
                        body.token?.let { token ->
                            Log.d("UserRepository", "💾 Saving token: $token")
                            ApiClient.saveSessionToken(token)

                            // СРАЗУ ПРОВЕРИМ что сохранилось
                            val savedToken = ApiClient.getSessionToken()
                            if (savedToken == token) {
                                Log.d("UserRepository", "✅ Token saved correctly: $savedToken")
                            } else {
                                Log.e("UserRepository", "❌ Token save FAILED! Saved: $savedToken, Expected: $token")
                            }
                        } ?: run {
                            Log.e("UserRepository", "❌ No token in login response!")
                        }

                        callback(true, body.message ?: "Вход выполнен")
                    } else {
                        val errorMsg = body?.message ?: "Неверный email или пароль"
                        Log.e("UserRepository", "❌ Login failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        401 -> "Неверный email или пароль"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    Log.e("UserRepository", "❌ Login HTTP error: ${response.code()}")
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Login network failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Получение профиля текущего пользователя
    fun getProfile(callback: (UserProfile?) -> Unit) {
        Log.d("UserRepository", "=== GET PROFILE START ===")

        // Проверим токен перед запросом
        val token = ApiClient.getSessionToken()
        Log.d("UserRepository", "🔑 Current session token: $token")

        if (token == null) {
            Log.e("UserRepository", "❌ No token available for profile request!")
            callback(null)
            return
        }

        Log.d("UserRepository", "🚀 Making profile request with token...")
        api.getProfile().enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                Log.d("UserRepository", "=== GET PROFILE RESPONSE ===")
                Log.d("UserRepository", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("UserRepository", "✅ Get profile SUCCESS")
                        Log.d("UserRepository", "User: ${body.user.firstName} ${body.user.lastName}")
                        callback(body.user)
                    } else {
                        Log.e("UserRepository", "❌ Get profile: empty body")
                        callback(null)
                    }
                } else {
                    Log.e("UserRepository", "❌ Get profile HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }
                    callback(null)
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Get profile NETWORK failed: ${t.message}")
                callback(null)
            }
        })
    }

    fun getAllProfiles(callback: (List<UserProfile>?, String?) -> Unit) {
        api.getAllProfiles().enqueue(object : Callback<ProfilesResponse> {
            override fun onResponse(
                call: Call<ProfilesResponse>,
                response: Response<ProfilesResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Loaded ${body.data?.size ?: 0} users")
                        callback(body.data ?: emptyList(), null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(null, errorMsg)
                    }
                } else {
                    val errorMsg = "Ошибка сервера: ${response.code()}"
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<ProfilesResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // ========== FRIENDS METHODS ==========

    // Получить список друзей
    fun getFriends(callback: (List<Friend>?, String?) -> Unit) {
        api.getFriends().enqueue(object : Callback<FriendsResponse> {
            override fun onResponse(call: Call<FriendsResponse>, response: Response<FriendsResponse>) {
                Log.d("UserRepository", "getFriends response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Loaded ${body.friends?.size ?: 0} friends")
                        callback(body.friends ?: emptyList(), null)
                    } else {
                        try {
                            val errorBody = response.errorBody()?.string()
                            Log.e("UserRepository", "Error body: $errorBody")
                        } catch (e: Exception) {
                            Log.e("UserRepository", "Error reading error body: ${e.message}")
                        }

                        val errorMsg = body?.message ?: "Неизвестная ошибка сервера"
                        callback(null, errorMsg)
                    }
                } else {
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "HTTP ${response.code()} error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }

                    val errorMsg = when (response.code()) {
                        401 -> "Не авторизован. Войдите заново."
                        404 -> "Функция друзей недоступна"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<FriendsResponse>, t: Throwable) {
                Log.e("UserRepository", "Network failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Получить входящие заявки в друзья
    fun getPendingRequests(callback: (List<FriendRequest>?, String?) -> Unit) {
        api.getPendingRequests().enqueue(object : Callback<FriendRequestsResponse> {
            override fun onResponse(call: Call<FriendRequestsResponse>, response: Response<FriendRequestsResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Loaded ${body.requests?.size ?: 0} pending requests")
                        callback(body.requests ?: emptyList(), null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(null, errorMsg)
                    }
                } else {
                    val errorMsg = "Ошибка сервера: ${response.code()}"
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<FriendRequestsResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Получить отправленные заявки
    fun getSentRequests(callback: (List<FriendRequest>?, String?) -> Unit) {
        api.getSentRequests().enqueue(object : Callback<FriendRequestsResponse> {
            override fun onResponse(call: Call<FriendRequestsResponse>, response: Response<FriendRequestsResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Loaded ${body.requests?.size ?: 0} sent requests")
                        callback(body.requests ?: emptyList(), null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(null, errorMsg)
                    }
                } else {
                    val errorMsg = "Ошибка сервера: ${response.code()}"
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<FriendRequestsResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Отправить заявку в друзья
    fun sendFriendRequest(userId: Int, callback: (Boolean, String?) -> Unit) {
        api.sendFriendRequest(userId).enqueue(object : Callback<FriendOperationResponse> {
            override fun onResponse(call: Call<FriendOperationResponse>, response: Response<FriendOperationResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Friend request sent to user $userId")
                        callback(true, body.message ?: "Запрос отправлен")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Невозможно отправить запрос"
                        404 -> "Пользователь не найден"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<FriendOperationResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Принять заявку в друзья
    fun acceptFriendRequest(userId: Int, callback: (Boolean, String?) -> Unit) {
        api.acceptFriendRequest(userId).enqueue(object : Callback<FriendOperationResponse> {
            override fun onResponse(call: Call<FriendOperationResponse>, response: Response<FriendOperationResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Friend request accepted from user $userId")
                        callback(true, body.message ?: "Запрос принят")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    callback(false, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<FriendOperationResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Отклонить заявку в друзья
    fun rejectFriendRequest(userId: Int, callback: (Boolean, String?) -> Unit) {
        api.rejectFriendRequest(userId).enqueue(object : Callback<FriendOperationResponse> {
            override fun onResponse(call: Call<FriendOperationResponse>, response: Response<FriendOperationResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Friend request rejected from user $userId")
                        callback(true, body.message ?: "Запрос отклонен")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    callback(false, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<FriendOperationResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Удалить из друзей
    fun removeFriend(userId: Int, callback: (Boolean, String?) -> Unit) {
        api.removeFriend(userId).enqueue(object : Callback<FriendOperationResponse> {
            override fun onResponse(call: Call<FriendOperationResponse>, response: Response<FriendOperationResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Friend removed: user $userId")
                        callback(true, body.message ?: "Пользователь удален из друзей")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    callback(false, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<FriendOperationResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Получить статус дружбы с пользователем
    fun getFriendshipStatus(userId: Int, callback: (String?, String?) -> Unit) {
        api.getFriendshipStatus(userId).enqueue(object : Callback<FriendshipStatusResponse> {
            override fun onResponse(call: Call<FriendshipStatusResponse>, response: Response<FriendshipStatusResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        callback(body.status, null)
                    } else {
                        callback(null, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(null, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<FriendshipStatusResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Обновление профиля
    fun updateProfile(
        firstName: String,
        lastName: String,
        gender: String,
        age: Int,
        phone: String,
        callback: (Boolean, String?, UserProfile?) -> Unit
    ) {
        Log.d("UserRepository", "=== UPDATE PROFILE START ===")
        Log.d("UserRepository", "Data: $firstName $lastName, $gender, $age, $phone")

        // Проверим токен перед запросом
        val token = ApiClient.getSessionToken()
        Log.d("UserRepository", "🔑 Current session token: $token")

        if (token == null) {
            Log.e("UserRepository", "❌ No token available for update request!")
            callback(false, "Не авторизован", null)
            return
        }

        val request = UpdateProfileRequest(firstName, lastName, gender, age, phone)
        Log.d("UserRepository", "🚀 Making update profile request...")

        api.updateProfile(request).enqueue(object : Callback<UpdateProfileResponse> {
            override fun onResponse(
                call: Call<UpdateProfileResponse>,
                response: Response<UpdateProfileResponse>
            ) {
                Log.d("UserRepository", "=== UPDATE PROFILE RESPONSE ===")
                Log.d("UserRepository", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "✅ Profile updated successfully")
                        Log.d("UserRepository", "Updated user: ${body.user?.firstName} ${body.user?.lastName}")
                        callback(true, body.message, body.user)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("UserRepository", "❌ Update profile failed: $errorMsg")
                        callback(false, errorMsg, null)
                    }
                } else {
                    Log.e("UserRepository", "❌ Update profile HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }

                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg, null)
                }
            }

            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Update profile NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}", null)
            }
        })
    }

    // Получение статистики
    fun getUserStats(callback: (UserStats?, String?) -> Unit) {
        Log.d("UserRepository", "Getting user stats...")
        api.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(
                call: Call<UserStatsResponse>,
                response: Response<UserStatsResponse>
            ) {
                Log.d("UserRepository", "User stats response code: ${response.code()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "User stats loaded successfully")
                        callback(body.stats, null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(null, errorMsg)
                    }
                } else {
                    val errorMsg = "Ошибка сервера: ${response.code()}"
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                Log.e("UserRepository", "User stats network failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun cancelFriendRequest(userId: Int, callback: (Boolean, String?) -> Unit) {
        api.cancelFriendRequest(userId).enqueue(object : Callback<FriendOperationResponse> {
            override fun onResponse(call: Call<FriendOperationResponse>, response: Response<FriendOperationResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Friend request cancelled for user $userId")
                        callback(true, body.message ?: "Заявка отменена")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Невозможно отменить заявку"
                        404 -> "Заявка не найдена"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<FriendOperationResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun getUserWallPosts(userId: Int, callback: (List<WallPost>?, String?) -> Unit) {
        api.getUserWallPosts(userId).enqueue(object : Callback<WallPostsResponse> {
            override fun onResponse(call: Call<WallPostsResponse>, response: Response<WallPostsResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        callback(body.posts ?: emptyList(), null)
                    } else {
                        callback(null, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(null, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WallPostsResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Создать запись на стене
    fun createWallPost(content: String, userId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepository", "=== CREATE WALL POST ===")
        Log.d("UserRepository", "Content: $content, UserID: $userId")

        api.createWallPost(content, userId).enqueue(object : Callback<WallPostResponse> {
            override fun onResponse(call: Call<WallPostResponse>, response: Response<WallPostResponse>) {
                Log.d("UserRepository", "Create wall post response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "✅ Wall post created successfully")
                        callback(true, body.message ?: "Запись создана")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("UserRepository", "❌ Create wall post failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    Log.e("UserRepository", "❌ Create wall post HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }

                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные. Проверьте длину текста (макс. 1000 символов)"
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<WallPostResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Create wall post NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Обновить запись
    fun updateWallPost(postId: Int, content: String, callback: (Boolean, String?) -> Unit) {
        val request = UpdateWallPostRequest(content)
        api.updateWallPost(postId, request).enqueue(object : Callback<WallPostResponse> {
            override fun onResponse(call: Call<WallPostResponse>, response: Response<WallPostResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        callback(true, body.message ?: "Запись обновлена")
                    } else {
                        callback(false, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(false, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WallPostResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Удалить запись
    fun deleteWallPost(postId: Int, callback: (Boolean, String?) -> Unit) {
        api.deleteWallPost(postId).enqueue(object : Callback<OperationResponse> {
            override fun onResponse(call: Call<OperationResponse>, response: Response<OperationResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        callback(true, body.message ?: "Запись удалена")
                    } else {
                        callback(false, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(false, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<OperationResponse>, t: Throwable) {
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }


    // В UserRepository.kt добавить:

    // Получить мои подписки
    fun getSubscriptions(callback: (List<Subscription>?, String?) -> Unit) {
        api.getSubscriptions().enqueue(object : Callback<SubscriptionsResponse> {
            override fun onResponse(call: Call<SubscriptionsResponse>, response: Response<SubscriptionsResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Loaded ${body.subscriptions?.size ?: 0} subscriptions")
                        callback(body.subscriptions ?: emptyList(), null)
                    } else {
                        callback(null, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(null, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<SubscriptionsResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Получить статистику подписок пользователя
    fun getSubscriptionStats(userId: Int, callback: (SubscriptionStats?, String?) -> Unit) {
        api.getSubscriptionStats(userId).enqueue(object : Callback<SubscriptionStatsResponse> {
            override fun onResponse(call: Call<SubscriptionStatsResponse>, response: Response<SubscriptionStatsResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        callback(body.stats, null)
                    } else {
                        callback(null, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(null, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<SubscriptionStatsResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }


    // Получить ленту новостей
    fun getNewsFeed(callback: (List<NewsFeedItem>?, String?) -> Unit) {
        Log.d("UserRepository", "=== GET NEWS FEED ===")

        api.getNewsFeed().enqueue(object : Callback<NewsFeedResponse> {
            override fun onResponse(call: Call<NewsFeedResponse>, response: Response<NewsFeedResponse>) {
                Log.d("UserRepository", "News feed response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        val allItems = mutableListOf<NewsFeedItem>()
                        body.posts?.let { allItems.addAll(it) }
                        body.events?.let { allItems.addAll(it) }
                        // Сортируем по дате (новые сначала)
                        allItems.sortByDescending { it.createdAt }
                        Log.d("UserRepository", "✅ Loaded ${allItems.size} news feed items")
                        callback(allItems, null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("UserRepository", "❌ News feed failed: $errorMsg")
                        callback(null, errorMsg)
                    }
                } else {
                    Log.e("UserRepository", "❌ News feed HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }
                    callback(null, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<NewsFeedResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ News feed NETWORK failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }




    // Подписаться на пользователя
    fun checkSubscription(userId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepository", "=== CHECK SUBSCRIPTION ===")
        Log.d("UserRepository", "Checking subscription for user ID: $userId")

        api.checkSubscription(userId).enqueue(object : Callback<OperationResponse> {
            override fun onResponse(call: Call<OperationResponse>, response: Response<OperationResponse>) {
                Log.d("UserRepository", "Check subscription response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "✅ User is subscribed")
                        callback(true, null)
                    } else {
                        // Если success = false, значит не подписан
                        Log.d("UserRepository", "❌ User is not subscribed")
                        callback(false, null)
                    }
                } else {
                    // Если 404 или другая ошибка - считаем что не подписан
                    Log.d("UserRepository", "Subscription check failed, assuming not subscribed")
                    callback(false, null)
                }
            }

            override fun onFailure(call: Call<OperationResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Check subscription NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Подписаться на пользователя - ИСПРАВЛЕННАЯ ВЕРСИЯ
    fun subscribeToUser(userId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepository", "=== SUBSCRIBE TO USER ===")
        Log.d("UserRepository", "Subscribing to user ID: $userId")

        api.subscribe(userId).enqueue(object : Callback<OperationResponse> {
            override fun onResponse(call: Call<OperationResponse>, response: Response<OperationResponse>) {
                Log.d("UserRepository", "Subscribe response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "✅ Subscribed to user $userId")
                        callback(true, body.message ?: "Подписка оформлена")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("UserRepository", "❌ Subscribe failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    Log.e("UserRepository", "❌ Subscribe HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }

                    val errorMsg = when (response.code()) {
                        400 -> "Невозможно подписаться"
                        404 -> "Пользователь не найден"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<OperationResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Subscribe NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Отписаться от пользователя - ИСПРАВЛЕННАЯ ВЕРСИЯ
    fun unsubscribeFromUser(userId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepository", "=== UNSUBSCRIBE FROM USER ===")
        Log.d("UserRepository", "Unsubscribing from user ID: $userId")

        api.unsubscribe(userId).enqueue(object : Callback<OperationResponse> {
            override fun onResponse(call: Call<OperationResponse>, response: Response<OperationResponse>) {
                Log.d("UserRepository", "Unsubscribe response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "✅ Unsubscribed from user $userId")
                        callback(true, body.message ?: "Подписка отменена")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("UserRepository", "❌ Unsubscribe failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    Log.e("UserRepository", "❌ Unsubscribe HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("UserRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error reading error body: ${e.message}")
                    }

                    val errorMsg = when (response.code()) {
                        400 -> "Невозможно отписаться"
                        404 -> "Пользователь не найден"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<OperationResponse>, t: Throwable) {
                Log.e("UserRepository", "❌ Unsubscribe NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }
    // Логаут
    fun logout() {
        ApiClient.clearSessionToken()
        Log.d("UserRepository", "User logged out")
    }
}