package com.waylo.app.ui.explore

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloGradients

private data class AdventureNode(
    val x: Float,
    val y: Float,
    val label: String,
)

private val adventureNodes = listOf(
    AdventureNode(x = 0.14f, y = 0.80f, label = "Home"),
    AdventureNode(x = 0.42f, y = 0.58f, label = "Forest Trail"),
    AdventureNode(x = 0.66f, y = 0.72f, label = "Riverside"),
    AdventureNode(x = 0.88f, y = 0.32f, label = "Mountain Pass"),
)

private val pathStrokeWidth = 6.dp
private val nodeRadius = 9.dp
private val labelStrokeWidth = 2.dp

@Composable
fun AdventurePathIllustration(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(color = WayloColors.OnSecondaryText)
    val pathBrush = WayloGradients.signature
    val currentNodeFill = WayloColors.Primary
    val currentNodeStroke = WayloColors.Cyan
    val lockedNodeFill = WayloColors.SurfaceElevated
    val lockedNodeStroke = WayloColors.OnSecondaryText
    val haloColor = WayloColors.Primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = adventureNodes.map { Offset(it.x * width, it.y * height) }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val current = points[index]
                val controlX = previous.x + (current.x - previous.x) * CONTROL_BIAS
                cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
            }
        }

        drawPath(
            path = path,
            brush = pathBrush,
            alpha = PATH_ALPHA,
            style = Stroke(width = pathStrokeWidth.toPx(), cap = StrokeCap.Round),
        )

        points.forEachIndexed { index, point ->
            val isCurrent = index == CURRENT_NODE_INDEX
            val radius = nodeRadius.toPx()

            drawCircle(
                color = haloColor.copy(alpha = if (isCurrent) CURRENT_HALO_ALPHA else LOCKED_HALO_ALPHA),
                radius = radius * HALO_SCALE,
                center = point,
            )
            drawCircle(
                color = if (isCurrent) currentNodeFill else lockedNodeFill,
                radius = radius,
                center = point,
            )
            drawCircle(
                color = if (isCurrent) currentNodeStroke else lockedNodeStroke,
                radius = radius,
                center = point,
                style = Stroke(width = labelStrokeWidth.toPx()),
            )

            val layout = textMeasurer.measure(
                text = AnnotatedString(adventureNodes[index].label),
                style = labelStyle,
            )
            val maxLabelX = (width - layout.size.width).coerceAtLeast(0f)
            val maxLabelY = (height - layout.size.height).coerceAtLeast(0f)
            val labelX = (point.x - layout.size.width / 2f).coerceIn(0f, maxLabelX)
            val labelY = (point.y + radius * LABEL_OFFSET_SCALE).coerceIn(0f, maxLabelY)

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(labelX, labelY),
            )
        }
    }
}

private const val CONTROL_BIAS = 0.5f
private const val PATH_ALPHA = 0.45f
private const val CURRENT_NODE_INDEX = 0
private const val CURRENT_HALO_ALPHA = 0.28f
private const val LOCKED_HALO_ALPHA = 0.12f
private const val HALO_SCALE = 2.2f
private const val LABEL_OFFSET_SCALE = 2.6f
