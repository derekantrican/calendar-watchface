package com.derekantrican.calendarwatchface.shared

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int,
    val isEnabled: Boolean = true
)
