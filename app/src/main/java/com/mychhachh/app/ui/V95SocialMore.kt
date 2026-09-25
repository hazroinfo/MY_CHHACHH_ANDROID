package com.mychhachh.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import java.time.Instant



@Composable
internal fun V95Votes(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.votes.isEmpty()) c.loadVotes() }
    V95PageList(c.t("Voting", "ووٹنگ"), c.t("Live community voting", "لائیو کمیونٹی ووٹنگ"), c.votes) { vote -> V95VoteCard(c, vote) }
}

@Composable
internal fun V95VoteCard(c: V95Controller, vote: Vote, detail: Boolean = false) {
    val mobile = LocalConfiguration.current.screenWidthDp <= 700
    var nowMs by remember(vote.id) { mutableLongStateOf(java.lang.System.currentTimeMillis()) }

    LaunchedEffect(vote.id, vote.endsAt, vote.status) {
        while (vote.status == "active" || vote.status == "started" || vote.status == "live") {
            nowMs = java.lang.System.currentTimeMillis()
            delay(1000)
        }
    }

    val remaining = v95VoteRemaining(vote.endsAt, nowMs, c)

    V95GlassCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vote.status.uppercase(Locale.getDefault()), color = V95Muted, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text(
                vote.title.ifBlank { "${vote.user1?.name.orEmpty()} VS ${vote.user2?.name.orEmpty()}" },
                color = V95Ink,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (mobile) 3.dp else 8.dp, vertical = if (mobile) 5.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V95VotePlayer(c, vote.user1, vote.leftText, vote.votes1, vote.leftUserId, vote, Modifier.weight(1f), mobile)

            Column(
                Modifier.width(if (mobile) 92.dp else 104.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(v95InputBrush())
                        .border(1.5.dp, Color.White, RoundedCornerShape(999.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(remaining, color = V95Purple, fontWeight = FontWeight.Black, fontSize = if (mobile) 9.sp else 10.sp)
                }
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .size(if (mobile) 47.dp else 62.dp)
                        .clip(CircleShape)
                        .background(v95PrimaryBrush())
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = if (mobile) 15.sp else 20.sp)
                }
            }

            V95VotePlayer(c, vote.user2, vote.rightText, vote.votes2, vote.rightUserId, vote, Modifier.weight(1f), mobile)
        }

        if (vote.status == "ended" || vote.status == "forfeit") {
            val winnerName = when (vote.winnerUserId) {
                vote.leftUserId -> vote.user1?.name ?: vote.leftText
                vote.rightUserId -> vote.user2?.name ?: vote.rightText
                else -> ""
            }
            Text(
                if (vote.tie) c.t("Result: Tie", "نتیجہ: برابر")
                else if (winnerName.isNotBlank()) c.t("Winner: $winnerName", "فاتح: $winnerName")
                else c.t("Voting ended", "ووٹنگ ختم ہوگئی"),
                modifier = Modifier.fillMaxWidth(),
                color = V95Purple,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!detail) {
                V95Button(c.t("Comments", "کمنٹس"), Modifier.weight(1f), icon = V95Icons.Comment) { c.openVote(vote) }
            }
            V95Button(
                c.t("Share", "شیئر"),
                Modifier.weight(1f),
                primary = true,
                icon = V95Icons.Share
            ) {
                if (c.user == null) c.route = V95Route.AUTH else c.shareVote(vote)
            }
        }
    }
}

private fun v95VoteRemaining(endsAt: String, nowMs: Long, c: V95Controller): String {
    if (endsAt.isBlank()) return c.t("Live", "لائیو")
    val end = runCatching { Instant.parse(endsAt).toEpochMilli() }.getOrNull()
        ?: return endsAt.replace('T', ' ').take(16)
    val seconds = ((end - nowMs) / 1000L).coerceAtLeast(0L)
    if (seconds <= 0L) return c.t("Ended", "ختم")
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (days > 0) "${days}d ${hours}h ${mins}m"
    else String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
}

