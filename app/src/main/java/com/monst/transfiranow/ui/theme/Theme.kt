package com.monst.transfiranow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat

@Composable
fun TransfiraNowTheme(
    dynamicColor: Boolean,
    accentColor: Color,
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = accentColor,
            secondary = accentColor.copy(alpha = 0.88f),
            tertiary = accentColor.copy(alpha = 0.72f),
            surface = Color(0xFF141218),
            surfaceContainer = Color(0xFF211F26),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF35333A)
        )
        else -> lightColorScheme(
            primary = accentColor,
            secondary = accentColor.copy(alpha = 0.88f),
            tertiary = accentColor.copy(alpha = 0.72f),
            surface = Color(0xFFFFF8FC),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9)
        )
    }

    val colorScheme = if (pureBlack && darkTheme) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF050505),
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF0F0F0F),
            surfaceContainerHighest = Color(0xFF141414)
        )
    } else {
        baseScheme
    }

    (context as? Activity)?.window?.let {
        WindowCompat.setDecorFitsSystemWindows(it, false)
        it.statusBarColor = Color.Transparent.toArgb()
        it.navigationBarColor = Color.Transparent.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
