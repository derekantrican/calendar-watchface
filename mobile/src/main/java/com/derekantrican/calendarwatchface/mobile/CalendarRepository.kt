package com.derekantrican.calendarwatchface.mobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.derekantrican.calendarwatchface.shared.CalendarInfo

class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCalendars(): List<CalendarInfo> {
        if (!hasPermission()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        val calendars = mutableListOf<CalendarInfo>()
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                // Unsynced calendars have no events on the device (or watch), so don't list them
                projection, "${CalendarContract.Calendars.SYNC_EVENTS} = 1", null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

                while (cursor.moveToNext()) {
                    calendars.add(
                        CalendarInfo(
                            id = cursor.getLong(idIdx),
                            name = cursor.getString(nameIdx) ?: "",
                            accountName = cursor.getString(accountIdx) ?: "",
                            color = cursor.getInt(colorIdx)
                        )
                    )
                }
            }
        } catch (_: SecurityException) {}

        return calendars
    }
}
