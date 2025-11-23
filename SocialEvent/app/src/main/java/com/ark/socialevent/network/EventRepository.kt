package com.ark.socialevent.network

import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventRepository(private val context: Context) {
    private val api: ApiService by lazy {
        ApiClient.initialize(context)
        ApiClient.getApiService()
    }

    fun getEvents(callback: (List<Event>?, String?) -> Unit) {
        Log.d("EventRepository", "=== GET EVENTS ===")

        api.getEvents().enqueue(object : Callback<EventsResponse> {
            override fun onResponse(call: Call<EventsResponse>, response: Response<EventsResponse>) {
                Log.d("EventRepository", "Events response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Loaded ${body.events?.size ?: 0} events")
                        callback(body.events ?: emptyList(), null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("EventRepository", "❌ Get events failed: $errorMsg")
                        callback(null, errorMsg)
                    }
                } else {
                    Log.e("EventRepository", "❌ Get events HTTP error: ${response.code()}")
                    val errorMsg = when (response.code()) {
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<EventsResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Get events NETWORK failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun createEvent(
        title: String,
        description: String,
        type: String,
        dateTime: String,
        location: String,
        latitude: String? = null,
        longitude: String? = null,
        isPrivate: Boolean = false,
        maxParticipants: Int? = null,
        callback: (Boolean, String?, Event?) -> Unit
    ) {
        Log.d("EventRepository", "=== CREATE EVENT ===")

        val request = CreateEventRequest(
            title = title,
            description = description,
            type = type,
            dateTime = dateTime,
            location = location,
            latitude = latitude,
            longitude = longitude,
            isPrivate = isPrivate,
            maxParticipants = maxParticipants
        )

        api.createEvent(request).enqueue(object : Callback<EventResponse> {
            override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                Log.d("EventRepository", "Create event response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Event created successfully")
                        callback(true, body.message, body.event)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("EventRepository", "❌ Create event failed: $errorMsg")
                        callback(false, errorMsg, null)
                    }
                } else {
                    Log.e("EventRepository", "❌ Create event HTTP error: ${response.code()}")
                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg, null)
                }
            }

            override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Create event NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}", null)
            }
        })
    }

    fun subscribeToEvent(eventId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("EventRepository", "=== SUBSCRIBE TO EVENT ===")

        api.subscribeToEvent(eventId).enqueue(object : Callback<EventSubscriptionResponse> {
            override fun onResponse(call: Call<EventSubscriptionResponse>, response: Response<EventSubscriptionResponse>) {
                Log.d("EventRepository", "Subscribe response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Subscribed to event $eventId")
                        callback(true, body.message ?: "Подписка оформлена")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Невозможно подписаться"
                        404 -> "Событие не найдено"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<EventSubscriptionResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Subscribe NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun unsubscribeFromEvent(eventId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("EventRepository", "=== UNSUBSCRIBE FROM EVENT ===")

        api.unsubscribeFromEvent(eventId).enqueue(object : Callback<EventSubscriptionResponse> {
            override fun onResponse(call: Call<EventSubscriptionResponse>, response: Response<EventSubscriptionResponse>) {
                Log.d("EventRepository", "Unsubscribe response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Unsubscribed from event $eventId")
                        callback(true, body.message ?: "Подписка отменена")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Невозможно отписаться"
                        404 -> "Событие не найдено"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<EventSubscriptionResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Unsubscribe NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }
}