package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Immutable
data class BarDatum(
    val label: String,
    val valueText: String,
    val value: Float,
    val color: Color,
)

/** Simple month-over-month bar chart drawn on a Canvas. */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (bars.isEmpty()) return
    val maxValue = (bars.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1e-3f)

    Column(
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = bar.valueText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / bars.size
            val barWidth = slot * 0.4f
            bars.forEachIndexed { index, bar ->
                val centerX = slot * index + slot / 2f
                val fraction = (bar.value / maxValue).coerceIn(0f, 1f)
                val barHeight = size.height * fraction
                drawRoundRect(
                    color = bar.color,
                    topLeft = Offset(centerX - barWidth / 2f, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
