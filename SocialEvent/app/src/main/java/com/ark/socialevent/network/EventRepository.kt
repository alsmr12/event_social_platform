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
            override fun onResponse(
                call: Call<EventSubscriptionResponse>,
                response: Response<EventSubscriptionResponse>
            ) {
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
            override fun onResponse(
                call: Call<EventSubscriptionResponse>,
                response: Response<EventSubscriptionResponse>
            ) {
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

    fun getEventsFeed(callback: (List<Event>?, String?) -> Unit) {
        Log.d("EventRepository", "=== GET EVENTS FEED ===")

        api.getEventsFeed().enqueue(object : Callback<EventsResponse> {
            override fun onResponse(
                call: Call<EventsResponse>,
                response: Response<EventsResponse>
            ) {
                Log.d("EventRepository", "Events feed response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d(
                            "EventRepository",
                            "✅ Loaded ${body.events?.size ?: 0} events from feed"
                        )
                        callback(body.events ?: emptyList(), null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(null, errorMsg)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<EventsResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Get events feed failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun getUserEvents(userId: Int, callback: (List<Event>?, String?) -> Unit) {
        api.getUserEvents(userId).enqueue(object : Callback<EventsResponse> {
            override fun onResponse(
                call: Call<EventsResponse>,
                response: Response<EventsResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        callback(body.events ?: emptyList(), null)
                    } else {
                        callback(null, body?.message ?: "Неизвестная ошибка")
                    }
                } else {
                    callback(null, "Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<EventsResponse>, t: Throwable) {
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun joinEventByCode(code: String, callback: (Boolean, String?, Event?) -> Unit) {
        Log.d("EventRepository", "=== JOIN EVENT BY CODE: $code ===")

        val request = JoinEventRequest(code = code)
        api.joinEventByCode(request).enqueue(object : Callback<JoinEventResponse> {
            override fun onResponse(
                call: Call<JoinEventResponse>,
                response: Response<JoinEventResponse>
            ) {
                Log.d("EventRepository", "Join event response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Joined event with code: $code")
                        callback(true, body.message, body.event)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        callback(false, errorMsg, null)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "Событие с таким кодом не найдено"
                        400 -> "Неверный код"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg, null)
                }
            }

            override fun onFailure(call: Call<JoinEventResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Join event failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}", null)
            }
        })
    }

    fun updateEvent(
        eventId: Int,
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

        api.updateEvent(eventId, request).enqueue(object : Callback<EventResponse> {
            override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                Log.d("EventRepository", "Update event response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Event updated successfully")
                        callback(true, body.message, body.event)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("EventRepository", "❌ Update event failed: $errorMsg")
                        callback(false, errorMsg, null)
                    }
                } else {
                    Log.e("EventRepository", "❌ Update event HTTP error: ${response.code()}")
                    val errorMsg = when (response.code()) {
                        400 -> "Некорректные данные"
                        401 -> "Не авторизован"
                        403 -> "Доступ запрещен"
                        404 -> "Событие не найдено"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg, null)
                }
            }

            override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Update event NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}", null)
            }
        })
    }

    fun deleteEvent(eventId: Int, callback: (Boolean, String?) -> Unit) {
        Log.d("EventRepository", "=== DELETE EVENT $eventId ===")

        api.deleteEvent(eventId).enqueue(object : Callback<OperationResponse> {
            override fun onResponse(
                call: Call<OperationResponse>,
                response: Response<OperationResponse>
            ) {
                Log.d("EventRepository", "Delete event response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Event deleted successfully")
                        callback(true, body.message ?: "Событие удалено")
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("EventRepository", "❌ Delete event failed: $errorMsg")
                        callback(false, errorMsg)
                    }
                } else {
                    Log.e("EventRepository", "❌ Delete event HTTP error: ${response.code()}")
                    val errorMsg = when (response.code()) {
                        401 -> "Не авторизован"
                        403 -> "Доступ запрещен"
                        404 -> "Событие не найдено"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(false, errorMsg)
                }
            }

            override fun onFailure(call: Call<OperationResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Delete event NETWORK failed: ${t.message}")
                callback(false, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun getEventsWithFilters(
        type: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        radius: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        timeFilter: String? = null,
        callback: (List<Event>?, String?) -> Unit
    ) {
        Log.d("EventRepository", "=== GET EVENTS WITH FILTERS ===")
        Log.d("EventRepository", "Filters: type=$type, dateFrom=$dateFrom, dateTo=$dateTo, radius=$radius, timeFilter=$timeFilter")

        // Используем новый endpoint для фильтрации
        api.getEventsWithFilters(
            type = type,
            dateFrom = dateFrom,  // Retrofit преобразует это в "date_from" благодаря аннотации
            dateTo = dateTo,
            radius = radius,
            timeFilter = timeFilter
        ).enqueue(object : Callback<EventsResponse> {
            override fun onResponse(
                call: Call<EventsResponse>,
                response: Response<EventsResponse>
            ) {
                Log.d("EventRepository", "Filtered events response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Loaded ${body.events?.size ?: 0} filtered events")
                        callback(body.events ?: emptyList(), null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("EventRepository", "❌ Get filtered events failed: $errorMsg")
                        callback(null, errorMsg)
                    }
                } else {
                    Log.e("EventRepository", "❌ Get filtered events HTTP error: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("EventRepository", "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("EventRepository", "Error reading error body: ${e.message}")
                    }

                    val errorMsg = when (response.code()) {
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<EventsResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Get filtered events NETWORK failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }

    fun getEventById(eventId: Int, callback: (Event?, String?) -> Unit) {
        Log.d("EventRepository", "=== GET EVENT BY ID: $eventId ===")

        api.getEvent(eventId).enqueue(object : Callback<EventResponse> {
            override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                Log.d("EventRepository", "Get event by ID response: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Log.d("EventRepository", "✅ Loaded event: ${body.event?.title}")
                        callback(body.event, null)
                    } else {
                        val errorMsg = body?.message ?: "Неизвестная ошибка"
                        Log.e("EventRepository", "❌ Get event by ID failed: $errorMsg")
                        callback(null, errorMsg)
                    }
                } else {
                    Log.e("EventRepository", "❌ Get event by ID HTTP error: ${response.code()}")
                    val errorMsg = when (response.code()) {
                        404 -> "Событие не найдено"
                        401 -> "Не авторизован"
                        500 -> "Ошибка сервера"
                        else -> "Ошибка: ${response.code()}"
                    }
                    callback(null, errorMsg)
                }
            }

            override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                Log.e("EventRepository", "❌ Get event by ID NETWORK failed: ${t.message}")
                callback(null, "Ошибка сети: ${t.message}")
            }
        })
    }
}