@Composable
internal fun RowScope.V95VotePlayer(c: V95Controller, person: User?, statement: String, count: Int, id: Long, vote: Vote, modifier: Modifier, mobile: Boolean) {
    Column(modifier.padding(horizontal = 3.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (person != null) V95Avatar(person, if (mobile) 62.dp else 76.dp) else Box(Modifier.size(if (mobile) 62.dp else 76.dp).clip(CircleShape).background(v95InputBrush()))
        Spacer(Modifier.height(5.dp))
        Text(person?.name ?: statement.ifBlank { c.t("Option", "آپشن") }, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
        Text(count.toString(), color = V95Purple, fontWeight = FontWeight.Black, fontSize = 16.sp)
        V95Button(c.t("Vote", "ووٹ"), primary = vote.myChoice != id) { if (c.user == null) c.route = V95Route.AUTH else if (id > 0) c.castVote(vote, id) }
    }
}

@Composable
internal fun V95Announcements(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.announcements.isEmpty()) c.loadAnnouncements() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { V95PageHeading(c.t("Announcements", "اعلانات"), c.t("Voice and community notices", "وائس اور کمیونٹی اعلانات")) }
        if (c.user != null) item { V95AnnouncementComposer(c) }
        if (c.announcements.isEmpty() && !c.busy) item { V95Empty(c.t("No announcements yet", "ابھی کوئی اعلان نہیں")) }
        items(c.announcements, key = { it.id }) { item -> V95AnnouncementCard(c, item) }
    }
}

@Composable
internal fun V95Notifications(c: V95Controller) {
    if (c.user == null) { V95RequireLogin(c); return }
    LaunchedEffect(Unit) { c.loadNotifications() }
    V95PageList(c.t("Notifications", "اطلاعات"), c.t("Recent activity", "حالیہ سرگرمی"), c.notices) { n ->
        V95GlassCard(radius = 22.dp, padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                n.actor?.let { V95Avatar(it, 44.dp) }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(n.text, color = V95Ink, fontWeight = if (n.read) FontWeight.SemiBold else FontWeight.Black, fontSize = 13.sp)
                    Text(n.createdAt, color = V95Muted, fontSize = 10.sp)
                }
                if (!n.read) Box(Modifier.size(9.dp).clip(CircleShape).background(V95Pink))
            }
        }
    }
}

