package com.derekantrican.calendarwatchface

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.derekantrican.calendarwatchface.shared.CalendarEvent
import com.derekantrican.calendarwatchface.shared.CalendarInfo
import java.util.Calendar
import java.util.TimeZone

class CalendarRepository(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getTodayEvents(enabledCalendarIds: List<Long>? = null): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        val now = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = startOfDay.timeInMillis
            add(Calendar.DAY_OF_MONTH, 1)
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.ALL_DAY
        )

        // The watch's own CalendarProvider is typically empty; Wear OS mirrors the phone's
        // calendar (same calendar IDs) into its own provider. Fall back to the standard one.
        val events = queryInstances(WEARABLE_INSTANCES_URI, projection, startOfDay, endOfDay, enabledCalendarIds)
        if (events != null) return events

        return queryInstances(CalendarContract.Instances.CONTENT_URI, projection, startOfDay, endOfDay, enabledCalendarIds)
            ?: emptyList()
    }

    private fun queryInstances(
        baseUri: Uri,
        projection: Array<String>,
        startOfDay: Calendar,
        endOfDay: Calendar,
        enabledCalendarIds: List<Long>?
    ): List<CalendarEvent>? {
        val uri = baseUri.buildUpon()
            .let { builder ->
                ContentUris.appendId(builder, startOfDay.timeInMillis)
                ContentUris.appendId(builder, endOfDay.timeInMillis)
                builder.build()
            }

        val events = mutableListOf<CalendarEvent>()
        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null) // Wearable provider rejects sortOrder; renderer sorts
                ?: return null
            cursor.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val eventColorIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
                val calColorIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext()) {
                    val calendarId = cursor.getLong(calIdIdx)
                    val isAllDay = cursor.getInt(allDayIdx) == 1

                    // Skip all-day events
                    if (isAllDay) continue

                    // Filter by enabled calendars (null = all enabled)
                    if (enabledCalendarIds != null && calendarId !in enabledCalendarIds) continue

                    val eventColor = cursor.getInt(eventColorIdx)
                    val calColor = cursor.getInt(calColorIdx)

                    events.add(
                        CalendarEvent(
                            title = cursor.getString(titleIdx) ?: "",
                            startTimeMillis = cursor.getLong(beginIdx),
                            endTimeMillis = cursor.getLong(endIdx),
                            color = if (eventColor != 0) eventColor else calColor,
                            calendarId = calendarId,
                            isAllDay = false
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked at runtime
            return null
        } catch (_: IllegalArgumentException) {
            // Provider doesn't exist on this device
            return null
        }

        return events
    }

    companion object {
        private val WEARABLE_INSTANCES_URI: Uri =
            Uri.parse("content://com.google.android.wearable.provider.calendar/instances/when")
    }

    fun getCalendars(): List<CalendarInfo> {
        if (!hasCalendarPermission()) return emptyList()

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
                projection, null, null, null
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
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }

        return calendars
    }
}
