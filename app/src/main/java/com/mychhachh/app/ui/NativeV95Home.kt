package com.mychhachh.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.Post
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun NativeHome(c: V95Controller) {
    LaunchedEffect(c.feedMode) {
        if (c.feed.isEmpty()) c.loadFeed(false)
    }
    val guest = c.user == null
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 7.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (guest) item { NativeGuestCard(c) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NativeFeedTab(c.t("For You", "آپ کے لیے"), c.feedMode == "for_you", Modifier.weight(1f)) { c.changeFeed("for_you") }
                if (!guest) {
                    NativeFeedTab(c.t("Following", "فالوونگ"), c.feedMode == "following", Modifier.weight(1f)) { c.changeFeed("following") }
                }
                NativeFeedTab(c.t("Shop Posts", "دکان پوسٹس"), c.feedMode == "shops", Modifier.weight(1f)) { c.changeFeed("shops") }
                if (guest) Spacer(Modifier.weight(1f))
            }
        }
        if (!guest && c.feedMode != "shops") item { NativeComposer(c) }
        if (c.feed.isEmpty() && !c.busy) item { NVEmpty(c.t("No posts yet", "ابھی کوئی پوسٹ نہیں")) }
        items(c.feed, key = { it.id }) { post -> NativePostCard(c, post) }
    }
}

@Composable
private fun NativeFeedTab(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.height(44.dp).clip(shape)
            .background(if (active) nvPrimaryBrush() else Brush.linearGradient(listOf(Color.White.copy(.82f), Color.White.copy(.64f))))
            .border(1.5.dp, Color.White.copy(.96f), shape)
            .clickable(onClick = onClick).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) Color.White else NVPurple, fontWeight = FontWeight.Black, fontSize = 10.5.sp, maxLines = 1)
    }
}

@Composable
private fun NativeGuestCard(c: V95Controller) {
    NVCard(radius = 28.dp, padding = 14.dp) {
        Text(c.t("Welcome to My Chhachh", "My Chhachh میں خوش آمدید"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(
            c.t(
                "Browse public posts. Login only when you want to post, like, comment or message.",
                "عوامی پوسٹس دیکھیں۔ پوسٹ، لائک، کمنٹ یا پیغام کے لیے لاگ اِن کریں۔"
            ),
            color = NVMuted, fontSize = 11.sp, lineHeight = 16.sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            NVButton(c.t("Login", "لاگ اِن"), Modifier.weight(1f)) { c.route = V95Route.AUTH }
            NVButton(c.t("Create account", "اکاؤنٹ بنائیں"), Modifier.weight(1f), primary = true) { c.route = V95Route.AUTH }
        }
    }
}

@Composable
private fun NativeComposer(c: V95Controller) {
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var video by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "post_photo") { photo = it }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "post_video") { video = it }
    }

    NVCard(radius = 28.dp, padding = 14.dp) {
        Text(c.t("Create post", "پوسٹ بنائیں"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
        val shape = RoundedCornerShape(20.dp)
        Box(
            Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(shape)
                .background(nvInputBrush()).border(1.5.dp, Color.White, shape).padding(12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = NVInk, fontSize = 13.sp),
                decorationBox = { inner ->
                    if (text.isBlank()) Text(c.t("What's happening in Chhachh?", "چھچھ میں کیا ہو رہا ہے؟"), color = Color(0xFF858EB1), fontSize = 12.sp)
                    inner()
                }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NativeComposerTool(c.t("Photo", "فوٹو"), NVIcons.Photo, Modifier.weight(1f)) { photoPicker.launch("image/*") }
            NativeComposerTool(c.t("Video", "ویڈیو"), NVIcons.Video, Modifier.weight(1f)) { videoPicker.launch("video/*") }
            NativeComposerTool(c.t("Check in", "چیک اِن"), NVIcons.Pin, Modifier.weight(1f)) { c.startPostCheckin() }
            NativeComposerTool(c.t("Feeling", "احساس"), NVIcons.Feeling, Modifier.weight(1f)) {
                feeling = if (feeling.isBlank()) c.t("Happy", "خوش") else ""
            }
            NativeComposerTool(c.t("Mention", "مینشن"), NVIcons.Mention, Modifier.weight(1f)) {
                text = if (text.endsWith(" ") || text.isBlank()) text + "@" else text + " @"
            }
        }
        if (c.composerCheckinName.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(NVIcons.Pin), null, Modifier.size(22.dp))
                Spacer(Modifier.width(5.dp))
                Text(c.composerCheckinName, color = NVMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(c.t("Remove", "ہٹائیں"), color = NVPink, fontSize = 10.sp, modifier = Modifier.clickable { c.cancelPostCheckin() })
            }
        }
        if (feeling.isNotBlank()) Text("☺ " + feeling, color = NVPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (photo.isNotBlank()) {
            AsyncImage(photo, null, Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
        }
        if (video.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(NVIcons.Video), null, Modifier.size(26.dp))
                Spacer(Modifier.width(6.dp))
                Text(c.t("Video ready", "ویڈیو تیار ہے"), color = NVGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NVButton(c.t("Public", "پبلک"), Modifier.weight(1f)) { }
            NVButton(
                c.t("Post", "پوسٹ"),
                Modifier.weight(.72f),
                primary = true,
                enabled = text.isNotBlank() || photo.isNotBlank() || video.isNotBlank()
            ) {
                c.createPost(text, photo, video, feeling = feeling) {
                    text = ""; photo = ""; video = ""; feeling = ""
                }
            }
        }
    }
}

@Composable
private fun NativeComposerTool(text: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(29.dp))
        Text(text, color = NVPurple, fontWeight = FontWeight.Black, fontSize = 8.2.sp, maxLines = 1)
    }
}

