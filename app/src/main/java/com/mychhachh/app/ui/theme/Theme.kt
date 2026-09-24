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

private val DefaultJellyInk = Color(0xFF10203D)
private val DefaultJellyMuted = Color(0xFF66738B)
private val DefaultJellyAccent = Color(0xFFFF4FB8)
private val DefaultJellyAccent2 = Color(0xFF6FC8FF)
private val DefaultJellySurface = Color(0xFFFFFFFF)
private val DefaultJellyOutline = Color(0xFFFFFFFF)
private val DefaultJellyIcon = Color(0xFF8B79FF)
private val DefaultJellyInput = Color(0xFFF7FBFF)

val JellyInk: Color get() = LiveJellyTheme.text
val JellyMuted: Color get() = LiveJellyTheme.muted
val JellyPink: Color get() = LiveJellyTheme.accent
val JellyPurple: Color get() = LiveJellyTheme.accent2
val JellyPink2 = Color(0xFFFF78C4)
val JellyCyan = Color(0xFF55D8FF)
val JellyBlue = Color(0xFF678CFF)
val JellyGold = Color(0xFFFFC75A)
val JellyGreen = Color(0xFF55DEB1)
val JellyBg = Color(0xFFDFF5FF)
val JellyBg2 = Color(0xFFF0EAFF)
val JellySurface: Color get() = LiveJellyTheme.cardColor
val JellyOutline: Color get() = LiveJellyTheme.borderColor
val JellyDanger = Color(0xFFB83057)

object LiveJellyTheme {
    var enabled by mutableStateOf(true)
    var accent by mutableStateOf(DefaultJellyAccent)
    var accent2 by mutableStateOf(DefaultJellyAccent2)
    var brandColor by mutableStateOf(Color(0xFF7B65D7))
    var brandColor2 by mutableStateOf(DefaultJellyAccent)
    var brandGlow by mutableFloatStateOf(12f)
    var brandBrightness by mutableFloatStateOf(1f)
    var brandSaturation by mutableFloatStateOf(1.25f)
    var customIconPalette by mutableStateOf(false)
    var iconHighlight by mutableStateOf(Color(0xFFF7FDFF))
    var iconShadow by mutableStateOf(Color(0xFF5F57CC))
    var text by mutableStateOf(DefaultJellyInk)
    var muted by mutableStateOf(DefaultJellyMuted)
    var headerColor by mutableStateOf(Color(0xFFDFF4FF))
    var navColor by mutableStateOf(Color.White)
    var cardColor by mutableStateOf(DefaultJellySurface)
    var borderColor by mutableStateOf(DefaultJellyOutline)
    var buttonColor by mutableStateOf(Color(0xFF1683FF))
    var inputColor by mutableStateOf(DefaultJellyInput)
    var iconColor by mutableStateOf(DefaultJellyIcon)
    var activeColor by mutableStateOf(DefaultJellyAccent)
    var cardOpacity by mutableFloatStateOf(.76f)
    var headerOpacity by mutableFloatStateOf(.78f)
    var navOpacity by mutableFloatStateOf(.74f)
    var inputOpacity by mutableFloatStateOf(.78f)
    var cardRadius by mutableFloatStateOf(28f)
    var headerRadius by mutableFloatStateOf(28f)
    var navRadius by mutableFloatStateOf(23f)
    var buttonRadius by mutableFloatStateOf(18f)
    var inputRadius by mutableFloatStateOf(20f)
    var shadow by mutableFloatStateOf(22f)
    var pageWidth by mutableFloatStateOf(920f)
    var cardPadding by mutableFloatStateOf(16f)
    var sectionGap by mutableFloatStateOf(12f)
    var navHeight by mutableFloatStateOf(72f)
    var buttonHeight by mutableFloatStateOf(44f)
    var fontScale by mutableFloatStateOf(1f)
    var profileCoverHeight by mutableFloatStateOf(165f)
    var profileAvatarSize by mutableFloatStateOf(96f)
    var shopCoverHeight by mutableFloatStateOf(185f)
    var shopAvatarSize by mutableFloatStateOf(96f)
    var profileOverlap by mutableFloatStateOf(44f)
    var shopOverlap by mutableFloatStateOf(50f)
    var notificationAvatarSize by mutableFloatStateOf(46f)
    var framePadding by mutableFloatStateOf(10f)
    var bgStrength by mutableFloatStateOf(1f)
    var rainbowBrand by mutableStateOf(true)
    var motion by mutableStateOf(true)
    var jellyDepth by mutableFloatStateOf(92f)
    var jellyShine by mutableFloatStateOf(96f)
    var jellyBorder by mutableFloatStateOf(92f)
    var jellySaturation by mutableFloatStateOf(140f)

