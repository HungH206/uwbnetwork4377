package com.example.uwbextracredit4377.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun DistanceSparkline(
    history: List<Float>,
    currentZone: ProximityZone?,
    modifier: Modifier = Modifier
) {
    val accentColor = currentZone?.color ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Distance Trend (Last 20 Samples)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (history.isNotEmpty()) {
                Text(
                    text = "Min: %.2fm  Max: %.2fm".format(history.minOrNull() ?: 0f, history.maxOrNull() ?: 0f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(top = 8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (history.isEmpty()) return@Canvas

                val minRange = 0.0f
                val maxRange = 4.0f // up to 4 meters scale
                val w = size.width
                val h = size.height

                // Draw background threshold grid lines (1m and 3m zones)
                val y1m = h - ((1.0f - minRange) / (maxRange - minRange)) * h
                val y3m = h - ((3.0f - minRange) / (maxRange - minRange)) * h

                // 1.0m line (Immediate / Near boundary)
                drawLine(
                    color = Color(0xFF1B5E20).copy(alpha = 0.25f),
                    start = Offset(0f, y1m),
                    end = Offset(w, y1m),
                    strokeWidth = 1.dp.toPx()
                )

                // 3.0m line (Near / Far boundary)
                drawLine(
                    color = Color(0xFFB71C1C).copy(alpha = 0.25f),
                    start = Offset(0f, y3m),
                    end = Offset(w, y3m),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw bars for each history point
                val barCount = history.size
                val barSpacing = 4.dp.toPx()
                val totalSpacing = barSpacing * (barCount - 1).coerceAtLeast(0)
                val barWidth = ((w - totalSpacing) / barCount.coerceAtLeast(1)).coerceAtLeast(4f)

                val path = Path()
                var firstPoint = true

                history.forEachIndexed { index, dist ->
                    val clamped = dist.coerceIn(minRange, maxRange)
                    val barHeight = ((clamped - minRange) / (maxRange - minRange)) * h
                    val x = index * (barWidth + barSpacing)
                    val y = h - barHeight

                    val barZoneColor = when {
                        dist < 1.0f -> Color(0xFF2E7D32)
                        dist <= 3.0f -> Color(0xFFF57C00)
                        else -> Color(0xFFD32F2F)
                    }

                    // Bar with rounded top
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barZoneColor.copy(alpha = 0.85f), barZoneColor.copy(alpha = 0.35f)),
                            startY = y,
                            endY = h
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Continuous sparkline connecting the peaks
                    val centerX = x + (barWidth / 2f)
                    if (firstPoint) {
                        path.moveTo(centerX, y)
                        firstPoint = false
                    } else {
                        path.lineTo(centerX, y)
                    }
                }

                // Draw peak outline
                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
