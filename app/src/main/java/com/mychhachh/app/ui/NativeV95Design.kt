package com.mychhachh.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.R
import com.mychhachh.app.data.Shop
import com.mychhachh.app.data.User

internal val NVInk = Color(0xFF2E315A)
internal val NVMuted = Color(0xFF66738B)
internal val NVPurple = Color(0xFF443578)
internal val NVPink = Color(0xFFFF4FAF)
internal val NVBlue = Color(0xFF678CFF)
internal val NVCyan = Color(0xFF55D8FF)
internal val NVDanger = Color(0xFFB4253B)
internal val NVGreen = Color(0xFF48B982)

private val NVScheme = lightColorScheme(
    primary = NVBlue,
    secondary = NVPink,
    background = Color(0xFFF3FBFF),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = NVInk,
    onSurface = NVInk
)

@Composable
internal fun NativeV95Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NVScheme, content = content)
}

internal fun nvGlassBrush() = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.White.copy(alpha = .88f),
        .51f to Color(0xFFDEF7FF).copy(alpha = .72f),
        1f to Color(0xFFF9E7F9).copy(alpha = .61f)
    )
)

internal fun nvInputBrush() = Brush.linearGradient(
    listOf(Color.White.copy(alpha = .92f), Color(0xFFE2F7FF).copy(alpha = .79f))
)

internal fun nvPrimaryBrush() = Brush.linearGradient(
    listOf(Color(0xFF5ED9FF), Color(0xFF7E8DFF), Color(0xFFBD70F4), Color(0xFFFF68B8))
)

@Composable
internal fun NVCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color(0x1A355F91), spotColor = Color(0x18355F91))
            .clip(shape)
            .background(nvGlassBrush())
            .border(1.5.dp, Color.White.copy(alpha = .94f), shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}

@Composable
internal fun NVButton(
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
            .background(if (primary) nvPrimaryBrush() else Brush.linearGradient(listOf(Color.White.copy(.78f), Color.White.copy(.60f))))
            .border(1.5.dp, Color.White.copy(.96f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Image(painterResource(icon), null, Modifier.size(23.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            color = when { primary -> Color.White; danger -> NVDanger; else -> NVPurple },
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
internal fun NVIconButton(
    icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 43.dp,
    iconSize: Dp = 34.dp,
    onClick: () -> Unit
) {
    Box(modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(iconSize), contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun NVInput(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    onValue: (String) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(nvInputBrush())
            .border(1.5.dp, Color.White.copy(.98f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            textStyle = TextStyle(color = NVInk, fontSize = 13.sp),
            decorationBox = { inner ->
                if (value.isBlank()) Text(placeholder, color = Color(0xFF858EB1), fontSize = 12.sp)
                inner()
            }
        )
    }
}

@Composable
internal fun NVBrand(fontSize: Float = 24f, name: String = "My Chhachh") {
    val colors = listOf(
        Color(0xFFFF47B3), Color(0xFFFFB642), Color(0xFF62DC9D),
        Color(0xFF56C9FF), Color(0xFF8B75F5), Color(0xFFF456C3)
    )
    val displayName = name.ifBlank { "My Chhachh" }
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        displayName.forEachIndexed { index, ch ->
            Text(
                ch.toString(),
                color = colors[(index * colors.size / displayName.length.coerceAtLeast(1)).coerceIn(0, colors.lastIndex)],
                fontWeight = FontWeight.Black,
                fontSize = fontSize.sp,
                letterSpacing = (-1.1).sp
            )
        }
    }
}

@Composable
internal fun NVAvatar(user: User, size: Dp, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier.size(size).clip(shape).background(nvInputBrush()).border(2.5.dp, Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        if (!user.avatar.isNullOrBlank()) {
            AsyncImage(user.avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(user.name.take(1).uppercase(), color = NVPurple, fontWeight = FontWeight.Black, fontSize = (size.value * .34f).sp)
        }
    }
}

@Composable
internal fun NVShopAvatar(shop: Shop, size: Dp, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier.size(size).clip(shape).background(nvInputBrush()).border(2.5.dp, Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        if (!shop.photo.isNullOrBlank()) AsyncImage(shop.photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Image(painterResource(NVIcons.Shop), null, Modifier.size(size * .56f))
    }
}

@Composable
internal fun NVHeading(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(title, color = NVInk, fontWeight = FontWeight.Black, fontSize = 22.sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = NVMuted, fontSize = 11.sp)
    }
}

@Composable
internal fun NVEmpty(text: String) {
    NVCard { Text(text, Modifier.fillMaxWidth(), color = NVMuted, fontSize = 12.sp, textAlign = TextAlign.Center) }
}

internal object NVIcons {
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
    val Edit = R.drawable.jelly_edit
    val Delete = R.drawable.jelly_delete
    val Info = R.drawable.jelly_info
    val Upload = R.drawable.jelly_upload
    val Address = R.drawable.jelly_address
    val Category = R.drawable.jelly_category
    val City = R.drawable.jelly_city
    val Gender = R.drawable.jelly_gender
    val Hometown = R.drawable.jelly_hometown
    val Mail = R.drawable.jelly_mail
    val Mohalla = R.drawable.jelly_mohalla
    val School = R.drawable.jelly_school
    val Village = R.drawable.jelly_village
    val Website = R.drawable.jelly_website
    val Work = R.drawable.jelly_work
    val Facebook = R.drawable.jelly_facebook
    val Instagram = R.drawable.jelly_instagram
    val Youtube = R.drawable.jelly_youtube
}
