package com.derekantrican.calendarwatchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.derekantrican.calendarwatchface.shared.CalendarEvent
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CalendarWatchFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    private val watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer2<CalendarWatchFaceRenderer.SharedAssets>(
    surfaceHolder = surfaceHolder,
    currentUserStyleRepository = currentUserStyleRepository,
    watchState = watchState,
    canvasType = CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 1000L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    class SharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    override suspend fun createSharedAssets(): SharedAssets = SharedAssets()

    private var settings = WatchFaceSettings()

    // Hardcoded test events (same as original C# code)
    private var events: List<CalendarEvent> = createTestEvents()

    private val hourPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 5f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        textSize = 80f
        typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
    }

    private val minutePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 3f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        textSize = 80f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    private val secondPaint = Paint().apply {
        setARGB(255, 255, 0, 0)
        strokeWidth = 2f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        textSize = 30f
    }

    private val tickPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val datePaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        isAntiAlias = true
    }

    fun updateSettings(newSettings: WatchFaceSettings) {
        settings = newSettings
    }

    fun updateEvents(newEvents: List<CalendarEvent>) {
        events = newEvents
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        val width = bounds.width()
        val height = bounds.height()
        val centerX = width / 2f
        val centerY = height / 2f
        val isAmbient = renderParameters.drawMode == androidx.wear.watchface.DrawMode.AMBIENT

        canvas.drawColor(Color.BLACK)

        // In ambient, the system shifts the screen a few px for burn-in protection. Shrink
        // the face toward the center so the edges (ticks, event bands) aren't shifted off-screen.
        val burnInInset = isAmbient && watchState.hasBurnInProtection
        if (burnInInset) {
            val scale = (centerX - BURN_IN_INSET_PX) / centerX
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY)
        }

        // Draw tick marks
        if (settings.showTickMarksOnAmbient || !isAmbient) {
            drawTickMarks(canvas, centerX, centerY)
        }

        // Draw now indicator
        if (settings.showNowIndicatorOnTickmarks && !isAmbient) {
            drawNowIndicator(canvas, centerX, centerY, zonedDateTime)
        }

        // Draw calendar event arcs
        if (settings.showCalendarEvents && (settings.showCalendarEventsInAmbient || !isAmbient)) {
            drawEventArcs(canvas, centerX, centerY, zonedDateTime, isAmbient)
        }

        // Draw clock
        if (settings.analogClock) {
            drawAnalogClock(canvas, centerX, centerY, zonedDateTime, isAmbient)
        } else {
            drawDigitalClock(canvas, centerX, centerY, zonedDateTime, isAmbient)
        }

        if (burnInInset) {
            canvas.restore()
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        canvas.drawColor(Color.BLACK)
    }

    private fun drawTickMarks(canvas: Canvas, centerX: Float, centerY: Float) {
        val outerRadius = centerX
        val numberOfTicks = 24

        for (i in 0 until numberOfTicks) {
            // 24h ring: every tick is an hour. 12h ring: odd ticks are half-hours (shorter).
            val innerRadius = if (i % 2 == 0 || settings.outerRing24Hours) {
                centerX - 15f
            } else {
                centerX - 10f
            }

            val angle = i * Math.PI * 2.0 / numberOfTicks
            val innerX = sin(angle).toFloat() * innerRadius
            val innerY = -cos(angle).toFloat() * innerRadius
            val outerX = sin(angle).toFloat() * outerRadius
            val outerY = -cos(angle).toFloat() * outerRadius

            canvas.drawLine(
                centerX + innerX, centerY + innerY,
                centerX + outerX, centerY + outerY,
                tickPaint
            )
        }
    }

    private fun drawNowIndicator(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        zonedDateTime: ZonedDateTime
    ) {
        val angle = getHourHandAngle(zonedDateTime, use24hours = settings.outerRing24Hours)
        val dist = centerX - 25f
        val indicatorPaint = Paint().apply { color = Color.WHITE }
        canvas.drawCircle(
            centerX + sin(angle) * dist,
            centerY + (-cos(angle) * dist).toFloat(),
            5f,
            indicatorPaint
        )
    }

    private fun drawEventArcs(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        // The ring covers the whole day (24h) or only the current AM/PM half (12h)
        val startOfDay = zonedDateTime.toLocalDate().atStartOfDay(zonedDateTime.zone)
        val (windowStart, windowEnd) = when {
            settings.outerRing24Hours -> startOfDay to startOfDay.plusDays(1)
            zonedDateTime.hour < 12 -> startOfDay to startOfDay.plusHours(12)
            else -> startOfDay.plusHours(12) to startOfDay.plusDays(1)
        }.let { (start, end) -> start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli() }
        val windowLength = (windowEnd - windowStart).toFloat()

        // Clip events to the window so ones spanning noon/midnight show only the visible part
        val visibleEvents = events.mapNotNull { event ->
            val start = max(event.startTimeMillis, windowStart)
            val end = min(event.endTimeMillis, windowEnd)
            if (start < end) event.copy(startTimeMillis = start, endTimeMillis = end) else null
        }
        if (visibleEvents.isEmpty()) return

        val arcThickness = settings.calendarEventThickness.toFloat()
        val levelSpacing = arcThickness + WatchFaceSettings.EVENT_RING_GAP
        val baseOffset = arcThickness / 2f + WatchFaceSettings.EVENT_RING_GAP / 2f
        val maxRings = 4

        // Assign ring levels based on overlap detection
        val ringAssignments = assignRingLevels(visibleEvents, maxRings)

        for ((event, ring) in ringAssignments) {
            val level = baseOffset + ring * levelSpacing
            val radius = centerX - level

            val color = if (settings.calendarEventColorWhiteInAmbient && isAmbient) {
                Color.WHITE
            } else {
                event.color
            }

            val eventPaint = Paint().apply {
                this.color = color
                strokeWidth = arcThickness
                isAntiAlias = true
                style = Paint.Style.STROKE
            }

            // drawArc measures from 3 o'clock; subtract 90 to start from 12 o'clock
            val startDeg = (event.startTimeMillis - windowStart) / windowLength * 360f - 90f
            val sweepDeg = (event.endTimeMillis - event.startTimeMillis) / windowLength * 360f

            val oval = RectF(
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius
            )
            canvas.drawArc(oval, startDeg, sweepDeg, false, eventPaint)
        }
    }

    // Assigns each event to a concentric ring level, avoiding overlaps within the same ring
    private fun assignRingLevels(
        events: List<CalendarEvent>,
        maxRings: Int
    ): List<Pair<CalendarEvent, Int>> {
        val sorted = events.sortedBy { it.startTimeMillis }
        val ringEnds = IntArray(maxRings) { 0 } // tracks latest end time per ring (as index into sorted)
        val ringEndTimes = LongArray(maxRings) { 0L }
        val result = mutableListOf<Pair<CalendarEvent, Int>>()

        for (event in sorted) {
            // Find the first ring where this event doesn't overlap
            var assignedRing = -1
            for (ring in 0 until maxRings) {
                if (event.startTimeMillis >= ringEndTimes[ring]) {
                    assignedRing = ring
                    break
                }
            }
            // If no ring available, use the last ring (stack overflow gracefully)
            if (assignedRing == -1) assignedRing = maxRings - 1

            ringEndTimes[assignedRing] = event.endTimeMillis
            result.add(event to assignedRing)
        }

        return result
    }

    private fun drawAnalogClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        if (settings.showDateOnFace && (settings.showDateInAmbient || !isAmbient)) {
            // Date above center for analog mode (placeholder for future)
        }

        val hrAngle = getHourHandAngle(zonedDateTime, use24hours = settings.outerRing24Hours)
        val minAngle = getMinuteHandAngle(zonedDateTime)

        val hrLength = centerX - 80f
        val minLength = centerX - 40f

        // Hour hand
        canvas.drawLine(
            centerX, centerY,
            centerX + sin(hrAngle) * hrLength,
            centerY + (-cos(hrAngle) * hrLength).toFloat(),
            hourPaint
        )

        // Minute hand
        canvas.drawLine(
            centerX, centerY,
            centerX + sin(minAngle) * minLength,
            centerY + (-cos(minAngle) * minLength).toFloat(),
            minutePaint
        )

        // Second hand (interactive only)
        if (!isAmbient) {
            val secAngle = getSecondHandAngle(zonedDateTime)
            val secLength = centerX - 20f
            canvas.drawLine(
                centerX, centerY,
                centerX + sin(secAngle) * secLength,
                centerY + (-cos(secAngle) * secLength).toFloat(),
                secondPaint
            )
        }
    }

    private fun drawDigitalClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        zonedDateTime: ZonedDateTime,
        isAmbient: Boolean
    ) {
        val hour = zonedDateTime.hour
        val minute = zonedDateTime.minute
        val second = zonedDateTime.second

        val displayHour = if (settings.militaryTime) hour else {
            val h = hour % 12
            if (h == 0) 12 else h
        }
        val hourStr = if (settings.hourLeadingZero) {
            String.format(Locale.US, "%02d", displayHour)
        } else {
            displayHour.toString()
        }
        val minStr = String.format(Locale.US, "%02d", minute)
        val secStr = String.format(Locale.US, "%02d", second)

        val segments = mutableListOf<Pair<String, Paint>>()
        segments.add(hourStr to hourPaint)

        if (settings.drawColonBetweenHourAndMinute) {
            val colonPaint = Paint().apply {
                color = Color.WHITE
                textSize = 60f
                isAntiAlias = true
            }
            segments.add(":" to colonPaint)
        }

        segments.add(minStr to minutePaint)

        if (!isAmbient) {
            segments.add(secStr to secondPaint)
        }

        drawMultipleTextCenter(canvas, segments, centerX, centerY)

        // Draw date
        if (settings.showDateOnFace && (settings.showDateInAmbient || !isAmbient)) {
            val formatter = DateTimeFormatter.ofPattern(settings.dateFormat, Locale.US)
            val dateStr = zonedDateTime.format(formatter)
            val timeBounds = measureMultipleText(segments)
            val dateBounds = Rect()
            datePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)
            val padding = 15f

            drawMultipleTextCenter(
                canvas,
                listOf(dateStr to datePaint),
                centerX,
                centerY - timeBounds.height() / 2f - dateBounds.height() / 2f - padding
            )
        }
    }

    private fun drawMultipleTextCenter(
        canvas: Canvas,
        segments: List<Pair<String, Paint>>,
        refX: Float,
        refY: Float
    ) {
        val widths = segments.map { (text, paint) -> paint.measureText(text) }
        val totalWidth = widths.sum()
        val maxHeight = segments.maxOf { (text, paint) ->
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            bounds.height().toFloat()
        }

        var x = refX - totalWidth / 2f
        for ((i, segment) in segments.withIndex()) {
            val (text, paint) = segment
            val saved = paint.textAlign
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(text, x, refY + maxHeight / 2f, paint)
            paint.textAlign = saved
            x += widths[i]
        }
    }

    private fun measureMultipleText(segments: List<Pair<String, Paint>>): Rect {
        val rect = Rect()
        var totalWidth = 0
        var maxHeight = 0
        for ((text, paint) in segments) {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            totalWidth += bounds.width()
            maxHeight = max(maxHeight, bounds.height())
        }
        rect.right = totalWidth
        rect.top = -maxHeight
        return rect
    }

    // Angle from 12 o'clock, clockwise, in radians
    private fun getHourHandAngle(
        time: ZonedDateTime,
        use24hours: Boolean = false
    ): Float {
        val hour = time.hour.toFloat()
        val minute = time.minute.toFloat()
        val divisor = if (use24hours) 12f else 6f
        return ((hour + minute / 60f) / divisor) * Math.PI.toFloat()
    }

    private fun getMinuteHandAngle(time: ZonedDateTime): Float {
        return time.minute / 30f * Math.PI.toFloat()
    }

    private fun getSecondHandAngle(time: ZonedDateTime): Float {
        return time.second / 30f * Math.PI.toFloat()
    }

    private fun createTestEvents(): List<CalendarEvent> {
        val today = java.time.LocalDate.now()
        fun timeMillis(hour: Int, minute: Int): Long {
            return today.atTime(hour, minute)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        return listOf(
            CalendarEvent(
                title = "Event 1",
                startTimeMillis = timeMillis(2, 30),
                endTimeMillis = timeMillis(6, 0),
                color = Color.GREEN,
                calendarId = 0
            ),
            CalendarEvent(
                title = "Event 2",
                startTimeMillis = timeMillis(1, 0),
                endTimeMillis = timeMillis(4, 0),
                color = Color.rgb(173, 216, 230), // LightBlue
                calendarId = 0
            ),
            CalendarEvent(
                title = "Event 3",
                startTimeMillis = timeMillis(2, 30),
                endTimeMillis = timeMillis(4, 30),
                color = Color.rgb(255, 69, 0), // OrangeRed
                calendarId = 0
            )
        )
    }

    companion object {
        // Wear OS burn-in protection shifts ambient content by up to ~10px
        private const val BURN_IN_INSET_PX = 10f
    }
}
