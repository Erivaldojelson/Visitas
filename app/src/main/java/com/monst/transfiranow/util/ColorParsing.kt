package com.monst.transfiranow.util

import androidx.compose.ui.graphics.Color

fun parseColor(hex: String): Color =
    Color(runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(0xFF6750A4.toInt()))