@Composable
internal fun NativePostCard(c: V95Controller, post: Post, compact: Boolean = false) {
    NVCard(
        modifier = Modifier.clickable { c.openPost(post) },
        radius = 28.dp,
        padding = if (compact) 11.dp else 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NVAvatar(post.user, if (compact) 38.dp else 42.dp, Modifier.clickable { c.openProfile(post.user) })
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.user.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                    if (post.user.verified) {
                        Spacer(Modifier.width(3.dp))
                        Image(painterResource(NVIcons.Check), null, Modifier.size(16.dp))
                    }
                }
                Text("@" + post.user.username + " · " + nvFormatDate(post.createdAt), color = NVMuted, fontSize = 9.5.sp, maxLines = 1)
            }
            Image(painterResource(NVIcons.More), null, Modifier.size(26.dp))
        }
        if (post.text.isNotBlank()) Text(post.text, color = NVInk, fontSize = 13.sp, lineHeight = 19.sp)
        if (!post.feeling.isNullOrBlank() || !post.checkin.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!post.checkin.isNullOrBlank()) {
                    Image(painterResource(NVIcons.Pin), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(listOfNotNull(post.feeling, post.checkin).joinToString(" · "), color = NVMuted, fontSize = 10.sp)
            }
        }
        if (!post.photo.isNullOrBlank()) {
            AsyncImage(
                post.photo, null,
                Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 420.dp).clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
        }
        if (!post.video.isNullOrBlank()) {
            Box(
                Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFE7F8FF), Color(0xFFEFE4FF)))),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(NVIcons.Video), null, Modifier.size(58.dp))
            }
        }
        if (post.reactionTotal > 0 || post.comments > 0 || post.shares > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (post.reactionTotal > 0) post.reactionTotal.toString() + " " + c.t("reactions", "ری ایکشن") else "",
                    color = NVMuted, fontSize = 9.5.sp
                )
                val right = buildString {
                    if (post.comments > 0) append(post.comments.toString() + " " + c.t("comments", "کمنٹس"))
                    if (post.comments > 0 && post.shares > 0) append(" · ")
                    if (post.shares > 0) append(post.shares.toString() + " " + c.t("shares", "شیئر"))
                }
                Text(right, color = NVMuted, fontSize = 9.5.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            NativePostAction(if (post.liked) c.t("Liked", "لائکڈ") else c.t("Like", "لائک"), NVIcons.Heart, post.liked) {
                if (c.user == null) c.route = V95Route.AUTH else c.likePost(post)
            }
            NativePostAction(c.t("Comment", "کمنٹ"), NVIcons.Comment) {
                if (c.user == null) c.route = V95Route.AUTH else c.openPost(post)
            }
            NativePostAction(c.t("Share", "شیئر"), NVIcons.Share) {
                if (c.user == null) c.route = V95Route.AUTH else c.sharePost(post)
            }
            NativePostAction(if (post.saved) c.t("Saved", "محفوظ") else c.t("Save", "سیو"), NVIcons.Save, post.saved) {
                if (c.user == null) c.route = V95Route.AUTH else c.savePost(post)
            }
        }
    }
}

@Composable
private fun NativePostAction(text: String, icon: Int, active: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 58.dp).height(44.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(24.dp))
        Text(text, color = if (active) NVPink else NVPurple, fontWeight = FontWeight.Black, fontSize = 8.6.sp, maxLines = 1)
    }
}

private val NV_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.getDefault())

private fun nvFormatDate(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        Instant.parse(raw).atZone(ZoneId.systemDefault()).format(NV_DATE_FORMATTER)
    }.getOrElse { raw.replace('T', ' ').replace("Z", "").take(16) }
}
