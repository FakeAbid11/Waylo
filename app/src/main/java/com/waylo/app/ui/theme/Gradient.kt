package com.waylo.app.ui.theme

import androidx.compose.ui.graphics.Brush

object WayloGradients {

    val signature: Brush
        get() = Brush.horizontalGradient(signatureStops)

    val signatureVertical: Brush
        get() = Brush.verticalGradient(signatureStops)

    fun signatureMuted(alpha: Float = MUTED_ALPHA): Brush {
        return Brush.horizontalGradient(signatureStops.map { it.copy(alpha = alpha) })
    }

    private val signatureStops = listOf(WayloColors.Cyan, WayloColors.Primary, WayloColors.Secondary)

    private const val MUTED_ALPHA = 0.35f
}
