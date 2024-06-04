package com.stopsmoke.kekkek.domain.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.Period

data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val dateTime: LocalDateTime,
    val category: NotificationCategory,
) {

    val elapsedDateTime = dateTime.getElapsedDateTime()
}

private fun LocalDateTime.getElapsedDateTime(): ElapsedDateTime {
    val currentDateTime = LocalDateTime.now()
    val period = Period.between(this.toLocalDate(), currentDateTime.toLocalDate())

    if (period.years > 0) {
        return ElapsedDateTime(DateTimeUnit.YEAR, period.years)
    }

    if (period.months > 0) {
        return ElapsedDateTime(DateTimeUnit.MONTH, period.months)
    }

    if (period.days >= 7) {
        return ElapsedDateTime(DateTimeUnit.WEEK, period.days / 7)
    }

    if (period.days > 0) {
        return ElapsedDateTime(DateTimeUnit.DAY, period.days)
    }

    val duration = Duration.between(this, currentDateTime)

    if (duration.toMinutes() > 0) {
        return ElapsedDateTime(DateTimeUnit.MINUTE, duration.toMinutes().toInt())
    }

    return ElapsedDateTime(DateTimeUnit.SECOND, duration.seconds.toInt())
}