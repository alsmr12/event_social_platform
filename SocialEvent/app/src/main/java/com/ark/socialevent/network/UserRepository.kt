package com.ark.socialevent.network

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRepository {
    private val api: ApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)
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
        api.getProfile().enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("UserRepository", "Get profile successful")
                        callback(body.user) // Используем body.user вместо body.data
                    } else {
                        Log.e("UserRepository", "Get profile: empty body")
                        callback(null)
                    }
                } else {
                    Log.e("UserRepository", "Get profile HTTP error: ${response.code()}")
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
        api.getAllProfiles().enqueue(object : Callback<List<UserProfile>> {
            override fun onResponse(
                call: Call<List<UserProfile>>,
                response: Response<List<UserProfile>>
            ) {
                if (response.isSuccessful) {
                    callback(response.body(), null)
                } else {
                    callback(null, "Ошибка: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<UserProfile>>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

}