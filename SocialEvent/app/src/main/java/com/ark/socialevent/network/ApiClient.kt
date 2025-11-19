// ApiClient.kt
package com.ark.socialevent.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    private var apiService: ApiService? = null

    // Используем Application Context когда он понадобится
    private var appContext: Context? = null

    // Инициализируем с контекстом (вызови это в MainActivity)
    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d("ApiClient", "ApiClient initialized with context")
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            if (appContext == null) {
                throw IllegalStateException("ApiClient not initialized. Call ApiClient.initialize(context) first.")
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .addInterceptor { chain ->
                    val original = chain.request()

                    val requestBuilder = original.newBuilder()
                        .header("Content-Type", "application/json")
                        .method(original.method, original.body)

                    // Добавляем токен в заголовок Authorization
                    val sessionToken = getSessionToken()
                    if (sessionToken != null) {
                        requestBuilder.header("Authorization", "Bearer $sessionToken")
                        Log.d("ApiClient", "Adding Authorization header with token: $sessionToken")
                    } else {
                        Log.d("ApiClient", "No session token found, sending request without auth")
                    }

                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            apiService = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
        return apiService!!
    }

    // Сохраняем токен после логина
    fun saveSessionToken(token: String) {
        val sharedPref = getSharedPreferences()
        sharedPref.edit().putString("session_token", token).apply()
        Log.d("ApiClient", "Session token saved: $token")
    }

    // Получаем сохраненный токен
    fun getSessionToken(): String? {
        val sharedPref = getSharedPreferences()
        val token = sharedPref.getString("session_token", null)
        Log.d("ApiClient", "Retrieved session token: $token")
        return token
    }

    // Очищаем токен при логауте
    fun clearSessionToken() {
        val sharedPref = getSharedPreferences()
        sharedPref.edit().remove("session_token").apply()
        Log.d("ApiClient", "Session token cleared")
    }

    private fun getSharedPreferences(): SharedPreferences {
        if (appContext == null) {
            throw IllegalStateException("ApiClient not initialized")
        }
        return appContext!!.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }
}