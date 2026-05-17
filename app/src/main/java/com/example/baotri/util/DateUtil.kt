package com.example.baotri.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    private val dateFormat    = SimpleDateFormat("dd/MM/yyyy", Locale("vi"))
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi"))
    private val timeFormat    = SimpleDateFormat("HH:mm", Locale("vi"))

    fun format(millis: Long): String = dateFormat.format(Date(millis))
    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

    fun parse(dateStr: String): Long? = try {
        dateFormat.parse(dateStr)?.time
    } catch (e: Exception) { null }

    fun startOfMonth(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfMonth(millis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }

    fun startOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        return cal.timeInMillis
    }

    fun relativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        return when {
            diff < 60_000L              -> "vừa xong"
            diff < 3_600_000L           -> "${diff / 60_000} phút trước"
            diff < 86_400_000L          -> "${diff / 3_600_000} giờ trước"
            diff < 2 * 86_400_000L      -> "hôm qua"
            else                        -> format(millis)
        }
    }
}

// File size display
fun Long.toReadableSize(): String = when {
    this < 1024L        -> "$this B"
    this < 1024 * 1024L -> "${"%.1f".format(this / 1024.0)} KB"
    else                -> "${"%.1f".format(this / (1024.0 * 1024))} MB"
}
