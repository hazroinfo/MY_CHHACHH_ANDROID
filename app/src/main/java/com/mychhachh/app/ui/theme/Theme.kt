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

private val DefaultJellyInk = Color(0xFF302467)
private val DefaultJellyMuted = Color(0xFF727B9E)
private val DefaultJellyPink = Color(0xFFFF4FAF)
private val DefaultJellyPurple = Color(0xFF9070F8)
private val DefaultJellySurface = Color(0xFFFDFEFF)
private val DefaultJellyOutline = Color(0xFFDCE6F6)

val JellyInk: Color get() = LiveJellyTheme.text
val JellyMuted: Color get() = LiveJellyTheme.muted
val JellyPink: Color get() = LiveJellyTheme.accent
val JellyPurple: Color get() = LiveJellyTheme.accent2
val JellyPink2 = Color(0xFFFF78C4)
val JellyCyan = Color(0xFF55D8FF)
val JellyBlue = Color(0xFF5E8DFF)
val JellyGold = Color(0xFFFFC75A)
val JellyGreen = Color(0xFF55DEB1)
val JellyBg = Color(0xFFF1FAFF)
val JellyBg2 = Color(0xFFF7F4FF)
val JellySurface: Color get() = LiveJellyTheme.cardColor
val JellyOutline: Color get() = LiveJellyTheme.borderColor

object LiveJellyTheme {
    var enabled by mutableStateOf(true)
    var accent by mutableStateOf(DefaultJellyPink)
    var accent2 by mutableStateOf(DefaultJellyPurple)
    var text by mutableStateOf(DefaultJellyInk)
    var muted by mutableStateOf(DefaultJellyMuted)
    var headerColor by mutableStateOf(Color.White)
    var navColor by mutableStateOf(Color.White)
    var cardColor by mutableStateOf(DefaultJellySurface)
    var borderColor by mutableStateOf(DefaultJellyOutline)
    var buttonColor by mutableStateOf(Color.White)
    var inputColor by mutableStateOf(Color.White)
    var iconColor by mutableStateOf(DefaultJellyInk)
    var activeColor by mutableStateOf(DefaultJellyPink)
    var cardOpacity by mutableFloatStateOf(.78f)
    var headerOpacity by mutableFloatStateOf(.78f)
    var navOpacity by mutableFloatStateOf(.78f)
    var inputOpacity by mutableFloatStateOf(.90f)
    var cardRadius by mutableFloatStateOf(30f)
    var headerRadius by mutableFloatStateOf(28f)
    var navRadius by mutableFloatStateOf(25f)
    var buttonRadius by mutableFloatStateOf(999f)
    var inputRadius by mutableFloatStateOf(17f)
    var shadow by mutableFloatStateOf(4f)
    var pageWidth by mutableFloatStateOf(980f)
    var cardPadding by mutableFloatStateOf(13f)
    var sectionGap by mutableFloatStateOf(9f)
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

    private fun resetVisualDefaults() {
        accent = DefaultJellyPink
        accent2 = DefaultJellyPurple
        text = DefaultJellyInk
        muted = DefaultJellyMuted
        headerColor = Color.White
        navColor = Color.White
        cardColor = DefaultJellySurface
        borderColor = DefaultJellyOutline
        buttonColor = Color.White
        inputColor = Color.White
        iconColor = DefaultJellyInk
        activeColor = DefaultJellyPink
        cardOpacity = .78f
        headerOpacity = .78f
        navOpacity = .78f
        inputOpacity = .90f
        cardRadius = 30f
        headerRadius = 28f
        navRadius = 25f
        buttonRadius = 999f
        inputRadius = 17f
        shadow = 4f
        pageWidth = 980f
        cardPadding = 13f
        sectionGap = 9f
        navHeight = 76f
        buttonHeight = 44f
        fontScale = 1f
        profileCoverHeight = 165f
        profileAvatarSize = 96f
        shopCoverHeight = 185f
        shopAvatarSize = 96f
        rainbowBrand = true
        motion = true
    }

