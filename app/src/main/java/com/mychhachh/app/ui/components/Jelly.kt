package com.mychhachh.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.R
import com.mychhachh.app.data.User
import com.mychhachh.app.ui.theme.*

object JellyIcons {
    val Home = R.drawable.jelly_home; val User = R.drawable.jelly_user; val People = R.drawable.jelly_people
    val Heart = R.drawable.jelly_heart; val Message = R.drawable.jelly_message; val Comment = R.drawable.jelly_comment
    val Share = R.drawable.jelly_share; val Eye = R.drawable.jelly_eye; val Edit = R.drawable.jelly_edit
    val Gear = R.drawable.jelly_gear; val Pin = R.drawable.jelly_pin; val Map = R.drawable.jelly_map
    val Address = R.drawable.jelly_address; val Phone = R.drawable.jelly_phone; val Mail = R.drawable.jelly_mail
    val Shield = R.drawable.jelly_shield; val Clock = R.drawable.jelly_clock; val Logout = R.drawable.jelly_logout
    val Photo = R.drawable.jelly_photo; val Video = R.drawable.jelly_video; val Shop = R.drawable.jelly_shop
    val Bell = R.drawable.jelly_bell; val Announcement = R.drawable.jelly_announcement; val Vote = R.drawable.jelly_vote
    val Search = R.drawable.jelly_search; val Plus = R.drawable.jelly_plus; val Star = R.drawable.jelly_star
    val Palette = R.drawable.jelly_palette; val Brush = R.drawable.jelly_brush; val Menu = R.drawable.jelly_menu
    val Filter = R.drawable.jelly_filter; val Whatsapp = R.drawable.jelly_whatsapp; val Feeling = R.drawable.jelly_feeling
    val Mention = R.drawable.jelly_mention; val Delete = R.drawable.jelly_delete; val Check = R.drawable.jelly_check
    val Reply = R.drawable.jelly_reply; val Crown = R.drawable.jelly_crown; val Lock = R.drawable.jelly_lock
    val Send = R.drawable.jelly_send; val Close = R.drawable.jelly_close; val Save = R.drawable.jelly_save
    val Weather = R.drawable.jelly_weather
    val Follow = R.drawable.jelly_follow; val More = R.drawable.jelly_more; val Arrow = R.drawable.jelly_arrow
    val City = R.drawable.jelly_city; val Village = R.drawable.jelly_village; val Mohalla = R.drawable.jelly_mohalla
    val Hometown = R.drawable.jelly_hometown; val Gender = R.drawable.jelly_gender; val Work = R.drawable.jelly_work
    val School = R.drawable.jelly_school; val Category = R.drawable.jelly_category; val Info = R.drawable.jelly_info
}

@Composable
fun JellyIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null
) {
    androidx.compose.foundation.Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun JellyGlass(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier
        .shadow(4.dp, shape, ambientColor = Color(0x124E5B90), spotColor = Color(0x164E5B90))
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    Color(0xF9FFFFFF),
                    Color(0xF2F5FBFF),
                    Color(0xF2F6F1FF)
                )
            )
        )
        .border(1.dp, Color.White.copy(.95f), shape)
    if (onClick != null) m = m.clickable { onClick() }
    Box(m.padding(padding), content = content)
}

@Composable
fun JellyPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .clip(shape)
            .clickable { onClick() }
            .background(
                if (selected) Brush.horizontalGradient(listOf(JellyPink, JellyPurple))
                else Brush.linearGradient(listOf(Color.White, Color(0xFFF1FAFF)))
            )
            .border(1.dp, if (selected) Color.White.copy(.85f) else JellyOutline, shape)
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else JellyInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun JellyButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val alpha = if (enabled) 1f else .45f
    Row(
        modifier
            .clip(shape)
            .clickable(enabled = enabled) { onClick() }
            .background(
                if (primary) Brush.horizontalGradient(listOf(JellyPink.copy(alpha = alpha), JellyPurple.copy(alpha = alpha)))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = alpha), Color(0xFFF0F9FF).copy(alpha = alpha)))
            )
            .border(1.dp, if (enabled) Color.White else JellyOutline, shape)
            .heightIn(min = 40.dp)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.let {
            JellyIcon(it, size = 22.dp)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            color = (if (primary) Color.White else JellyInk).copy(alpha = alpha),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun JellyIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        JellyIcon(icon, size = 27.dp, contentDescription = contentDescription)
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(999.dp))
                    .background(JellyPink)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (badge > 99) "99+" else badge.toString(), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun Brand(name: String = "My Chhachh", modifier: Modifier = Modifier, fontSize: Int = 27) {
    Text(
        name,
        modifier = modifier,
        style = TextStyle(
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFFFF47B3),
                    Color(0xFFFFB642),
                    Color(0xFF62DC9D),
                    Color(0xFF56C9FF),
                    Color(0xFF8B75F5)
                )
            ),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1f).sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun Avatar(user: User, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFEEFBFF), Color(0xFFDDCEFF))))
            .border(1.5f.dp, Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        if (!user.avatar.isNullOrBlank()) {
            AsyncImage(user.avatar, user.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                user.name.firstOrNull()?.uppercase() ?: "M",
                color = JellyInk,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * .4f).sp
            )
        }
    }
}

@Composable
fun UserName(user: User, fontSize: Int = 15) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            user.name.ifBlank { "User" },
            color = JellyInk,
            fontWeight = FontWeight.Black,
            fontSize = fontSize.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (user.verified) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF2F9EF5)), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LoadingBlock() = Box(
    Modifier.fillMaxWidth().padding(30.dp),
    contentAlignment = Alignment.Center
) { CircularProgressIndicator(color = JellyPurple) }

@Composable
fun ErrorCard(message: String, onRetry: (() -> Unit)? = null) {
    JellyGlass(Modifier.fillMaxWidth(), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(message, color = Color(0xFFB23A55), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            onRetry?.let { JellyButton("Retry", primary = true, onClick = it) }
        }
    }
}

@Composable
fun EmptyCard(text: String, @DrawableRes icon: Int = JellyIcons.Info) {
    JellyGlass(Modifier.fillMaxWidth(), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JellyIcon(icon, size = 31.dp)
            Spacer(Modifier.width(9.dp))
            Text(text, color = JellyMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun PageTitle(title: String, subtitle: String? = null, @DrawableRes icon: Int? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            JellyIcon(it, size = 34.dp)
            Spacer(Modifier.width(7.dp))
        }
        Column {
            Text(title, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 22.sp)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = JellyMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
}

fun shortTime(raw: String): String = raw.replace('T', ' ').take(16).ifBlank { "Just now" }
