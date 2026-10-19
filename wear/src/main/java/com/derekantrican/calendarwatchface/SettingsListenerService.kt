package com.derekantrican.calendarwatchface

import com.derekantrican.calendarwatchface.shared.Constants
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class SettingsListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == Constants.SETTINGS_PATH
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                scope.launch {
                    applicationContext.settingsDataStore.edit { prefs ->
                        prefs[booleanPreferencesKey("analog_clock")] = dataMap.getBoolean("analogClock", false)
                        prefs[booleanPreferencesKey("military_time")] = dataMap.getBoolean("militaryTime", true)
                        prefs[booleanPreferencesKey("outer_ring_24_hours")] = dataMap.getBoolean("outerRing24Hours", false)
                        prefs[booleanPreferencesKey("hour_leading_zero")] = dataMap.getBoolean("hourLeadingZero", true)
                        prefs[booleanPreferencesKey("draw_colon")] = dataMap.getBoolean("drawColon", false)
                        prefs[booleanPreferencesKey("tick_marks_ambient")] = dataMap.getBoolean("tickMarksAmbient", false)
                        prefs[booleanPreferencesKey("now_indicator")] = dataMap.getBoolean("nowIndicator", true)
                        prefs[booleanPreferencesKey("show_date")] = dataMap.getBoolean("showDate", true)
                        prefs[stringPreferencesKey("date_format")] = dataMap.getString("dateFormat", "EEE  dd")!!
                        prefs[booleanPreferencesKey("date_ambient")] = dataMap.getBoolean("dateAmbient", false)
                        prefs[booleanPreferencesKey("calendar_events")] = dataMap.getBoolean("calendarEvents", true)
                        prefs[booleanPreferencesKey("calendar_events_ambient")] = dataMap.getBoolean("calendarEventsAmbient", true)
                        prefs[booleanPreferencesKey("calendar_white_ambient")] = dataMap.getBoolean("calendarWhiteAmbient", true)
                        prefs[intPreferencesKey("calendar_event_thickness")] =
                            dataMap.getInt("calendarEventThickness", WatchFaceSettings.DEFAULT_EVENT_THICKNESS)
                        dataMap.getLongArray("enabledCalendars")?.let { ids ->
                            prefs[stringPreferencesKey("enabled_calendars")] = ids.joinToString(",")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
