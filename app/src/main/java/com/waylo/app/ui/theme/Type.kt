package com.waylo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object WayloTextStyles {
    val display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    )

    val largeNumber = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    )

    val heading = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )

    val title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )

    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    val caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    val label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
}

val WayloTypography = Typography(
    displayLarge = WayloTextStyles.display,
    displayMedium = WayloTextStyles.largeNumber,
    displaySmall = WayloTextStyles.heading,
    headlineLarge = WayloTextStyles.heading,
    headlineMedium = WayloTextStyles.heading.copy(fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = WayloTextStyles.title.copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = WayloTextStyles.title,
    titleMedium = WayloTextStyles.title.copy(fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = WayloTextStyles.title.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = WayloTextStyles.body,
    bodyMedium = WayloTextStyles.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = WayloTextStyles.caption,
    labelLarge = WayloTextStyles.label.copy(fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = WayloTextStyles.label,
    labelSmall = WayloTextStyles.caption.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
)
