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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = accentColor,
            secondary = accentColor.copy(alpha = 0.88f),
            tertiary = accentColor.copy(alpha = 0.72f)
        )
        else -> lightColorScheme(
            primary = accentColor,
            secondary = accentColor.copy(alpha = 0.88f),
            tertiary = accentColor.copy(alpha = 0.72f)
        )
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
