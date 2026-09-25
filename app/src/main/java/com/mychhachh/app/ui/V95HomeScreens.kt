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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
internal fun V95Home(c: V95Controller) {
    LaunchedEffect(c.feedMode) { if (c.feed.isEmpty()) c.loadFeed(false) }
    val guest = c.user == null
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 5.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (guest) item { V95GuestCard(c) }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                V95Tab(c.t("For You", "آپ کے لیے"), c.feedMode == "for_you", Modifier.weight(1f)) { c.changeFeed("for_you") }
                if (!guest) {
                    V95Tab(c.t("Following", "فالوونگ"), c.feedMode == "following", Modifier.weight(1f)) { c.changeFeed("following") }
                }
                V95Tab(c.t("Shop Posts", "دکان پوسٹس"), c.feedMode == "shops", Modifier.weight(1f)) { c.changeFeed("shops") }
                if (guest) Spacer(Modifier.weight(1f))
            }
        }
        if (!guest && c.feedMode != "shops") item { V95Composer(c) }
        if (c.feed.isEmpty() && !c.busy) item { V95Empty(c.t("No posts yet", "ابھی کوئی پوسٹ نہیں")) }
        items(c.feed, key = { it.id }) { post -> V95PostCard(c, post) }
    }
}

@Composable
internal fun V95Tab(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(LocalV95Features.current.optDouble("theme_button_radius", 18.0).toFloat().dp)
    Box(
        modifier
            .height(43.dp)
            .clip(shape)
            .background(
                if (active) {
                    Brush.linearGradient(
                        listOf(Color(0xFFFF6EC0), Color(0xFFFF4CAD), Color(0xFFB671F1))
                    )
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(.72f), Color.White.copy(.62f)))
                }
            )
            .border(1.5.dp, Color.White.copy(.95f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) Color.White else V95Purple,
            fontWeight = FontWeight.Black,
            fontSize = 10.5.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun V95GuestCard(c: V95Controller) {
    V95GlassCard(radius = 28.dp, padding = 16.dp) {
        Text(
            c.t("Welcome to My Chhachh", "My Chhachh میں خوش آمدید"),
            color = V95Ink,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        Text(
            c.t(
                "Browse public posts, or sign in to like, comment, message and post.",
                "عوامی پوسٹس دیکھیں، لائک، کمنٹ، پیغام اور پوسٹ کے لیے لاگ اِن کریں۔"
            ),
            color = V95Muted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V95Button(c.t("Login", "لاگ اِن"), Modifier.weight(1f)) { c.route = V95Route.AUTH }
            V95Button(c.t("Create account", "اکاؤنٹ بنائیں"), Modifier.weight(1f), primary = true) { c.route = V95Route.AUTH }
        }
    }
}

@Composable
internal fun V95Composer(c: V95Controller) {
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var video by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("public") }
    var feelingIndex by remember { mutableIntStateOf(0) }
    val feelings = listOf("", "happy", "thankful", "excited", "sad", "proud")
    val feeling = feelings[feelingIndex]

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "post_photo") { photo = it }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "post_video") { video = it }
    }

    V95GlassCard {
        Text(c.t("Create post", "پوسٹ بنائیں"), fontWeight = FontWeight.Black, color = V95Ink, fontSize = 15.sp)
        V95TextArea(text, c.t("What's happening in Chhachh?", "چھچھ میں کیا ہو رہا ہے؟")) { text = it }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            V95Tool(c.t("Photo", "فوٹو"), V95Icons.Photo, Modifier.weight(1f)) { photoPicker.launch("image/*") }
            V95Tool(c.t("Video", "ویڈیو"), V95Icons.Video, Modifier.weight(1f)) { videoPicker.launch("video/*") }
            V95Tool(c.t("Check in", "چیک اِن"), V95Icons.Pin, Modifier.weight(1f)) { c.startPostCheckin() }
            V95Tool(c.t("Feeling", "احساس"), V95Icons.Feeling, Modifier.weight(1f)) {
                feelingIndex = (feelingIndex + 1) % feelings.size
            }
            V95Tool(c.t("Mention", "مینشن"), V95Icons.Mention, Modifier.weight(1f)) {
                text = if (text.endsWith(" ") || text.isBlank()) text + "@" else text + " @"
            }
        }

        if (c.composerCheckinName.isNotBlank() || feeling.isNotBlank()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (c.composerCheckinName.isNotBlank()) {
                    V95Button(
                        c.composerCheckinName.take(28),
                        Modifier.weight(1f),
                        icon = V95Icons.Pin
                    ) { c.startPostCheckin() }
                }
                if (feeling.isNotBlank()) {
                    V95Button(
                        c.t("Feeling: $feeling", "احساس: $feeling"),
                        Modifier.weight(1f),
                        icon = V95Icons.Feeling
                    ) { feelingIndex = (feelingIndex + 1) % feelings.size }
                }
            }
        }

        if (photo.isNotBlank()) {
            AsyncImage(
                photo,
                null,
                Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
        }
        if (video.isNotBlank()) V95NativeVideo(video)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val privacyLabel = when (privacy) {
                "followers" -> c.t("Followers", "فالوورز")
                "private" -> c.t("Only me", "صرف میں")
                else -> c.t("Everyone", "سب")
            }
            V95Button(privacyLabel, Modifier.weight(1f), icon = V95Icons.Shield) {
                privacy = when (privacy) {
                    "public" -> "followers"
                    "followers" -> "private"
                    else -> "public"
                }
            }
            V95Button(
                c.t("Post", "پوسٹ"),
                Modifier.weight(.7f),
                primary = true,
                enabled = text.isNotBlank() || photo.isNotBlank() || video.isNotBlank()
            ) {
                c.createPost(text, photo, video, privacy, feeling) {
                    text = ""
                    photo = ""
                    video = ""
                    privacy = "public"
                    feelingIndex = 0
                }
            }
        }
    }
}

