package com.derekantrican.calendarwatchface.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CalendarUpdateReceiver : BroadcastReceiver() {

    var onCalendarChanged: (() -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        onCalendarChanged?.invoke()
    }
}
