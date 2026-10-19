package com.derekantrican.calendarwatchface.shared

data class CalendarEvent(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val color: Int,
    val calendarId: Long,
    val isAllDay: Boolean = false
)
