package com.waylo.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.waylo.app.core.common.MascotContent
import com.waylo.app.core.common.MascotState
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloGradients

@Composable
fun WayloMascot(
    state: MascotState = MascotState.Idle,
    modifier: Modifier = Modifier,
    size: Dp = WayloDimens.mascotSize,
    decorative: Boolean = false,
) {
    val motionEnabled = rememberMotionEnabled()
    val celebration = remember(state) { Animatable(1f) }

    LaunchedEffect(state, motionEnabled) {
        if (motionEnabled && state.isCelebratory) {
            repeat(CELEBRATION_BOUNCES) {
                celebration.animateTo(
                    targetValue = CELEBRATION_SCALE,
                    animationSpec = tween(CELEBRATION_UP_MS, easing = FastOutSlowInEasing),
                )
                celebration.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(CELEBRATION_DOWN_MS, easing = FastOutSlowInEasing),
                )
            }
        } else {
            celebration.snapTo(1f)
        }
    }

    val scale = celebration.value * breathScale(state = state, motionEnabled = motionEnabled)
    val waddle = waddleDegrees(state = state, motionEnabled = motionEnabled)
    val glowAlpha = if (state == MascotState.LevelUp) {
        ((celebration.value - 1f) / (CELEBRATION_SCALE - 1f)).coerceIn(0f, 1f) * GLOW_MAX_ALPHA
    } else {
        0f
    }

    val stateSemantics = if (decorative) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.semantics { contentDescription = MascotContent.contentDescription(state) }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(stateSemantics)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = waddle
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WayloColors.Cyan.copy(alpha = glowAlpha), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WayloGradients.signature, CircleShape),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(RING_INSET)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(WayloColors.Surface),
        ) {
            FoxFace(state = state, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) != 0f
        }.getOrDefault(true)
    }
}

@Composable
private fun breathScale(state: MascotState, motionEnabled: Boolean): Float {
    if (!motionEnabled || state !in BREATHING_STATES) return 1f
    val transition = rememberInfiniteTransition(label = "mascot-breath")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = BREATH_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(BREATH_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot-breath-scale",
    ).value
}

@Composable
private fun waddleDegrees(state: MascotState, motionEnabled: Boolean): Float {
    if (!motionEnabled || state != MascotState.Walking) return 0f
    val transition = rememberInfiniteTransition(label = "mascot-waddle")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WADDLE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot-waddle-phase",
    ).value
    return (phase - 0.5f) * 2f * WADDLE_DEGREES
}