@Composable
internal fun V95Profile(c: V95Controller) {
    val original = c.selectedUser ?: c.user
    if (original == null) { V95RequireLogin(c); return }

    val d = c.profileDetails
    val person = d?.optJSONObject("user")?.toUser() ?: original
    val mine = person.id == c.user?.id
    val followersHidden = d?.optBoolean("followers_hidden", person.hideFollowers) ?: person.hideFollowers
    val followers = d?.optInt("followers", 0) ?: 0
    val following = d?.optInt("following_count", 0) ?: 0
    val posts = d?.optJSONArray("posts")?.posts() ?: emptyList()
    val profileShop = d?.optJSONObject("shop")?.toShop()

    val width = LocalConfiguration.current.screenWidthDp
    val coverHeight = c.features.optDouble("theme_profile_cover_height", 165.0).toFloat().dp
    val configuredAvatar = c.features.optDouble("theme_profile_avatar_size", 96.0).toFloat()
    val avatarSize = (if (width <= 430) configuredAvatar.coerceAtMost(90f) else configuredAvatar).dp
    val overlap = c.features.optDouble("theme_profile_overlap", 44.0).toFloat().dp

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            V95GlassCard(padding = 0.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(coverHeight)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))
                ) {
                    if (!person.cover.isNullOrBlank()) {
                        AsyncImage(person.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = if (width <= 430) 12.dp else 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    V95Avatar(person, avatarSize, Modifier.offset(y = -overlap))
                    Spacer(Modifier.width(if (width <= 430) 9.dp else 12.dp))
                    Column(Modifier.weight(1f).padding(top = 9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                person.name,
                                color = V95Ink,
                                fontWeight = FontWeight.Black,
                                fontSize = if (width <= 430) 21.sp else 24.sp
                            )
                            if (person.verified) {
                                Spacer(Modifier.width(3.dp))
                                Image(painterResource(V95Icons.Check), null, Modifier.size(17.dp))
                            }
                        }
                        Text("@${person.username}", color = V95Muted, fontSize = 11.sp)
                    }
                }

                Column(
                    Modifier.padding(horizontal = if (width <= 430) 12.dp else 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        V95ProfileCount(
                            if (followersHidden && !mine) "—" else followers.toString(),
                            c.t("Followers", "فالوورز")
                        ) {
                            if (!(followersHidden && !mine)) c.openRelations(person, "followers")
                        }
                        V95ProfileCount(
                            if (followersHidden && !mine) "—" else following.toString(),
                            c.t("Following", "فالوونگ")
                        ) {
                            if (!(followersHidden && !mine)) c.openRelations(person, "following")
                        }
                        V95ProfileCount(posts.size.toString(), c.t("Posts", "پوسٹس"))
                    }

                    if (person.bio.isNotBlank()) {
                        Text(person.bio, color = V95Ink, fontSize = 13.sp, lineHeight = 19.sp)
                    }

                    val detailItems = listOf(
                        V95Icons.Pin to listOf(person.area, person.village, person.city).filter { it.isNotBlank() }.joinToString(", "),
                        V95Icons.Feeling to person.hometown,
                        V95Icons.User to person.gender,
                        V95Icons.People to person.relationshipStatus,
                        V95Icons.Shop to person.work,
                        V95Icons.Crown to person.school,
                        V95Icons.Mail to if (person.showEmail || mine) person.email else "",
                        V95Icons.Phone to if (person.showPhone || mine) person.phone else ""
                    ).filter { it.second.isNotBlank() }

                    detailItems.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { item -> V95InfoChip(item.first, item.second, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }

                    if (mine) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            V95Button(c.t("Edit profile", "پروفائل ایڈٹ"), Modifier.weight(1f), primary = true, icon = V95Icons.User) {
                                c.route = V95Route.SETTINGS
                            }
                            V95Button(c.t("Settings", "ترتیبات"), Modifier.weight(1f), icon = V95Icons.Gear) {
                                c.route = V95Route.SETTINGS
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            V95Button(
                                if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                                Modifier.weight(1f),
                                primary = !person.followed,
                                icon = V95Icons.Follow
                            ) {
                                if (c.user == null) c.route = V95Route.AUTH else c.follow(person)
                            }
                            V95Button(c.t("Message", "پیغام"), Modifier.weight(1f), icon = V95Icons.Message) {
                                if (c.user == null) c.route = V95Route.AUTH else c.openChat(person)
                            }
                            V95Button(c.t("Block", "بلاک"), Modifier.weight(1f), danger = true) {
                                if (c.user == null) c.route = V95Route.AUTH else c.toggleBlock(person)
                            }
                        }
                    }
                }
            }
        }

        if (profileShop != null) {
            item {
                V95GlassCard(radius = 20.dp, padding = 10.dp) {
                    Row(
                        Modifier.fillMaxWidth().clickable { c.openShop(profileShop) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V95ShopAvatar(profileShop, 52.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profileShop.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(profileShop.category, color = V95Muted, fontSize = 10.sp)
                        }
                        Image(painterResource(V95Icons.Shop), null, Modifier.size(28.dp))
                    }
                }
            }
        }

        if (posts.isNotEmpty()) {
            item { Text(c.t("Posts", "پوسٹس"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 3.dp)) }
            items(posts, key = { it.id }) { V95PostCard(c, it) }
        }
    }
}

@Composable
private fun RowScope.V95ProfileCount(value: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 46.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(value, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, color = V95Muted, fontSize = 10.sp)
    }
}

