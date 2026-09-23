package com.mychhachh.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JellyInk = Color(0xFF302467)
val JellyMuted = Color(0xFF727B9E)
val JellyPink = Color(0xFFFF4FAF)
val JellyPink2 = Color(0xFFFF78C4)
val JellyCyan = Color(0xFF55D8FF)
val JellyBlue = Color(0xFF5E8DFF)
val JellyPurple = Color(0xFF9070F8)
val JellyGold = Color(0xFFFFC75A)
val JellyGreen = Color(0xFF55DEB1)
val JellyBg = Color(0xFFF1FAFF)
val JellyBg2 = Color(0xFFF7F4FF)
val JellySurface = Color(0xFFFDFEFF)
val JellyOutline = Color(0xFFDCE6F6)

private val Colors = lightColorScheme(
    primary = JellyPurple,
    secondary = JellyPink,
    tertiary = JellyGreen,
    background = JellyBg,
    surface = JellySurface,
    surfaceVariant = Color(0xFFF3F7FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = JellyInk,
    onSurface = JellyInk,
    outline = JellyOutline
)

@Composable
fun MyChhachhTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
