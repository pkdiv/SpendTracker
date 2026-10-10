package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Minimal Canvas sparkline. The Path is built inside [drawWithCache] so it is only
 * recomputed when the values or the size change — never per frame.
 */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
            .drawWithCache {
                val strokePx = strokeWidth.toPx()
                val line = buildSparkPath(size, values, strokePx)
                val baselineY = size.height - strokePx / 2f
                val baselineColor = lineColor.copy(alpha = 0.25f)
                val baselineStroke = 1.dp.toPx()
                onDrawBehind {
                    drawLine(
                        color = baselineColor,
                        start = Offset(0f, baselineY),
                        end = Offset(size.width, baselineY),
                        strokeWidth = baselineStroke,
                    )
                    drawPath(
                        path = line,
                        color = lineColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            },
    ) { }
}

private fun buildSparkPath(size: Size, values: List<Float>, strokePx: Float): Path {
    val path = Path()
    if (values.size < 2 || size.width <= 0f || size.height <= 0f) return path
    val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1e-3f)
    val stepX = size.width / (values.size - 1)
    val usableHeight = (size.height - strokePx).coerceAtLeast(1f)
    values.forEachIndexed { index, value ->
        val x = index * stepX
        val fraction = (value / maxValue).coerceIn(0f, 1f)
        val y = strokePx / 2f + usableHeight * (1f - fraction)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}