@Composable
internal fun V95InfoChip(icon: Int, text: String, modifier: Modifier) {
    Row(modifier.heightIn(min = 66.dp).clip(RoundedCornerShape(18.dp)).background(v95InputBrush()).border(1.5.dp, Color.White, RoundedCornerShape(18.dp)).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(28.dp)); Spacer(Modifier.width(7.dp)); Text(text, color = V95Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun V95ShopDetail(c: V95Controller) {
    val fallback = c.selectedShop ?: return
    val d = c.shopDetails
    val shop = d?.optJSONObject("shop")?.toShop() ?: fallback
    val owner = d?.optJSONObject("owner")?.toUser()
    val posts = d?.optJSONArray("posts")?.posts() ?: emptyList()
    val context = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp
    val coverHeight = c.features.optDouble("theme_shop_cover_height", 185.0).toFloat().dp
    val configuredAvatar = c.features.optDouble("theme_shop_avatar_size", 96.0).toFloat()
    val avatarSize = (if (width <= 430) configuredAvatar.coerceAtMost(90f) else configuredAvatar).dp
    val overlap = c.features.optDouble("theme_shop_overlap", 50.0).toFloat().dp
    val mine = shop.userId == c.user?.id

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            V95GlassCard(padding = 0.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(coverHeight)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))
                ) {
                    if (!shop.cover.isNullOrBlank()) AsyncImage(shop.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = if (width <= 430) 12.dp else 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    V95ShopAvatar(shop, avatarSize, Modifier.offset(y = -overlap))
                    Spacer(Modifier.width(if (width <= 430) 9.dp else 12.dp))
                    Column(Modifier.weight(1f).padding(top = 9.dp)) {
                        Text(shop.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = if (width <= 430) 21.sp else 24.sp)
                        if (shop.username.isNotBlank()) Text("@${shop.username}", color = V95Muted, fontSize = 10.sp)
                        Text(shop.category, color = V95Muted, fontSize = 11.sp)
                        Text(listOf(shop.village, shop.city).filter { it.isNotBlank() }.joinToString(" · "), color = V95Muted, fontSize = 10.sp)
                    }
                }

                Column(Modifier.padding(horizontal = if (width <= 430) 12.dp else 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        V95ProfileCount(shop.products.toString(), c.t("Products", "پروڈکٹس"))
                        V95ProfileCount(shop.followers.toString(), c.t("Followers", "فالوورز")) { c.openShopFollowers(shop) }
                        V95ProfileCount(d?.optInt("views", 0)?.toString() ?: "0", c.t("Views", "ویوز"))
                    }

                    val detailItems = listOf(
                        V95Icons.Pin to listOf(shop.area, shop.village, shop.city).filter { it.isNotBlank() }.joinToString(", "),
                        V95Icons.Phone to shop.phone,
                        V95Icons.Whatsapp to shop.whatsapp,
                        V95Icons.Map to shop.location,
                        V95Icons.User to (owner?.name ?: "")
                    ).filter { it.second.isNotBlank() }

                    detailItems.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { item -> V95InfoChip(item.first, item.second, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }

                    if (shop.description.isNotBlank()) {
                        Text(shop.description, color = V95Ink, fontSize = 13.sp, lineHeight = 19.sp)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!mine) {
                            V95Button(
                                if (shop.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                                Modifier.weight(1f),
                                primary = !shop.followed,
                                icon = V95Icons.Follow
                            ) {
                                if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop)
                            }
                        }
                        V95Button(c.t("Map", "نقشہ"), Modifier.weight(1f), icon = V95Icons.Map) { c.route = V95Route.MAP }
                        if (shop.phone.isNotBlank()) {
                            V95Button(c.t("Call", "کال"), Modifier.weight(1f), icon = V95Icons.Phone) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shop.phone}"))) }
                            }
                        }
                    }

                    if (shop.whatsapp.isNotBlank()) {
                        V95Button(c.t("WhatsApp", "واٹس ایپ"), Modifier.fillMaxWidth(), icon = V95Icons.Whatsapp) {
                            val number = shop.whatsapp.filter { it.isDigit() || it == '+' }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${number.replace("+", "")}"))) }
                        }
                    }
                }
            }
        }

        if (posts.isNotEmpty()) {
            item { Text(c.t("Shop Posts", "دکان پوسٹس"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 3.dp)) }
            items(posts, key = { it.id }) { V95PostCard(c, it) }
        } else {
            item { V95Empty(c.t("No shop posts yet", "ابھی کوئی شاپ پوسٹ نہیں")) }
        }
    }
}
