package com.brtech.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

fun qualityColor(q: String) = when (q) {
    "Excelente" -> Color(0xFF2ECC71)
    "Boa" -> Color(0xFF8BC34A)
    "Regular" -> Orange
    else -> Color(0xFFE53935)
}

@Composable
fun BrCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun SectionTitle(t: String) =
    Text(t, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

@Composable
fun Pill(text: String, color: Color) {
    Box(Modifier.background(color.copy(alpha = 0.18f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricGrid(items: List<Pair<String, String>>) {
    items.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            row.forEach { (l, v) ->
                Column(Modifier.weight(1f)) {
                    Text(l, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun LineChart(values: List<Float>, minY: Float, maxY: Float, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width
        val h = size.height
        for (i in 0..3) { val y = h * i / 3f; drawLine(grid, Offset(0f, y), Offset(w, y), 1f) }
        if (values.size < 2) return@Canvas
        val range = (maxY - minY).coerceAtLeast(0.001f)
        val step = w / (values.size - 1)
        val path = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v - minY) / range).coerceIn(0f, 1f) * h
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, h); fill.lineTo(x, y) }
            else { path.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo(w, h); fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme.primary
    val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val n = values.size
            if (n == 0) return@Canvas
            val slot = size.width / n
            val bw = slot * 0.55f
            values.forEachIndexed { i, v ->
                val bh = ((v.toFloat() / maxV) * size.height).coerceAtLeast(4f)
                drawRoundRect(
                    if (i == n - 1) c else c.copy(alpha = 0.45f),
                    topLeft = Offset(i * slot + (slot - bw) / 2, size.height - bh),
                    size = Size(bw, bh), cornerRadius = CornerRadius(8f, 8f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
