package com.derekantrican.calendarwatchface.mobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.derekantrican.calendarwatchface.shared.Constants
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "companion_settings")

class SettingsRepository(private val context: Context) {

    suspend fun loadSettings(): WatchFaceSettings {
        val prefs = context.settingsDataStore.data.first()
        val defaults = WatchFaceSettings()
        return WatchFaceSettings(
            analogClock = prefs[Keys.ANALOG_CLOCK] ?: defaults.analogClock,
            militaryTime = prefs[Keys.MILITARY_TIME] ?: defaults.militaryTime,
            use24hourHands = prefs[Keys.USE_24HOUR_HANDS] ?: defaults.use24hourHands,
            hourLeadingZero = prefs[Keys.HOUR_LEADING_ZERO] ?: defaults.hourLeadingZero,
            drawColonBetweenHourAndMinute = prefs[Keys.DRAW_COLON] ?: defaults.drawColonBetweenHourAndMinute,
            showTickMarksOnAmbient = prefs[Keys.TICK_MARKS_AMBIENT] ?: defaults.showTickMarksOnAmbient,
            use24hourTickMarks = prefs[Keys.USE_24HOUR_TICKS] ?: defaults.use24hourTickMarks,
            showNowIndicatorOnTickmarks = prefs[Keys.NOW_INDICATOR] ?: defaults.showNowIndicatorOnTickmarks,
            showDateOnFace = prefs[Keys.SHOW_DATE] ?: defaults.showDateOnFace,
            dateFormat = prefs[Keys.DATE_FORMAT] ?: defaults.dateFormat,
            showDateInAmbient = prefs[Keys.DATE_AMBIENT] ?: defaults.showDateInAmbient,
            showCalendarEvents = prefs[Keys.CALENDAR_EVENTS] ?: defaults.showCalendarEvents,
            showCalendarEventsInAmbient = prefs[Keys.CALENDAR_EVENTS_AMBIENT] ?: defaults.showCalendarEventsInAmbient,
            calendarEventColorWhiteInAmbient = prefs[Keys.CALENDAR_WHITE_AMBIENT] ?: defaults.calendarEventColorWhiteInAmbient,
            calendarEventThickness = prefs[Keys.CALENDAR_EVENT_THICKNESS] ?: defaults.calendarEventThickness
        )
    }

    // Calendars the user has unchecked. Stored as the disabled set so newly added calendars default to enabled.
    suspend fun loadDisabledCalendarIds(): Set<Long> {
        val prefs = context.settingsDataStore.data.first()
        return prefs[Keys.DISABLED_CALENDARS]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun saveSettings(settings: WatchFaceSettings, disabledCalendarIds: Set<Long>) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ANALOG_CLOCK] = settings.analogClock
            prefs[Keys.MILITARY_TIME] = settings.militaryTime
            prefs[Keys.USE_24HOUR_HANDS] = settings.use24hourHands
            prefs[Keys.HOUR_LEADING_ZERO] = settings.hourLeadingZero
            prefs[Keys.DRAW_COLON] = settings.drawColonBetweenHourAndMinute
            prefs[Keys.TICK_MARKS_AMBIENT] = settings.showTickMarksOnAmbient
            prefs[Keys.USE_24HOUR_TICKS] = settings.use24hourTickMarks
            prefs[Keys.NOW_INDICATOR] = settings.showNowIndicatorOnTickmarks
            prefs[Keys.SHOW_DATE] = settings.showDateOnFace
            prefs[Keys.DATE_FORMAT] = settings.dateFormat
            prefs[Keys.DATE_AMBIENT] = settings.showDateInAmbient
            prefs[Keys.CALENDAR_EVENTS] = settings.showCalendarEvents
            prefs[Keys.CALENDAR_EVENTS_AMBIENT] = settings.showCalendarEventsInAmbient
            prefs[Keys.CALENDAR_WHITE_AMBIENT] = settings.calendarEventColorWhiteInAmbient
            prefs[Keys.CALENDAR_EVENT_THICKNESS] = settings.calendarEventThickness
            prefs[Keys.DISABLED_CALENDARS] = disabledCalendarIds.map { it.toString() }.toSet()
        }
    }

    suspend fun syncSettingsToWatch(settings: WatchFaceSettings, enabledCalendarIds: List<Long>) {
        try {
            val putDataReq = PutDataMapRequest.create(Constants.SETTINGS_PATH).apply {
                dataMap.putBoolean("analogClock", settings.analogClock)
                dataMap.putBoolean("militaryTime", settings.militaryTime)
                dataMap.putBoolean("use24hourHands", settings.use24hourHands)
                dataMap.putBoolean("hourLeadingZero", settings.hourLeadingZero)
                dataMap.putBoolean("drawColon", settings.drawColonBetweenHourAndMinute)
                dataMap.putBoolean("tickMarksAmbient", settings.showTickMarksOnAmbient)
                dataMap.putBoolean("use24hourTicks", settings.use24hourTickMarks)
                dataMap.putBoolean("nowIndicator", settings.showNowIndicatorOnTickmarks)
                dataMap.putBoolean("showDate", settings.showDateOnFace)
                dataMap.putString("dateFormat", settings.dateFormat)
                dataMap.putBoolean("dateAmbient", settings.showDateInAmbient)
                dataMap.putBoolean("calendarEvents", settings.showCalendarEvents)
                dataMap.putBoolean("calendarEventsAmbient", settings.showCalendarEventsInAmbient)
                dataMap.putBoolean("calendarWhiteAmbient", settings.calendarEventColorWhiteInAmbient)
                dataMap.putInt("calendarEventThickness", settings.calendarEventThickness)
                dataMap.putLongArray("enabledCalendars", enabledCalendarIds.toLongArray())
                // Force update by adding timestamp
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(putDataReq).await()
        } catch (_: Exception) {
            // Watch may not be connected
        }
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
        val DISABLED_CALENDARS = stringSetPreferencesKey("disabled_calendars")
    }
}
