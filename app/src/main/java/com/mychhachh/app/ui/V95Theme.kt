package com.mychhachh.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychhachh.app.R

val V95Ink = Color(0xFF2E315A)
val V95Muted = Color(0xFF66738B)
val V95Purple = Color(0xFF443578)
val V95Pink = Color(0xFFFF4FAF)
val V95Blue = Color(0xFF678CFF)
val V95Cyan = Color(0xFF55D8FF)
val V95Danger = Color(0xFFB4253B)
val V95Green = Color(0xFF48B982)

private val V95Scheme = lightColorScheme(
    primary = V95Blue,
    secondary = V95Pink,
    background = Color(0xFFF3FBFF),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = V95Ink,
    onSurface = V95Ink
)

@Composable
fun V95Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = V95Scheme, content = content)
}

fun v95GlassBrush() = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.White.copy(alpha = .88f),
        .51f to Color(0xFFDEF7FF).copy(alpha = .72f),
        1f to Color(0xFFF9E7F9).copy(alpha = .61f)
    )
)

fun v95InputBrush() = Brush.linearGradient(
    listOf(Color.White.copy(alpha = .92f), Color(0xFFE2F7FF).copy(alpha = .79f))
)

fun v95PrimaryBrush() = Brush.linearGradient(
    listOf(Color(0xFF5ED9FF), Color(0xFF7E8DFF), Color(0xFFBD70F4), Color(0xFFFF68B8))
)

@Composable
fun V95GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color(0x26355F91), spotColor = Color(0x22355F91))
            .clip(shape)
            .background(v95GlassBrush())
            .border(2.dp, Color.White.copy(alpha = .94f), shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun V95InputShell(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(v95InputBrush())
            .border(1.5.dp, Color.White.copy(alpha = .98f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
        content = content
    )
}

@Composable
fun V95Button(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    icon: Int? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .then(
                if (primary) Modifier.background(v95PrimaryBrush())
                else Modifier.background(Color.White.copy(alpha = .66f))
            )
            .border(1.5.dp, Color.White.copy(alpha = .94f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Image(painterResource(icon), null, Modifier.size(24.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = when { primary -> Color.White; danger -> V95Danger; else -> V95Purple },
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun V95IconButton(
    icon: Int,
    size: Dp = 43.dp,
    iconSize: Dp = 34.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(iconSize), contentScale = ContentScale.Fit)
    }
}

@Composable
fun V95Badge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(999.dp)).background(V95Pink).padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun RainbowBrand(name: String = "My Chhachh", fontSize: Float = 31f) {
    val colors = listOf(
        Color(0xFFFF47B3), Color(0xFFFFB642), Color(0xFF62DC9D),
        Color(0xFF56C9FF), Color(0xFF8B75F5), Color(0xFFF456C3)
    )
    Row(horizontalArrangement = Arrangement.Center) {
        name.forEachIndexed { index, c ->
            Text(
                c.toString(),
                color = colors[(index * colors.size / name.length.coerceAtLeast(1)).coerceIn(0, colors.lastIndex)],
                fontWeight = FontWeight.Black,
                fontSize = fontSize.sp,
                letterSpacing = (-1.3).sp
            )
        }
    }
}

object V95Icons {
    val Menu = R.drawable.jelly_menu
    val Search = R.drawable.jelly_search
    val Filter = R.drawable.jelly_filter
    val Bell = R.drawable.jelly_bell
    val Home = R.drawable.jelly_home
    val People = R.drawable.jelly_people
    val Shop = R.drawable.jelly_shop
    val Map = R.drawable.jelly_map
    val Message = R.drawable.jelly_message
    val Vote = R.drawable.jelly_vote
    val Weather = R.drawable.jelly_weather
    val Announcement = R.drawable.jelly_announcement
    val Photo = R.drawable.jelly_photo
    val Video = R.drawable.jelly_video
    val Pin = R.drawable.jelly_pin
    val Feeling = R.drawable.jelly_feeling
    val Mention = R.drawable.jelly_mention
    val Heart = R.drawable.jelly_heart
    val Eye = R.drawable.jelly_eye
    val Comment = R.drawable.jelly_comment
    val Share = R.drawable.jelly_share
    val Save = R.drawable.jelly_save
    val More = R.drawable.jelly_more
    val User = R.drawable.jelly_user
    val Follow = R.drawable.jelly_follow
    val Close = R.drawable.jelly_close
    val Gear = R.drawable.jelly_gear
    val Shield = R.drawable.jelly_shield
    val Palette = R.drawable.jelly_palette
    val Logout = R.drawable.jelly_logout
    val Send = R.drawable.jelly_send
    val Phone = R.drawable.jelly_phone
    val Whatsapp = R.drawable.jelly_whatsapp
    val Clock = R.drawable.jelly_clock
    val Crown = R.drawable.jelly_crown
    val Check = R.drawable.jelly_check
    val Plus = R.drawable.jelly_plus
}