@Composable
internal fun V95TextArea(value: String, placeholder: String, onValue: (String) -> Unit) {
    V95InputShell(Modifier.fillMaxWidth().heightIn(min = 92.dp)) {
        BasicTextField(value, onValue, Modifier.fillMaxWidth(), textStyle = TextStyle(color = V95Ink, fontSize = 15.sp), decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (value.isBlank()) Text(placeholder, color = Color(0xFF858EB1), fontSize = 14.sp)
                inner()
            }
        })
    }
}

@Composable
internal fun V95Tool(text: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(58.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(icon), null, Modifier.size(30.dp))
        Text(text, color = V95Purple, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
internal fun V95PostCard(c: V95Controller, post: Post) {
    V95GlassCard(radius = 28.dp, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            V95Avatar(post.user, 42.dp, Modifier.clickable { c.openProfile(post.user) })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.user.name, fontWeight = FontWeight.Black, color = V95Ink, fontSize = 13.sp)
                    if (post.user.verified) { Spacer(Modifier.width(4.dp)); Image(painterResource(V95Icons.Check), null, Modifier.size(18.dp)) }
                }
                Text("@${post.user.username} · ${v95FormatTime(post.createdAt)} · ${if (post.privacy == "followers") c.t("Followers", "فالوورز") else c.t("Everyone", "سب")}", color = V95Muted, fontSize = 10.sp)
            }
            var moreOpen by remember(post.id) { mutableStateOf(false) }
            Box {
                Image(
                    painterResource(V95Icons.More),
                    null,
                    Modifier.size(28.dp).clickable { moreOpen = true }
                )
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (post.saved) c.t("Remove from Saved", "محفوظ سے ہٹائیں") else c.t("Save post", "پوسٹ محفوظ کریں")) },
                        leadingIcon = { Image(painterResource(V95Icons.Save), null, Modifier.size(24.dp)) },
                        onClick = {
                            moreOpen = false
                            if (c.user == null) c.route = V95Route.AUTH else c.savePost(post)
                        }
                    )
                    if (c.user != null && (c.user?.id == post.user.id || c.user?.isAdmin == true)) {
                        DropdownMenuItem(
                            text = { Text(c.t("Delete post", "پوسٹ حذف کریں"), color = V95Danger) },
                            onClick = { moreOpen = false; c.deletePost(post) }
                        )
                    } else if (c.user != null) {
                        DropdownMenuItem(
                            text = { Text(c.t("Report post", "پوسٹ رپورٹ کریں")) },
                            leadingIcon = { Image(painterResource(V95Icons.Shield), null, Modifier.size(24.dp)) },
                            onClick = { moreOpen = false; c.reportPost(post) }
                        )
                    }
                }
            }
        }
        if (post.text.isNotBlank()) Text(post.text, color = V95Ink, fontSize = 13.sp, lineHeight = 19.5.sp)
        if (!post.photo.isNullOrBlank()) {
            AsyncImage(
                post.photo,
                null,
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 440.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { c.openPost(post) },
                contentScale = ContentScale.Crop
            )
        }
        if (!post.video.isNullOrBlank()) {
            V95NativeVideo(post.video)
        }
        if (!post.feeling.isNullOrBlank() || !post.checkin.isNullOrBlank()) {
            Text(listOfNotNull(post.feeling, post.checkin).joinToString(" · "), color = V95Muted, fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = .55f), RoundedCornerShape(1.dp)).padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            V95PostAction(
                if (post.liked) c.t("Liked", "لائکڈ") else c.t("Like", "لائک"),
                V95Icons.Heart,
                count = post.reactionTotal,
                active = post.liked
            ) { if (c.user == null) c.route = V95Route.AUTH else c.likePost(post) }
            V95PostAction(c.t("Comment", "کمنٹ"), V95Icons.Comment, count = post.comments) {
                if (c.user == null) c.route = V95Route.AUTH else c.openPost(post)
            }
            V95PostAction(c.t("Views", "ویوز"), V95Icons.Eye, count = post.views)
            V95PostAction(c.t("Share", "شیئر"), V95Icons.Share, count = post.shares) {
                if (c.user == null) c.route = V95Route.AUTH else c.sharePost(post)
            }
        }
    }
}

