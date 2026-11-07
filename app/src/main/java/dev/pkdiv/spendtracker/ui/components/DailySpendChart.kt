package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

/**
 * Day-ordered daily spend bars drawn on a Canvas (no chart library). One bar per
 * day; days with no spend are empty.
 */
@Composable
fun DailySpendChart(
    values: List<BigDecimal>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val floats = remember(values) { values.map { it.toFloat() } }
    val barColor = MaterialTheme.colorScheme.primary
    if (floats.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        val maxValue = (floats.maxOrNull() ?: 0f).coerceAtLeast(1e-3f)
        val slot = size.width / floats.size
        val barWidth = (slot * 0.5f).coerceAtMost(18.dp.toPx())
        floats.forEachIndexed { index, value ->
            val fraction = (value / maxValue).coerceIn(0f, 1f)
            val barHeight = size.height * fraction
            if (barHeight > 0f) {
                val centerX = slot * index + slot / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
    }
}
