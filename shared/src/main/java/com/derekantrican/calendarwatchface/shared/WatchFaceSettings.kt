package com.derekantrican.calendarwatchface.shared

data class WatchFaceSettings(
    val analogClock: Boolean = false,
    val militaryTime: Boolean = true,
    val outerRing24Hours: Boolean = false, // false = 12h ring showing only the current AM/PM half
    val hourLeadingZero: Boolean = true,
    val drawColonBetweenHourAndMinute: Boolean = false,
    val showTickMarksOnAmbient: Boolean = false,
    val showNowIndicatorOnTickmarks: Boolean = true,
    val showDateOnFace: Boolean = true,
    val dateFormat: String = "EEE  dd",
    val showDateInAmbient: Boolean = false,
    val showCalendarEvents: Boolean = true,
    val showCalendarEventsInAmbient: Boolean = true,
    val calendarEventColorWhiteInAmbient: Boolean = true,
    val calendarEventThickness: Int = DEFAULT_EVENT_THICKNESS, // px on the watch
    val enabledCalendars: List<Long>? = null // null = all calendars enabled
) {
    companion object {
        const val DEFAULT_EVENT_THICKNESS = 3
        const val MIN_EVENT_THICKNESS = 2
        const val MAX_EVENT_THICKNESS = 16
        const val EVENT_RING_GAP = 3f
    }
}
