package com.mychhachh.app.ui

import android.content.Context
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



@Composable
internal fun V95Votes(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.votes.isEmpty()) c.loadVotes() }
    V95PageList(c.t("Voting", "ووٹنگ"), c.t("Live community voting", "لائیو کمیونٹی ووٹنگ"), c.votes) { vote -> V95VoteCard(c, vote) }
}

@Composable
internal fun V95VoteCard(c: V95Controller, vote: Vote) {
    val mobile = LocalConfiguration.current.screenWidthDp <= 700
    V95GlassCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vote.status.uppercase(Locale.getDefault()), color = V95Muted, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Text(vote.title.ifBlank { "${vote.user1?.name.orEmpty()} VS ${vote.user2?.name.orEmpty()}" }, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Text(vote.endsAt.ifBlank { c.t("Live", "لائیو") }, color = V95Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = if (mobile) 5.dp else 8.dp, vertical = if (mobile) 6.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
            V95VotePlayer(c, vote.user1, vote.leftText, vote.votes1, vote.leftUserId, vote, Modifier.weight(1f), mobile)
            Box(Modifier.width(if (mobile) 92.dp else 104.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(if (mobile) 47.dp else 62.dp).clip(CircleShape).background(v95PrimaryBrush()).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = if (mobile) 15.sp else 20.sp)
                }
            }
            V95VotePlayer(c, vote.user2, vote.rightText, vote.votes2, vote.rightUserId, vote, Modifier.weight(1f), mobile)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V95Button(c.t("Comments", "کمنٹس"), Modifier.weight(1f), icon = V95Icons.Comment) { }
            V95Button(c.t("Share", "شیئر"), Modifier.weight(1f), primary = true, icon = V95Icons.Share) { if (c.user == null) c.route = V95Route.AUTH else c.shareVote(vote) }
        }
    }
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
    V95PageList(c.t("Announcements", "اعلانات"), c.t("Voice and community notices", "وائس اور کمیونٹی اعلانات"), c.announcements) { a ->
        V95GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                a.author?.let { V95Avatar(it, 46.dp) }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.author?.name ?: c.t("My Chhachh", "مائی چھچھ"), fontWeight = FontWeight.Black, color = V95Ink)
                    Text(a.createdAt, color = V95Muted, fontSize = 11.sp)
                }
            }
            if (a.text.isNotBlank()) Text(a.text, color = V95Ink, fontSize = 15.sp, lineHeight = 22.sp)
            if (!a.photo.isNullOrBlank()) AsyncImage(a.photo, null, Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
            if (!a.audio.isNullOrBlank()) V95Button(c.t("Play voice announcement", "وائس اعلان چلائیں"), primary = true, icon = V95Icons.Message) { }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                V95PostAction("${a.likes} ${c.t("Like", "لائک")}", V95Icons.Heart, a.liked) { if (c.user == null) c.route = V95Route.AUTH else c.likeAnnouncement(a) }
                V95PostAction("${a.comments} ${c.t("Comment", "کمنٹ")}", V95Icons.Comment) { }
                V95PostAction(c.t("Report", "رپورٹ"), V95Icons.More) { }
            }
        }
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
    val person = c.selectedUser ?: c.user
    if (person == null) { V95RequireLogin(c); return }
    val mine = person.id == c.user?.id
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            V95GlassCard(padding = 0.dp) {
                Box(Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp)).background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))) {
                    if (!person.cover.isNullOrBlank()) AsyncImage(person.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    V95Avatar(person, 90.dp, Modifier.offset(y = (-44).dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).padding(top = 9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(person.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            if (person.verified) { Spacer(Modifier.width(4.dp)); Image(painterResource(V95Icons.Check), null, Modifier.size(20.dp)) }
                        }
                        Text("@${person.username}", color = V95Muted, fontSize = 12.sp)
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (person.bio.isNotBlank()) Text(person.bio, color = V95Ink, fontSize = 14.sp, lineHeight = 21.sp)
                    val details = listOf(
                        V95Icons.Pin to listOf(person.area, person.village, person.city).filter { it.isNotBlank() }.joinToString(", "),
                        V95Icons.Feeling to person.hometown,
                        V95Icons.User to person.gender,
                        V95Icons.People to person.relationshipStatus,
                        V95Icons.Shop to person.work,
                        V95Icons.Crown to person.school
                    ).filter { it.second.isNotBlank() }
                    details.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { pair.forEach { d -> V95InfoChip(d.first, d.second, Modifier.weight(1f)) }; if (pair.size == 1) Spacer(Modifier.weight(1f)) } }
                    if (mine) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V95Button(c.t("Edit profile", "پروفائل ایڈٹ"), Modifier.weight(1f), primary = true, icon = V95Icons.User) { c.route = V95Route.SETTINGS }
                        V95Button(c.t("Settings", "ترتیبات"), Modifier.weight(1f), icon = V95Icons.Gear) { c.route = V95Route.SETTINGS }
                    } else V95Button(if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), primary = !person.followed, icon = V95Icons.Follow) { if (c.user == null) c.route = V95Route.AUTH else c.follow(person) }
                }
            }
        }
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
    val shop = c.selectedShop ?: return
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            V95GlassCard(padding = 0.dp) {
                Box(Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp)).background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))) {
                    if (!shop.cover.isNullOrBlank()) AsyncImage(shop.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    V95ShopAvatar(shop, 90.dp, Modifier.offset(y = (-50).dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).padding(top = 9.dp)) {
                        Text(shop.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(shop.category, color = V95Muted, fontSize = 12.sp)
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (shop.description.isNotBlank()) Text(shop.description, color = V95Ink, fontSize = 14.sp, lineHeight = 21.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        V95InfoChip(V95Icons.Pin, listOf(shop.area, shop.village, shop.city).filter { it.isNotBlank() }.joinToString(", "), Modifier.weight(1f))
                        V95InfoChip(V95Icons.People, "${shop.followers} ${c.t("followers", "فالوورز")}", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V95Button(c.t("Follow", "فالو"), Modifier.weight(1f), primary = !shop.followed, icon = V95Icons.Follow) { if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop) }
                        V95Button(c.t("Map", "نقشہ"), Modifier.weight(1f), icon = V95Icons.Map) { c.route = V95Route.MAP }
                    }
                }
            }
        }
    }
}

