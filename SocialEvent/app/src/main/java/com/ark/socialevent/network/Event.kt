package com.ark.socialevent.network

import com.google.gson.annotations.SerializedName
import java.util.*

data class Event(
    val id: Int,
    val title: String,
    val description: String,
    val type: String,
    @SerializedName("date_time") val dateTime: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("creator_id") val creatorId: Int,
    val creator: UserProfile,
    @SerializedName("is_private") val isPrivate: Boolean,
    @SerializedName("invite_code") val inviteCode: String?,
    @SerializedName("private_key") val privateKey: String?,
    @SerializedName("max_participants") val maxParticipants: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("is_subscribed") val isSubscribed: Boolean = false,
    @SerializedName("subscribers_count") val subscribersCount: Int = 0,
    @SerializedName("is_past") val isPast: Boolean = false
)

data class CreateEventRequest(
    val title: String,
    val description: String,
    val type: String,
    @SerializedName("date_time") val dateTime: String,
    val location: String,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("max_participants") val maxParticipants: Int? = null
)

data class EventsResponse(
    val success: Boolean,
    val events: List<Event>? = null,
    val message: String? = null
)

data class EventResponse(
    val success: Boolean,
    val event: Event? = null,
    val message: String? = null
)

data class EventSubscriptionResponse(
    val success: Boolean,
    val message: String? = null
)