@Composable
internal fun V95PostAction(
    text: String,
    icon: Int,
    active: Boolean = false,
    count: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .widthIn(min = 64.dp)
            .height(44.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(25.dp))
        Spacer(Modifier.width(3.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text,
                color = if (active) V95Pink else V95Purple,
                fontWeight = FontWeight.Black,
                fontSize = 8.5.sp,
                maxLines = 1
            )
            if (count != null) {
                Text(
                    count.toString(),
                    color = V95Muted,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun V95People(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.people.isEmpty()) c.loadPeople() }
    V95PageList(title = c.t("People", "لوگ"), subtitle = c.t("People you may know", "لوگ جنہیں آپ جانتے ہوں"), items = c.people) { person ->
        V95GlassCard(radius = 22.dp, padding = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V95Avatar(person, 54.dp, Modifier.clickable { c.openProfile(person) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(person.name, color = V95Ink, fontWeight = FontWeight.Black)
                    Text("@${person.username}", color = V95Muted, fontSize = 12.sp)
                    if (person.village.isNotBlank() || person.city.isNotBlank()) Text(listOf(person.village, person.city).filter { it.isNotBlank() }.joinToString(" · "), color = V95Muted, fontSize = 11.sp)
                }
                if (c.user?.id != person.id) V95Button(if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), primary = !person.followed, icon = V95Icons.Follow) { if (c.user == null) c.route = V95Route.AUTH else c.follow(person) }
            }
        }
    }
}

@Composable
internal fun <T> V95PageList(title: String, subtitle: String, items: List<T>, row: @Composable (T) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { V95PageHeading(title, subtitle) }
        if (items.isEmpty()) item { V95Empty("No items") }
        items(items) { row(it) }
    }
}

