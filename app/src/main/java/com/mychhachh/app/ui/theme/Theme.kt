package com.mychhachh.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

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

object LiveJellyTheme {
    var accent by mutableStateOf(JellyPink)
    var accent2 by mutableStateOf(JellyPurple)
    var text by mutableStateOf(JellyInk)
    var muted by mutableStateOf(JellyMuted)
    var cardOpacity by mutableFloatStateOf(.78f)
    var headerOpacity by mutableFloatStateOf(.78f)
    var cardRadius by mutableFloatStateOf(30f)
    var headerRadius by mutableFloatStateOf(28f)
    var navHeight by mutableFloatStateOf(76f)
    var buttonHeight by mutableFloatStateOf(44f)
    var fontScale by mutableFloatStateOf(1f)
    var profileCoverHeight by mutableFloatStateOf(165f)
    var profileAvatarSize by mutableFloatStateOf(96f)
    var shopCoverHeight by mutableFloatStateOf(185f)
    var shopAvatarSize by mutableFloatStateOf(96f)
    var rainbowBrand by mutableStateOf(true)
    var motion by mutableStateOf(true)

    private fun color(raw: String, fallback: Color): Color {
        val v = raw.trim()
        if (!Regex("^#[0-9a-fA-F]{6}$").matches(v)) return fallback
        return runCatching {
            val rgb = v.removePrefix("#").toLong(16)
            Color(0xFF000000L or rgb)
        }.getOrDefault(fallback)
    }

    fun apply(settings: JSONObject?) {
        if (settings == null) return
        accent = color(settings.optString("theme_accent", ""), JellyPink)
        accent2 = color(settings.optString("theme_accent2", ""), JellyPurple)
        text = color(settings.optString("theme_text_color", ""), JellyInk)
        muted = color(settings.optString("theme_muted_color", ""), JellyMuted)
        cardOpacity = (settings.optInt("theme_card_opacity", 78).coerceIn(35, 100) / 100f)
        headerOpacity = (settings.optInt("theme_header_opacity", 78).coerceIn(35, 100) / 100f)
        cardRadius = settings.optInt("theme_card_radius", 30).coerceIn(0, 60).toFloat()
        headerRadius = settings.optInt("theme_header_radius", 28).coerceIn(0, 60).toFloat()
        navHeight = settings.optInt("theme_nav_height", 76).coerceIn(44, 100).toFloat()
        buttonHeight = settings.optInt("theme_button_height", 44).coerceIn(32, 72).toFloat()
        fontScale = settings.optInt("theme_font_scale", 100).coerceIn(90, 115) / 100f
        profileCoverHeight = settings.optInt("theme_profile_cover_height", 165).coerceIn(90, 300).toFloat()
        profileAvatarSize = settings.optInt("theme_profile_avatar_size", 96).coerceIn(56, 160).toFloat()
        shopCoverHeight = settings.optInt("theme_shop_cover_height", 185).coerceIn(100, 320).toFloat()
        shopAvatarSize = settings.optInt("theme_shop_avatar_size", 96).coerceIn(60, 170).toFloat()
        rainbowBrand = settings.optInt("theme_brand_rainbow", 1) != 0
        motion = settings.optInt("theme_motion", 1) != 0
    }
}

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
