package com.derekantrican.calendarwatchface.mobile

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.derekantrican.calendarwatchface.mobile.ui.CalendarPickerScreen
import com.derekantrican.calendarwatchface.mobile.ui.PreviewScreen
import com.derekantrican.calendarwatchface.mobile.ui.SettingsScreen
import com.derekantrican.calendarwatchface.shared.CalendarInfo
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var calendarRepo: CalendarRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private var settings by mutableStateOf(WatchFaceSettings())
    private var calendars by mutableStateOf<List<CalendarInfo>>(emptyList())
    private var disabledCalendarIds: Set<Long> = emptySet()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) loadCalendars()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepo = SettingsRepository(applicationContext)
        calendarRepo = CalendarRepository(applicationContext)

        scope.launch {
            settings = settingsRepo.loadSettings()
            disabledCalendarIds = settingsRepo.loadDisabledCalendarIds()

            if (calendarRepo.hasPermission()) {
                loadCalendars()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        }

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Calendar Watch Face") }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "settings",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("settings") {
                            Column(modifier = Modifier.fillMaxSize()) {
                                PreviewScreen(settings = settings)
                                SettingsScreen(
                                    settings = settings,
                                    onSettingsChanged = { newSettings ->
                                        settings = newSettings
                                        syncSettings()
                                    },
                                    onNavigateToCalendarPicker = {
                                        navController.navigate("calendars")
                                    }
                                )
                            }
                        }
                        composable("calendars") {
                            CalendarPickerScreen(
                                calendars = calendars,
                                onCalendarToggled = { id, enabled ->
                                    calendars = calendars.map {
                                        if (it.id == id) it.copy(isEnabled = enabled) else it
                                    }
                                    syncSettings()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadCalendars() {
        val loaded = calendarRepo.getCalendars()
        calendars = loaded.map { it.copy(isEnabled = it.id !in disabledCalendarIds) }
    }

    private fun syncSettings() {
        val enabledIds = calendars.filter { it.isEnabled }.map { it.id }
        // Keep disabled IDs for calendars not currently listed (e.g. sync temporarily off)
        disabledCalendarIds = (disabledCalendarIds - calendars.map { it.id }.toSet()) +
            calendars.filter { !it.isEnabled }.map { it.id }
        val settingsSnapshot = settings
        val disabledSnapshot = disabledCalendarIds
        scope.launch {
            settingsRepo.saveSettings(settingsSnapshot, disabledSnapshot)
            settingsRepo.syncSettingsToWatch(settingsSnapshot, enabledIds)
        }
    }
}
