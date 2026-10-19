package com.derekantrican.calendarwatchface

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

    val settings: Flow<WatchFaceSettings> = context.settingsDataStore.data.map { prefs ->
        WatchFaceSettings(
            analogClock = prefs[Keys.ANALOG_CLOCK] ?: false,
            militaryTime = prefs[Keys.MILITARY_TIME] ?: true,
            use24hourHands = prefs[Keys.USE_24HOUR_HANDS] ?: false,
            hourLeadingZero = prefs[Keys.HOUR_LEADING_ZERO] ?: true,
            drawColonBetweenHourAndMinute = prefs[Keys.DRAW_COLON] ?: false,
            showTickMarksOnAmbient = prefs[Keys.TICK_MARKS_AMBIENT] ?: false,
            use24hourTickMarks = prefs[Keys.USE_24HOUR_TICKS] ?: false,
            showNowIndicatorOnTickmarks = prefs[Keys.NOW_INDICATOR] ?: true,
            showDateOnFace = prefs[Keys.SHOW_DATE] ?: true,
            dateFormat = prefs[Keys.DATE_FORMAT] ?: "EEE  dd",
            showDateInAmbient = prefs[Keys.DATE_AMBIENT] ?: false,
            showCalendarEvents = prefs[Keys.CALENDAR_EVENTS] ?: true,
            showCalendarEventsInAmbient = prefs[Keys.CALENDAR_EVENTS_AMBIENT] ?: true,
            calendarEventColorWhiteInAmbient = prefs[Keys.CALENDAR_WHITE_AMBIENT] ?: true,
            calendarEventThickness = prefs[Keys.CALENDAR_EVENT_THICKNESS] ?: WatchFaceSettings.DEFAULT_EVENT_THICKNESS,
            enabledCalendars = prefs[Keys.ENABLED_CALENDARS]?.split(",")?.mapNotNull { it.toLongOrNull() }
        )
    }

    private object Keys {
        val ANALOG_CLOCK = booleanPreferencesKey("analog_clock")
        val MILITARY_TIME = booleanPreferencesKey("military_time")
        val USE_24HOUR_HANDS = booleanPreferencesKey("use_24hour_hands")
        val HOUR_LEADING_ZERO = booleanPreferencesKey("hour_leading_zero")
        val DRAW_COLON = booleanPreferencesKey("draw_colon")
        val TICK_MARKS_AMBIENT = booleanPreferencesKey("tick_marks_ambient")
        val USE_24HOUR_TICKS = booleanPreferencesKey("use_24hour_ticks")
        val NOW_INDICATOR = booleanPreferencesKey("now_indicator")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val DATE_AMBIENT = booleanPreferencesKey("date_ambient")
        val CALENDAR_EVENTS = booleanPreferencesKey("calendar_events")
        val CALENDAR_EVENTS_AMBIENT = booleanPreferencesKey("calendar_events_ambient")
        val CALENDAR_WHITE_AMBIENT = booleanPreferencesKey("calendar_white_ambient")
        val CALENDAR_EVENT_THICKNESS = intPreferencesKey("calendar_event_thickness")
        val ENABLED_CALENDARS = stringPreferencesKey("enabled_calendars")
    }
}
