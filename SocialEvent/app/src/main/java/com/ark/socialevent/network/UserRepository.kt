package com.ark.socialevent.network

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRepository {
    private val api: ApiService = ApiClient.apiService
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()


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

    // Логин пользователя
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        val request = LoginRequest(email, password)
        api.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Login successful")
                        callback(true, body.message ?: "Вход выполнен")
                    } else {
                        val errorMsg = body?.message ?: "Неверный email или пароль"
                        Log.e("UserRepository", "Login failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        401 -> "Неверный email или пароль"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    Log.e("UserRepository", "Login HTTP error: ${response.code()}")
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("UserRepository", "Login network failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    // Получение профиля текущего пользователя - ИСПРАВЛЕНО
    fun getProfile(callback: (UserProfile?) -> Unit) {
        Log.d("UserRepository", "Getting profile...")
        api.getProfile().enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                Log.d("UserRepository", "Profile response code: ${response.code()}")
                Log.d("UserRepository", "Profile response headers: ${response.headers()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("UserRepository", "Get profile successful")
                        callback(body.user)
                    } else {
                        Log.e("UserRepository", "Get profile: empty body")
                        callback(null)
                    }
                } else {
                    Log.e("UserRepository", "Get profile HTTP error: ${response.code()}")
                    // Пробуем прочитать тело ошибки
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
                Log.e("UserRepository", "Get profile network failed: ${t.message}")
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
                        // Пробуем прочитать raw response для отладки
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
                    // Пробуем прочитать raw response для отладки
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

    fun updateProfile(
        firstName: String,
        lastName: String,
        gender: String,
        age: Int,
        phone: String,
        callback: (Boolean, String?, UserProfile?) -> Unit
    ) {
        Log.d("UserRepository", "Updating profile: $firstName $lastName")

        val request = UpdateProfileRequest(firstName, lastName, gender, age, phone)
        api.updateProfile(request).enqueue(object : Callback<UpdateProfileResponse> {
            override fun onResponse(
                call: Call<UpdateProfileResponse>,
                response: Response<UpdateProfileResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("UserRepository", "Profile updated successfully")
                        callback(true, body.message, body.user)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("UserRepository", "Update profile failed: $errorMsg")
                        callback(false, errorMsg, null)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    Log.e("UserRepository", "Update profile HTTP error: ${response.code()}")
                    callback(false, errorMsg, null)
                }
            }

            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                Log.e("UserRepository", "Update profile network failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}", null)
            }
        })
    }

    // Добавьте метод для получения статистики
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
}