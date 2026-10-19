package com.derekantrican.calendarwatchface.mobile.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.derekantrican.calendarwatchface.shared.WatchFaceSettings
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Mirrors CalendarWatchFaceRenderer (interactive mode). Sizes are in watch px
// relative to a 390px display and scaled to the preview's size.
private const val WATCH_SIZE_PX = 390f

@Composable
fun PreviewScreen(
    settings: WatchFaceSettings,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L - now.nano / 1_000_000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(Color.Black)
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val scale = size.width / WATCH_SIZE_PX

            drawTickMarks(settings, centerX, centerY, scale)

            if (settings.showNowIndicatorOnTickmarks) {
                drawNowIndicator(settings, now, centerX, centerY, scale)
            }

            if (settings.showCalendarEvents) {
                drawDummyEventArcs(settings, centerX, centerY, scale)
            }

            if (settings.analogClock) {
                drawAnalogHands(settings, now, centerX, centerY, scale)
            } else {
                drawDigitalTime(settings, now, centerX, centerY, scale)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Preview uses sample events",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawTickMarks(settings: WatchFaceSettings, cx: Float, cy: Float, scale: Float) {
    val outerR = cx
    val numberOfTicks = 24

    for (i in 0 until numberOfTicks) {
        val innerR = if (i % 2 == 0 || settings.outerRing24Hours) cx - 15f * scale else cx - 10f * scale
        val angle = i * Math.PI * 2.0 / numberOfTicks

        drawLine(
            color = Color.White,
            start = Offset(
                cx + (sin(angle) * innerR).toFloat(),
                cy + (-cos(angle) * innerR).toFloat()
            ),
            end = Offset(
                cx + (sin(angle) * outerR).toFloat(),
                cy + (-cos(angle) * outerR).toFloat()
            ),
            strokeWidth = 2f * scale
        )
    }
}

private fun DrawScope.drawNowIndicator(settings: WatchFaceSettings, now: LocalDateTime, cx: Float, cy: Float, scale: Float) {
    val angle = hourAngle(now, settings.outerRing24Hours)
    val dist = cx - 25f * scale
    drawCircle(
        color = Color.White,
        radius = 5f * scale,
        center = Offset(
            cx + (sin(angle) * dist).toFloat(),
            cy + (-cos(angle) * dist).toFloat()
        )
    )
}

private fun DrawScope.drawDummyEventArcs(settings: WatchFaceSettings, cx: Float, cy: Float, scale: Float) {
    data class DummyEvent(val startHour: Float, val endHour: Float, val color: Color)

    val events = listOf(
        DummyEvent(2.5f, 6f, Color.Green),
        DummyEvent(1f, 4f, Color(0xFFADD8E6)),
        DummyEvent(2.5f, 4.5f, Color(0xFFFF4500))
    )

    val arcThickness = settings.calendarEventThickness * scale
    val levelSpacing = arcThickness + WatchFaceSettings.EVENT_RING_GAP * scale
    val baseOffset = arcThickness / 2f + WatchFaceSettings.EVENT_RING_GAP / 2f * scale
    val ringHours = if (settings.outerRing24Hours) 24f else 12f

    for ((index, event) in events.withIndex()) {
        val radius = cx - baseOffset - index * levelSpacing

        // Angles from 3 o'clock reference (for drawArc)
        val startDeg = event.startHour / ringHours * 360f - 90f
        val endDeg = event.endHour / ringHours * 360f - 90f

        drawArc(
            color = event.color,
            startAngle = startDeg,
            sweepAngle = endDeg - startDeg,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = arcThickness)
        )
    }
}

private fun DrawScope.drawAnalogHands(
    settings: WatchFaceSettings,
    now: LocalDateTime,
    cx: Float,
    cy: Float,
    scale: Float
) {
    val hrAngle = hourAngle(now, settings.outerRing24Hours)
    val minAngle = now.minute / 30.0 * Math.PI
    val secAngle = now.second / 30.0 * Math.PI

    drawHand(cx, cy, hrAngle, cx - 80f * scale, Color.White, 5f * scale)
    drawHand(cx, cy, minAngle, cx - 40f * scale, Color.White, 3f * scale)
    drawHand(cx, cy, secAngle, cx - 20f * scale, Color.Red, 2f * scale)
}

private fun DrawScope.drawHand(cx: Float, cy: Float, angle: Double, length: Float, color: Color, width: Float) {
    drawLine(
        color = color,
        start = Offset(cx, cy),
        end = Offset(
            cx + (sin(angle) * length).toFloat(),
            cy + (-cos(angle) * length).toFloat()
        ),
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDigitalTime(
    settings: WatchFaceSettings,
    now: LocalDateTime,
    cx: Float,
    cy: Float,
    scale: Float
) {
    val displayHour = if (settings.militaryTime) now.hour else {
        val h = now.hour % 12
        if (h == 0) 12 else h
    }
    val hourStr = if (settings.hourLeadingZero) String.format(Locale.US, "%02d", displayHour) else displayHour.toString()
    val minStr = String.format(Locale.US, "%02d", now.minute)
    val secStr = String.format(Locale.US, "%02d", now.second)

    val segments = mutableListOf(
        hourStr to textPaint(80f * scale, android.graphics.Color.WHITE, Typeface.create("sans-serif-light", Typeface.BOLD))
    )
    if (settings.drawColonBetweenHourAndMinute) {
        segments.add(":" to textPaint(60f * scale, android.graphics.Color.WHITE))
    }
    segments.add(minStr to textPaint(80f * scale, android.graphics.Color.WHITE, Typeface.create("sans-serif-light", Typeface.NORMAL)))
    segments.add(secStr to textPaint(30f * scale, android.graphics.Color.RED))

    drawTextSegmentsCentered(segments, cx, cy)

    if (settings.showDateOnFace) {
        val dateStr = try {
            now.format(DateTimeFormatter.ofPattern(settings.dateFormat, Locale.US))
        } catch (_: IllegalArgumentException) {
            return
        }
        val datePaint = textPaint(30f * scale, android.graphics.Color.WHITE)
        val timeHeight = segments.maxOf { (text, paint) -> textHeight(text, paint) }
        val dateHeight = textHeight(dateStr, datePaint)
        drawTextSegmentsCentered(
            listOf(dateStr to datePaint),
            cx,
            cy - timeHeight / 2f - dateHeight / 2f - 15f * scale
        )
    }
}

private fun textPaint(size: Float, color: Int, typeface: Typeface? = null) = Paint().apply {
    this.color = color
    textSize = size
    isAntiAlias = true
    typeface?.let { this.typeface = it }
}

private fun textHeight(text: String, paint: Paint): Float {
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    return bounds.height().toFloat()
}

// Draws text segments side by side, centered on (refX, refY)
private fun DrawScope.drawTextSegmentsCentered(segments: List<Pair<String, Paint>>, refX: Float, refY: Float) {
    val widths = segments.map { (text, paint) -> paint.measureText(text) }
    val maxHeight = segments.maxOf { (text, paint) -> textHeight(text, paint) }
    var x = refX - widths.sum() / 2f
    for ((i, segment) in segments.withIndex()) {
        val (text, paint) = segment
        drawContext.canvas.nativeCanvas.drawText(text, x, refY + maxHeight / 2f, paint)
        x += widths[i]
    }
}

// Angle from 12 o'clock, clockwise, in radians
private fun hourAngle(time: LocalDateTime, use24hours: Boolean): Double {
    val divisor = if (use24hours) 12.0 else 6.0
    return ((time.hour + time.minute / 60.0) / divisor) * Math.PI
}
