package com.ark.socialevent.network

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import java.net.CookieManager
import java.net.CookiePolicy

object ApiClient {

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager)) // поддержка сессий через куки
            .build()
    }

    const val BASE_URL = "http://10.0.2.2:8080" // эмулятор → localhost
}
