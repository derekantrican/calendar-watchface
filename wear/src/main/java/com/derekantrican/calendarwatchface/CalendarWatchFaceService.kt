package com.derekantrican.calendarwatchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.CalendarContract
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CalendarWatchFaceService : WatchFaceService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var settingsRepository: SettingsRepository
    private var renderer: CalendarWatchFaceRenderer? = null
    @Volatile private var enabledCalendarIds: List<Long>? = null

    private val timezoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // ZonedDateTime in render() auto-uses current timezone
        }
    }

    private val calendarReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshCalendarEvents()
        }
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        calendarRepository = CalendarRepository(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)

        val watchFaceRenderer = CalendarWatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository
        )
        renderer = watchFaceRenderer

        registerReceiver(timezoneReceiver, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED))
        registerReceiver(calendarReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PROVIDER_CHANGED)
            addDataScheme("content")
            addDataAuthority(CalendarContract.AUTHORITY, null)
        })

        // Observe settings changes
        serviceScope.launch {
            settingsRepository.settings.collect { settings ->
                watchFaceRenderer.updateSettings(settings)
                if (settings.enabledCalendars != enabledCalendarIds) {
                    enabledCalendarIds = settings.enabledCalendars
                    refreshCalendarEvents()
                }
            }
        }

        // Refresh calendar events every 5 minutes
        serviceScope.launch {
            while (true) {
                refreshCalendarEvents()
                delay(5 * 60 * 1000L)
            }
        }

        return WatchFace(WatchFaceType.DIGITAL, watchFaceRenderer)
    }

    private fun refreshCalendarEvents() {
        serviceScope.launch(Dispatchers.IO) {
            val events = calendarRepository.getTodayEvents(enabledCalendarIds)
            renderer?.updateEvents(events)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            unregisterReceiver(timezoneReceiver)
            unregisterReceiver(calendarReceiver)
        } catch (_: Exception) {}
    }
}