@Composable
internal fun V95Shops(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.shops.isEmpty()) c.loadShops() }
    V95PageList(c.t("Shops", "دکانیں"), c.t("Discover local shops and services", "مقامی دکانیں اور سروسز تلاش کریں"), c.shops) { shop ->
        V95GlassCard(radius = 22.dp, padding = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V95ShopAvatar(shop, 58.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { c.openShop(shop) }) {
                    Text(shop.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(shop.category, color = V95Muted, fontSize = 12.sp)
                    Text(listOf(shop.village, shop.city).filter { it.isNotBlank() }.joinToString(" · "), color = V95Muted, fontSize = 11.sp)
                }
                V95Button(if (shop.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), primary = !shop.followed) { if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop) }
            }
        }
    }
}

@Composable
internal fun V95Messages(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.user != null && c.conversations.isEmpty()) c.loadMessages() }
    if (c.user == null) { V95RequireLogin(c); return }
    V95PageList(c.t("Messages", "پیغامات"), c.t("Your conversations", "آپ کی گفتگو"), c.conversations) { conv ->
        V95GlassCard(radius = 20.dp, padding = 10.dp) {
            Row(Modifier.fillMaxWidth().clickable { c.openChat(conv.user) }, verticalAlignment = Alignment.CenterVertically) {
                V95Avatar(conv.user, 50.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(conv.user.name, color = V95Ink, fontWeight = FontWeight.Black)
                    Text(conv.preview.ifBlank { c.t("Open conversation", "گفتگو کھولیں") }, color = V95Muted, fontSize = 12.sp, maxLines = 1)
                }
                if (conv.unread) Box(Modifier.size(10.dp).clip(CircleShape).background(V95Pink))
            }
        }
    }
}

@Composable
internal fun V95Chat(c: V95Controller) {
    if (c.user == null || c.selectedChatUser == null) { V95RequireLogin(c); return }

    Column(Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            V95Button(c.t("Back", "واپس")) { c.route = V95Route.MESSAGES }
            Spacer(Modifier.width(7.dp))
            V95Avatar(c.selectedChatUser!!, 38.dp)
            Spacer(Modifier.width(7.dp))
            V95PageHeading(c.selectedChatUser!!.name, "@${c.selectedChatUser!!.username}")
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(c.chatMessages, key = { it.id }) { m ->
                val mine = m.senderId == c.user?.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier
                            .fillMaxWidth(.78f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (mine) Brush.linearGradient(listOf(Color(0xFF6BD2FF).copy(.62f), Color(0xFF9F84FF).copy(.55f)))
                                else v95GlassBrush()
                            )
                            .border(1.dp, Color.White.copy(.86f), RoundedCornerShape(18.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (m.text.isNotBlank()) Text(m.text, color = V95Ink, fontSize = 12.5.sp, lineHeight = 18.sp)
                        if (!m.photo.isNullOrBlank()) {
                            AsyncImage(
                                m.photo,
                                null,
                                Modifier.fillMaxWidth().heightIn(max = 280.dp).clip(RoundedCornerShape(15.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (!m.audio.isNullOrBlank()) V95NativeAudio(m.audio)
                        if (m.locationLat != null && m.locationLng != null) {
                            V95Button(c.t("Open location", "لوکیشن کھولیں"), icon = V95Icons.Map) {
                                c.route = V95Route.MAP
                            }
                        }
                        if (m.createdAt.isNotBlank()) {
                            Text(m.createdAt.replace('T', ' ').take(16), color = V95Muted, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        V95MessageComposer(c)
    }
}


private val V95_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.getDefault())

private fun v95FormatTime(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        Instant.parse(raw).atZone(ZoneId.systemDefault()).format(V95_TIME_FORMATTER)
    }.getOrElse { raw.replace('T', ' ').take(16) }
}
