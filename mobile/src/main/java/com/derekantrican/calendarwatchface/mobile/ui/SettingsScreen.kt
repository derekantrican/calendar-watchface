package com.derekantrican.calendarwatchface.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: WatchFaceSettings,
    onSettingsChanged: (WatchFaceSettings) -> Unit,
    onNavigateToCalendarPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        SectionHeader("Clock", topSpacing = 0.dp)

        SettingToggle("Analog Clock", settings.analogClock) {
            onSettingsChanged(settings.copy(analogClock = it))
        }
        SettingToggle("24-Hour Time", settings.militaryTime) {
            onSettingsChanged(settings.copy(militaryTime = it))
        }
        SettingToggle("Hour Leading Zero", settings.hourLeadingZero) {
            onSettingsChanged(settings.copy(hourLeadingZero = it))
        }
        SettingToggle("Draw Colon", settings.drawColonBetweenHourAndMinute) {
            onSettingsChanged(settings.copy(drawColonBetweenHourAndMinute = it))
        }

        SectionHeader("Display")

        SettingToggle("Tick Marks in Ambient", settings.showTickMarksOnAmbient) {
            onSettingsChanged(settings.copy(showTickMarksOnAmbient = it))
        }
        SettingToggle("Outer Ring Represents 24 Hours", settings.outerRing24Hours) {
            onSettingsChanged(settings.copy(outerRing24Hours = it))
        }
        SettingToggle("Now Indicator", settings.showNowIndicatorOnTickmarks) {
            onSettingsChanged(settings.copy(showNowIndicatorOnTickmarks = it))
        }
        SettingToggle("Show Date", settings.showDateOnFace) {
            onSettingsChanged(settings.copy(showDateOnFace = it))
        }
        SettingToggle("Date in Ambient", settings.showDateInAmbient) {
            onSettingsChanged(settings.copy(showDateInAmbient = it))
        }

        SectionHeader("Calendar")

        SettingToggle("Show Calendar Events", settings.showCalendarEvents) {
            onSettingsChanged(settings.copy(showCalendarEvents = it))
        }
        SettingToggle("Events in Ambient", settings.showCalendarEventsInAmbient) {
            onSettingsChanged(settings.copy(showCalendarEventsInAmbient = it))
        }
        SettingToggle("White Events in Ambient", settings.calendarEventColorWhiteInAmbient) {
            onSettingsChanged(settings.copy(calendarEventColorWhiteInAmbient = it))
        }
        SettingSlider(
            label = "Event Band Thickness",
            value = settings.calendarEventThickness,
            range = WatchFaceSettings.MIN_EVENT_THICKNESS..WatchFaceSettings.MAX_EVENT_THICKNESS
        ) {
            onSettingsChanged(settings.copy(calendarEventThickness = it))
        }

        Button(
            onClick = onNavigateToCalendarPicker,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Select Calendars")
        }
    }
}

@Composable
private fun SectionHeader(title: String, topSpacing: Dp = 12.dp) {
    Spacer(modifier = Modifier.height(topSpacing))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
}

@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChangeFinished: (Int) -> Unit
) {
    // Track the drag locally so we only sync to the watch once the user lets go
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = sliderValue.roundToInt().toString(), style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChangeFinished(sliderValue.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // The whole row is the touch target, so the Switch itself can skip its 48dp minimum size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}
