package com.ark.socialevent.data

object EventTypes {
    val eventTypes = listOf(
        "concert",      // Концерт
        "sport",        // Спорт
        "lecture",      // Лекция
        "meeting",      // Встреча
        "party",        // Вечеринка
        "conference",   // Конференция
        "exhibition",   // Выставка
        "other"         // Другое
    )

    // Функция для получения русского названия по ключу
    fun getDisplayName(type: String): String {
        return when (type) {
            "concert" -> "Концерт"
            "sport" -> "Спорт"
            "lecture" -> "Лекция"
            "meeting" -> "Встреча"
            "party" -> "Вечеринка"
            "conference" -> "Конференция"
            "exhibition" -> "Выставка"
            "other" -> "Другое"
            else -> "Другое"
        }
    }
}