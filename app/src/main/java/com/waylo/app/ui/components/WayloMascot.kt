package com.waylo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloGradients

@Composable
fun WayloMascot(
    modifier: Modifier = Modifier,
    size: Dp = WayloDimens.mascotSize,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
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
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = WayloColors.Cyan,
                modifier = Modifier.size(size * ICON_SCALE),
            )
        }
    }
}

private const val RING_INSET = 0.94f
private const val ICON_SCALE = 0.45f