@Composable
private fun FoxFace(state: MascotState, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val fur = WayloColors.Orange
        val dark = WayloColors.Background
        val light = WayloColors.OnBackground

        val leftEar = Path().apply {
            moveTo(px(0.21f), px(0.44f))
            lineTo(px(0.30f), px(0.07f))
            lineTo(px(0.48f), px(0.31f))
            close()
        }
        val rightEar = Path().apply {
            moveTo(px(0.79f), px(0.44f))
            lineTo(px(0.70f), px(0.07f))
            lineTo(px(0.52f), px(0.31f))
            close()
        }
        val leftInnerEar = Path().apply {
            moveTo(px(0.27f), px(0.40f))
            lineTo(px(0.31f), px(0.16f))
            lineTo(px(0.43f), px(0.31f))
            close()
        }
        val rightInnerEar = Path().apply {
            moveTo(px(0.73f), px(0.40f))
            lineTo(px(0.69f), px(0.16f))
            lineTo(px(0.57f), px(0.31f))
            close()
        }

        drawPath(leftEar, fur)
        drawPath(rightEar, fur)
        drawPath(leftInnerEar, light.copy(alpha = 0.9f))
        drawPath(rightInnerEar, light.copy(alpha = 0.9f))

        drawCircle(color = fur, radius = px(0.295f), center = center(0.5f, 0.56f))

        drawOval(
            color = light,
            topLeft = androidx.compose.ui.geometry.Offset(px(0.375f), px(0.585f)),
            size = androidx.compose.ui.geometry.Size(px(0.25f), px(0.17f)),
        )
        drawCircle(color = dark, radius = px(0.036f), center = center(0.5f, 0.625f))

        when (state) {
            MascotState.Celebrating,
            MascotState.XpEarned,
            MascotState.LevelUp,
            -> {
                drawHappyEye(left = true)
                drawHappyEye(left = false)
                drawOval(
                    color = dark,
                    topLeft = androidx.compose.ui.geometry.Offset(px(0.42f), px(0.66f)),
                    size = androidx.compose.ui.geometry.Size(px(0.16f), px(0.10f)),
                )
            }

            MascotState.Paused -> {
                drawHalfLidEye(left = true)
                drawHalfLidEye(left = false)
                drawLine(
                    color = dark,
                    start = androidx.compose.ui.geometry.Offset(px(0.46f), px(0.70f)),
                    end = androidx.compose.ui.geometry.Offset(px(0.54f), px(0.70f)),
                    strokeWidth = px(0.03f),
                    cap = StrokeCap.Round,
                )
            }

            MascotState.Resting -> {
                drawClosedEye(left = true)
                drawClosedEye(left = false)
                drawLine(
                    color = dark,
                    start = androidx.compose.ui.geometry.Offset(px(0.465f), px(0.70f)),
                    end = androidx.compose.ui.geometry.Offset(px(0.535f), px(0.70f)),
                    strokeWidth = px(0.028f),
                    cap = StrokeCap.Round,
                )
            }

            else -> {
                drawOpenEye(left = true)
                drawOpenEye(left = false)
                drawArc(
                    color = dark,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(px(0.435f), px(0.635f)),
                    size = androidx.compose.ui.geometry.Size(px(0.13f), px(0.095f)),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = px(0.035f),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.px(fraction: Float): Float =
    size.width * fraction

private fun androidx.compose.ui.graphics.drawscope.DrawScope.center(
    x: Float,
    y: Float,
): androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(
    size.width * x,
    size.height * y,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOpenEye(left: Boolean) {
    val x = if (left) 0.375f else 0.625f
    drawCircle(color = WayloColors.Background, radius = px(0.042f), center = center(x, 0.515f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHappyEye(left: Boolean) {
    val topLeftX = if (left) 0.32f else 0.57f
    drawArc(
        color = WayloColors.Background,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(px(topLeftX), px(0.455f)),
        size = androidx.compose.ui.geometry.Size(px(0.11f), px(0.115f)),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = px(0.045f),
            cap = StrokeCap.Round,
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHalfLidEye(left: Boolean) {
    val topLeftX = if (left) 0.325f else 0.575f
    drawArc(
        color = WayloColors.Background,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(px(topLeftX), px(0.475f)),
        size = androidx.compose.ui.geometry.Size(px(0.10f), px(0.09f)),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = px(0.04f),
            cap = StrokeCap.Round,
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClosedEye(left: Boolean) {
    val topLeftX = if (left) 0.325f else 0.575f
    drawArc(
        color = WayloColors.Background,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(px(topLeftX), px(0.47f)),
        size = androidx.compose.ui.geometry.Size(px(0.10f), px(0.10f)),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = px(0.04f),
            cap = StrokeCap.Round,
        ),
    )
}

private val MascotState.isCelebratory: Boolean
    get() = this == MascotState.Celebrating ||
        this == MascotState.XpEarned ||
        this == MascotState.LevelUp

private val BREATHING_STATES = setOf(
    MascotState.Idle,
    MascotState.Encouraging,
    MascotState.Streak,
)

private const val RING_INSET = 0.94f
private const val BREATH_SCALE = 1.025f
private const val BREATH_DURATION_MS = 2_600
private const val WADDLE_DEGREES = 5f
private const val WADDLE_DURATION_MS = 560
private const val CELEBRATION_BOUNCES = 2
private const val CELEBRATION_SCALE = 1.12f
private const val CELEBRATION_UP_MS = 140
private const val CELEBRATION_DOWN_MS = 180
private const val GLOW_MAX_ALPHA = 0.45f
