package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class DonutSlice(val label: String, val value: Float, val color: Color)

/** Category share donut. Purely Canvas; a summary string is exposed for TalkBack. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    thickness: Dp = 22.dp,
    contentDescription: String? = null,
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1e-3f)

    Canvas(
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        ),
    ) {
        val strokePx = thickness.toPx()
        val diameter = size.minDimension - strokePx
        if (diameter <= 0f) return@Canvas
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Butt)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = 360f * (slice.value / total)
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = (sweep - 3f).coerceAtLeast(0f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += sweep
        }
    }
}
