package com.derekantrican.calendarwatchface.mobile

import android.content.Context
import com.derekantrican.calendarwatchface.shared.Constants
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val context: Context) {

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
}
