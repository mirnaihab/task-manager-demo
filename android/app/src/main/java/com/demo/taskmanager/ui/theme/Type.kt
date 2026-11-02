package com.demo.taskmanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, weight: FontWeight, lineHeight: Double = 1.4) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
)

// DESIGN.md scale: headline-lg 28/700, title-md 17/600, body-md 15/400, label-sm 12/500.
val Typography = Typography(
    headlineLarge = style(28, FontWeight.Bold),
    titleMedium = style(17, FontWeight.SemiBold),
    bodyMedium = style(15, FontWeight.Normal),
    labelSmall = style(12, FontWeight.Medium),
)