    private fun color(raw: String, fallback: Color): Color {
        val v = raw.trim()
        if (!Regex("^#[0-9a-fA-F]{6}$").matches(v)) return fallback
        return runCatching {
            val rgb = v.removePrefix("#").toLong(16)
            Color(0xFF000000L or rgb)
        }.getOrDefault(fallback)
    }

    private fun resetVisualDefaults() {
        accent = DefaultJellyAccent
        accent2 = DefaultJellyAccent2
        brandColor = Color(0xFF7B65D7)
        brandColor2 = DefaultJellyAccent
        brandGlow = 12f
        brandBrightness = 1f
        brandSaturation = 1.25f
        customIconPalette = false
        iconHighlight = Color(0xFFF7FDFF)
        iconShadow = Color(0xFF5F57CC)
        text = DefaultJellyInk
        muted = DefaultJellyMuted
        headerColor = Color(0xFFDFF4FF)
        navColor = Color.White
        cardColor = DefaultJellySurface
        borderColor = DefaultJellyOutline
        buttonColor = Color(0xFF1683FF)
        inputColor = DefaultJellyInput
        iconColor = DefaultJellyIcon
        activeColor = DefaultJellyAccent
        cardOpacity = .76f
        headerOpacity = .78f
        navOpacity = .74f
        inputOpacity = .78f
        cardRadius = 28f
        headerRadius = 28f
        navRadius = 23f
        buttonRadius = 18f
        inputRadius = 20f
        shadow = 22f
        pageWidth = 920f
        cardPadding = 16f
        sectionGap = 12f
        navHeight = 72f
        buttonHeight = 44f
        fontScale = 1f
        profileCoverHeight = 165f
        profileAvatarSize = 96f
        shopCoverHeight = 185f
        shopAvatarSize = 96f
        profileOverlap = 44f
        shopOverlap = 50f
        notificationAvatarSize = 46f
        framePadding = 10f
        bgStrength = 1f
        rainbowBrand = true
        motion = true
        jellyDepth = 92f
        jellyShine = 96f
        jellyBorder = 92f
        jellySaturation = 140f
    }