    fun apply(settings: JSONObject?) {
        if (settings == null) return
        enabled = settings.optInt("theme_enabled", 1) != 0
        if (!enabled) {
            resetVisualDefaults()
            enabled = false
            return
        }
        accent = color(settings.optString("theme_accent", ""), DefaultJellyPink)
        accent2 = color(settings.optString("theme_accent2", ""), DefaultJellyPurple)
        text = color(settings.optString("theme_text_color", ""), DefaultJellyInk)
        muted = color(settings.optString("theme_muted_color", ""), DefaultJellyMuted)
        headerColor = color(settings.optString("theme_header_color", ""), Color.White)
        navColor = color(settings.optString("theme_nav_color", ""), Color.White)
        cardColor = color(settings.optString("theme_card_color", ""), DefaultJellySurface)
        borderColor = color(settings.optString("theme_border_color", ""), DefaultJellyOutline)
        buttonColor = color(settings.optString("theme_button_color", ""), Color.White)
        inputColor = color(settings.optString("theme_input_color", ""), Color.White)
        iconColor = color(settings.optString("theme_icon_color", ""), DefaultJellyInk)
        activeColor = color(settings.optString("theme_active_color", ""), accent)
        cardOpacity = settings.optInt("theme_card_opacity", 78).coerceIn(35, 100) / 100f
        headerOpacity = settings.optInt("theme_header_opacity", 78).coerceIn(35, 100) / 100f
        navOpacity = settings.optInt("theme_nav_opacity", 78).coerceIn(35, 100) / 100f
        inputOpacity = settings.optInt("theme_input_opacity", 90).coerceIn(35, 100) / 100f
        cardRadius = settings.optInt("theme_card_radius", 30).coerceIn(0, 60).toFloat()
        headerRadius = settings.optInt("theme_header_radius", 28).coerceIn(0, 60).toFloat()
        navRadius = settings.optInt("theme_nav_radius", 25).coerceIn(0, 60).toFloat()
        buttonRadius = settings.optInt("theme_button_radius", 999).coerceIn(0, 999).toFloat()
        inputRadius = settings.optInt("theme_input_radius", 17).coerceIn(0, 60).toFloat()
        shadow = settings.optInt("theme_shadow", 4).coerceIn(0, 30).toFloat()
        pageWidth = settings.optInt("theme_page_width", 980).coerceIn(320, 1400).toFloat()
        cardPadding = settings.optInt("theme_card_padding", 13).coerceIn(0, 40).toFloat()
        sectionGap = settings.optInt("theme_section_gap", 9).coerceIn(0, 40).toFloat()
        navHeight = settings.optInt("theme_nav_height", 76).coerceIn(44, 100).toFloat()
        buttonHeight = settings.optInt("theme_button_height", 44).coerceIn(32, 72).toFloat()
        fontScale = settings.optInt("theme_font_scale", 100).coerceIn(85, 120) / 100f
        profileCoverHeight = settings.optInt("theme_profile_cover_height", 165).coerceIn(90, 300).toFloat()
        profileAvatarSize = settings.optInt("theme_profile_avatar_size", 96).coerceIn(56, 160).toFloat()
        shopCoverHeight = settings.optInt("theme_shop_cover_height", 185).coerceIn(100, 320).toFloat()
        shopAvatarSize = settings.optInt("theme_shop_avatar_size", 96).coerceIn(60, 170).toFloat()
        rainbowBrand = settings.optInt("theme_brand_rainbow", 1) != 0
        motion = settings.optInt("theme_motion", 1) != 0
    }
}

private val Colors = lightColorScheme(
    primary = DefaultJellyPurple,
    secondary = DefaultJellyPink,
    tertiary = JellyGreen,
    background = JellyBg,
    surface = DefaultJellySurface,
    surfaceVariant = Color(0xFFF3F7FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DefaultJellyInk,
    onSurface = DefaultJellyInk,
    outline = DefaultJellyOutline
)

@Composable
fun MyChhachhTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
