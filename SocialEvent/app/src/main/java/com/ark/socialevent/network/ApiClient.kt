package com.ark.socialevent.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient {
    companion object {
        private const val PREFS_NAME = "SocialEventPrefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"

        // URL по умолчанию для локального сервера
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

        private lateinit var sharedPreferences: SharedPreferences
        private var apiService: ApiService? = null

        fun initialize(context: Context) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun getBaseUrl(): String {
            return sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        }

        fun setBaseUrl(url: String) {
            sharedPreferences.edit().putString(KEY_BASE_URL, url).apply()
            // Сбрасываем apiService при изменении baseUrl
            apiService = null
        }

        fun getApiService(): ApiService {
            if (apiService == null) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val requestBuilder = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("User-Agent", "SocialEvent-Android-App/1.0")

                        // Добавляем токен авторизации если есть
                        getSessionToken()?.let { token ->
                            requestBuilder.header("Authorization", "Bearer $token")
                        }

                        val request = requestBuilder.build()
                        chain.proceed(request)
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                apiService = retrofit.create(ApiService::class.java)
            }
            return apiService!!
        }

        // Остальные методы для работы с токеном остаются без изменений...
        fun saveSessionToken(token: String) {
            sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token).apply()
        }

        fun getSessionToken(): String? {
            return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
        }

        fun clearSessionToken() {
            sharedPreferences.edit().remove(KEY_AUTH_TOKEN).apply()
        }
    }
}