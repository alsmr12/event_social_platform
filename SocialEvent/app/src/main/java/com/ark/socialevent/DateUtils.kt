package com.ark.socialevent.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    // Вычисляет возраст из даты рождения
    fun calculateAge(birthDate: String): Int {
        return try {
            val birth = dateFormat.parse(birthDate)
            val now = Calendar.getInstance().time

            val diff = now.time - birth.time
            val age = TimeUnit.MILLISECONDS.toDays(diff) / 365

            age.toInt()
        } catch (e: Exception) {
            0
        }
    }

    // Форматирует дату для отображения
    fun formatDateForDisplay(date: String): String {
        return try {
            val parsed = dateFormat.parse(date)
            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            displayFormat.format(parsed)
        } catch (e: Exception) {
            date
        }
    }

    // Получает текущую дату в формате строки
    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    // Вычисляет дату рождения из возраста
    fun calculateBirthDateFromAge(age: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -age)
        return dateFormat.format(calendar.time)
    }

    // Проверяет валидность даты
    fun isValidDate(date: String): Boolean {
        return try {
            dateFormat.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }
    fun formatBirthDate(birthDate: String): String {
        return try {
            val parsed = dateFormat.parse(birthDate)
            displayFormat.format(parsed)
        } catch (e: Exception) {
            birthDate
        }
    }
}