    fun apply(settings: JSONObject?) {
        if (settings == null) return
        enabled = settings.optInt("theme_enabled", 1) != 0
        if (!enabled) {
            resetVisualDefaults()
            enabled = false
            return
        }
        accent = color(settings.optString("theme_accent", ""), DefaultJellyAccent)
        accent2 = color(settings.optString("theme_accent2", ""), DefaultJellyAccent2)
        brandColor = color(settings.optString("theme_brand_color", ""), Color(0xFF7B65D7))
        brandColor2 = color(settings.optString("theme_brand_color2", ""), DefaultJellyAccent)
        brandGlow = settings.optInt("theme_brand_glow", 12).coerceIn(0, 36).toFloat()
        brandBrightness = settings.optInt("theme_brand_brightness", 100).coerceIn(40, 180) / 100f
        brandSaturation = settings.optInt("theme_brand_saturation", 125).coerceIn(40, 220) / 100f
        customIconPalette = settings.optString("theme_icon_mode", "multicolor") == "custom"
        iconHighlight = color(settings.optString("theme_icon_highlight", ""), Color(0xFFF7FDFF))
        iconShadow = color(settings.optString("theme_icon_shadow", ""), Color(0xFF5F57CC))
        text = color(settings.optString("theme_text_color", ""), DefaultJellyInk)
        muted = color(settings.optString("theme_muted_color", ""), DefaultJellyMuted)
        headerColor = color(settings.optString("theme_header_color", ""), Color(0xFFDFF4FF))
        navColor = color(settings.optString("theme_nav_color", ""), Color.White)
        cardColor = color(settings.optString("theme_card_color", ""), DefaultJellySurface)
        borderColor = color(settings.optString("theme_border_color", ""), DefaultJellyOutline)
        buttonColor = color(settings.optString("theme_button_color", ""), Color(0xFF1683FF))
        inputColor = color(settings.optString("theme_input_color", ""), DefaultJellyInput)
        iconColor = color(settings.optString("theme_icon_color", ""), DefaultJellyIcon)
        activeColor = color(settings.optString("theme_active_color", ""), accent)
        cardOpacity = settings.optInt("theme_card_opacity", 76).coerceIn(35, 100) / 100f
        headerOpacity = settings.optInt("theme_header_opacity", 78).coerceIn(35, 100) / 100f
        navOpacity = settings.optInt("theme_nav_opacity", 74).coerceIn(35, 100) / 100f
        inputOpacity = settings.optInt("theme_input_opacity", 78).coerceIn(35, 100) / 100f
        cardRadius = settings.optInt("theme_card_radius", 28).coerceIn(0, 60).toFloat()
        headerRadius = settings.optInt("theme_header_radius", 28).coerceIn(0, 60).toFloat()
        navRadius = settings.optInt("theme_nav_radius", 23).coerceIn(0, 60).toFloat()
        buttonRadius = settings.optInt("theme_button_radius", 18).coerceIn(0, 999).toFloat()
        inputRadius = settings.optInt("theme_input_radius", 20).coerceIn(0, 60).toFloat()
        shadow = settings.optInt("theme_shadow", 22).coerceIn(0, 30).toFloat()
        pageWidth = settings.optInt("theme_page_width", 920).coerceIn(320, 1400).toFloat()
        cardPadding = settings.optInt("theme_card_padding", 16).coerceIn(0, 40).toFloat()
        sectionGap = settings.optInt("theme_section_gap", 12).coerceIn(0, 40).toFloat()
        navHeight = settings.optInt("theme_nav_height", 72).coerceIn(44, 100).toFloat()
        buttonHeight = settings.optInt("theme_button_height", 44).coerceIn(32, 72).toFloat()
        fontScale = settings.optInt("theme_font_scale", 100).coerceIn(85, 120) / 100f
        profileCoverHeight = settings.optInt("theme_profile_cover_height", 165).coerceIn(90, 300).toFloat()
        profileAvatarSize = settings.optInt("theme_profile_avatar_size", 96).coerceIn(56, 160).toFloat()
        shopCoverHeight = settings.optInt("theme_shop_cover_height", 185).coerceIn(100, 320).toFloat()
        shopAvatarSize = settings.optInt("theme_shop_avatar_size", 96).coerceIn(60, 170).toFloat()
        profileOverlap = settings.optInt("theme_profile_overlap", 44).coerceIn(0, 80).toFloat()
        shopOverlap = settings.optInt("theme_shop_overlap", 50).coerceIn(0, 90).toFloat()
        notificationAvatarSize = settings.optInt("theme_notification_avatar_size", 46).coerceIn(32, 72).toFloat()
        framePadding = settings.optInt("theme_frame_padding", 10).coerceIn(0, 28).toFloat()
        bgStrength = settings.optInt("theme_bg_strength", 100).coerceIn(0, 100) / 100f
        rainbowBrand = settings.optInt("theme_brand_rainbow", 1) != 0
        motion = settings.optInt("theme_motion", 1) != 0
        jellyDepth = settings.optInt("theme_jelly_depth", 92).coerceIn(0, 100).toFloat()
        jellyShine = settings.optInt("theme_jelly_shine", 96).coerceIn(0, 100).toFloat()
        jellyBorder = settings.optInt("theme_jelly_border", 92).coerceIn(0, 100).toFloat()
        jellySaturation = settings.optInt("theme_jelly_saturation", 140).coerceIn(80, 180).toFloat()
    }
}

private val Colors = lightColorScheme(
    primary = DefaultJellyAccent,
    secondary = DefaultJellyAccent2,
    tertiary = JellyGreen,
    background = JellyBg,
    surface = DefaultJellySurface,
    surfaceVariant = DefaultJellyInput,
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
