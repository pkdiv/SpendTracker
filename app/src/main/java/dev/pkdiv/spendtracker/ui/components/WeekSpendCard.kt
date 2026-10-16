package dev.pkdiv.spendtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.ui.Formatters
import java.math.BigDecimal

/** One week (Mon–Sun) of spend, ordered most-recent-first by [weeksAgo] (0 = this week). */
@Immutable
data class WeekSpend(
    val weeksAgo: Int,
    val rangeLabel: String,
    val totalSpend: BigDecimal,
    val transactionCount: Int,
    val dailySpend: List<BigDecimal>,
)

/**
 * Swipeable week card: one page per week, from this week back through the last
 * four weeks. Each page is a smaller sibling of [HeroSpendCard] with a Mon–Sun
 * bar chart; dots below hint that more weeks are a swipe away.
 */
@Composable
fun WeekSpendPager(
    weeks: List<WeekSpend>,
    dayLabels: List<String>,
    modifier: Modifier = Modifier,
    chartDescription: String? = null,
) {
    if (weeks.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { weeks.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
        ) { page ->
            WeekSpendPage(
                week = weeks[page],
                dayLabels = dayLabels,
                chartDescription = chartDescription,
            )
        }

        if (weeks.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            PageIndicator(
                count = weeks.size,
                current = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun WeekSpendPage(
    week: WeekSpend,
    dayLabels: List<String>,
    chartDescription: String?,
) {
    val label = when (week.weeksAgo) {
        0 -> stringResource(R.string.home_total_spend_week)
        1 -> stringResource(R.string.home_week_last)
        else -> stringResource(R.string.home_week_ago, week.weeksAgo)
    }
    val amount = Formatters.amountParts(week.totalSpend)
    val caption = stringResource(
        R.string.home_week_caption,
        week.rangeLabel,
        stringResource(R.string.home_transactions_today, week.transactionCount),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = amount.whole,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = amount.paise,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeekBars(
                values = week.dailySpend.map { it.toFloat() },
                labels = dayLabels,
                contentDescription = chartDescription,
            )
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun WeekBars(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .drawWithCache {
                    val slot = if (values.isEmpty()) 0f else size.width / values.size
                    val barWidth = slot * 0.5f
                    val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
                    val stub = 2.dp.toPx()
                    val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1e-3f)
                    val bars = values.mapIndexed { index, value ->
                        val centerX = slot * index + slot / 2f
                        val height = (size.height * (value / maxValue)).coerceIn(stub, size.height)
                        Triple(centerX, height, value > 0f)
                    }
                    onDrawBehind {
                        bars.forEach { (centerX, barHeight, filled) ->
                            drawRoundRect(
                                color = if (filled) barColor else emptyColor,
                                topLeft = Offset(centerX - barWidth / 2f, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = corner,
                            )
                        }
                    }
                },
        ) { }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
