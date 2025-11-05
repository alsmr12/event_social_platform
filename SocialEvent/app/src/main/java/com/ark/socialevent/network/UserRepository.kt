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
            .baseUrl(ApiClient.BASE_URL)
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
        val request = RegisterRequest(email, password, firstName, lastName, gender, age, phone)
        api.register(request).enqueue(object : Callback<ApiResponse<Unit>> {
            override fun onResponse(call: Call<ApiResponse<Unit>>, response: Response<ApiResponse<Unit>>) {
                val body = response.body()
                callback(body?.success == true, body?.message)
            }

            override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                Log.e("UserRepository", "Register failed: ${t.message}")
                callback(false, t.message)
            }
        })
    }

    // Логин пользователя
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        val request = LoginRequest(email, password)
        api.login(request).enqueue(object : Callback<ApiResponse<Unit>> {
            override fun onResponse(call: Call<ApiResponse<Unit>>, response: Response<ApiResponse<Unit>>) {
                val body = response.body()
                callback(body?.success == true, body?.message)
            }

            override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                Log.e("UserRepository", "Login failed: ${t.message}")
                callback(false, t.message)
            }
        })
    }

    // Получение профиля текущего пользователя
    fun getProfile(callback: (ProfileResponse?) -> Unit) {
        api.getProfile().enqueue(object : Callback<ApiResponse<ProfileResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<ProfileResponse>>,
                response: Response<ApiResponse<ProfileResponse>>
            ) {
                val body = response.body()
                if (body?.success == true) {
                    callback(body.data)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<ApiResponse<ProfileResponse>>, t: Throwable) {
                Log.e("UserRepository", "Get profile failed: ${t.message}")
                callback(null)
            }
        })
    }
}