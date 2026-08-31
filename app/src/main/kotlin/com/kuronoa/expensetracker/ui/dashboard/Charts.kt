package com.kuronoa.expensetracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kuronoa.expensetracker.ui.theme.CategoryPalette
import kotlin.math.min

/** Donut chart sederhana (Canvas murni, tanpa library) untuk breakdown kategori. */
@Composable
fun DonutChart(
    slices: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = CategoryPalette
) {
    val total = slices.sumOf { it.second }
    Box(modifier = modifier.aspectRatio(1f).padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val strokeWidth = size.minDimension * 0.22f
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            var startAngle = -90f
            if (total <= 0.0) {
                drawArc(
                    color = Color.LightGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth)
                )
            } else {
                slices.forEachIndexed { index, (_, value) ->
                    val sweep = (value / total * 360.0).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweep
                }
            }
        }
    }
}

/** Bar chart horizontal sederhana untuk tren bulanan / perbandingan kategori. */
@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Double) -> String = { it.toString() }
) {
    val max = items.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Column(modifier = modifier) {
        items.forEach { (label, value) ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                        val fraction = (value / max).toFloat().coerceIn(0f, 1f)
                        drawRoundRect(
                            color = Color(0xFFEDE6D6),
                            size = Size(size.width, size.height)
                        )
                        drawRoundRect(
                            color = barColor,
                            size = Size(size.width * fraction, size.height)
                        )
                    }
                }
                Text(valueFormatter(value), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
