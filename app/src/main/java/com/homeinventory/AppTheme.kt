package com.homeinventory
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
private val LightColors = lightColorScheme(
primary = Color(0xFF006C4C),
onPrimary = Color.White,
surfaceVariant = Color(0xFFF1F1F1),
background = Color(0xFFFAFAFA)
)
private val DarkColors = darkColorScheme(
primary = Color(0xFF42E8AA),
onPrimary = Color(0xFF003826),
surfaceVariant = Color(0xFF2D2D2D),
background = Color(0xFF121212)
)
@Composable
fun AppTheme(
darkTheme: Boolean = isSystemInDarkTheme(),
content: @Composable () -> Unit
) {
val colorScheme = if (darkTheme) DarkColors else LightColors
val view = LocalView.current
if (!view.isInEditMode) {
SideEffect {
val window = (view.context as Activity).window
window.statusBarColor = colorScheme.background.toArgb()
}
}
MaterialTheme(colorScheme = colorScheme, content = content)
}