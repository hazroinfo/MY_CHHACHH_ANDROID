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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier.height(43.dp).clip(shape)
            .background(if (active) Brush.linearGradient(listOf(Color(0xFFFF6EC0), Color(0xFFFF4CAD), Color(0xFFB671F1))) else Brush.linearGradient(listOf(Color.White.copy(.82f), Color.White.copy(.64f))))
            .border(1.5.dp, Color.White.copy(.96f), shape)
            .clickable(onClick = onClick).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) Color.White else NVPurple, fontWeight = FontWeight.Black, fontSize = 10.5.sp, maxLines = 1)
    }
}

@Composable
private fun NativeGuestCard(c: V95Controller) {
    NVCard(radius = 22.dp, padding = 16.dp) {
        Text(c.t("Welcome to My Chhachh", "My Chhachh میں خوش آمدید"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(
            c.t(
                "Browse public posts, or sign in to like, comment, message and post.",
                "عوامی پوسٹس دیکھیں، اور لائک، کمنٹ، پیغام یا پوسٹ کے لیے لاگ اِن کریں۔"
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
    var privacy by remember { mutableStateOf("public") }
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
                val options = listOf(
                    c.t("Happy", "خوش"),
                    c.t("Excited", "پرجوش"),
                    c.t("Thankful", "شکر گزار"),
                    c.t("Sad", "اداس")
                )
                val current = options.indexOf(feeling)
                feeling = if (current < 0) options.first() else if (current == options.lastIndex) "" else options[current + 1]
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
            val privacyLabel = when (privacy) {
                "followers" -> c.t("Followers", "فالوورز")
                "private" -> c.t("Only me", "صرف میں")
                else -> c.t("Public", "پبلک")
            }
            NVButton(privacyLabel, Modifier.weight(1f)) {
                privacy = when (privacy) {
                    "public" -> "followers"
                    "followers" -> "private"
                    else -> "public"
                }
            }
            NVButton(
                c.t("Post", "پوسٹ"),
                Modifier.weight(.72f),
                primary = true,
                enabled = text.isNotBlank() || photo.isNotBlank() || video.isNotBlank()
            ) {
                c.createPost(text, photo, video, privacy = privacy, feeling = feeling) {
                    text = ""; photo = ""; video = ""; feeling = ""; privacy = "public"
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
    var menuOpen by remember(post.id) { mutableStateOf(false) }
    var editOpen by remember(post.id) { mutableStateOf(false) }
    var deleteConfirm by remember(post.id) { mutableStateOf(false) }
    var editText by remember(post.id) { mutableStateOf(post.text) }
    var editPrivacy by remember(post.id) { mutableStateOf(post.privacy.ifBlank { "public" }) }
    val canManage = c.user?.id == post.user.id || c.user?.isAdmin == true

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
            Box {
                Image(
                    painterResource(NVIcons.More),
                    null,
                    Modifier.size(30.dp).clickable { menuOpen = true }
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (post.saved) c.t("Saved", "محفوظ") else c.t("Save post", "پوسٹ محفوظ کریں")) },
                        onClick = {
                            menuOpen = false
                            if (c.user == null) c.route = V95Route.AUTH else c.savePost(post)
                        }
                    )
                    if (canManage) {
                        DropdownMenuItem(
                            text = { Text(c.t("Edit post", "پوسٹ میں ترمیم")) },
                            onClick = {
                                menuOpen = false
                                editText = post.text
                                editPrivacy = post.privacy.ifBlank { "public" }
                                editOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(c.t("Delete post", "پوسٹ حذف کریں"), color = NVDanger) },
                            onClick = {
                                menuOpen = false
                                deleteConfirm = true
                            }
                        )
                    } else if (c.user != null) {
                        DropdownMenuItem(
                            text = { Text(c.t("Report post", "پوسٹ رپورٹ کریں"), color = NVDanger) },
                            onClick = {
                                menuOpen = false
                                c.reportPost(post)
                            }
                        )
                    }
                }
            }
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
            NativeVideoPlayer(post.video!!)
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

    if (editOpen) {
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text(c.t("Edit post", "پوسٹ میں ترمیم"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(shape)
                            .background(nvInputBrush()).border(1.dp, Color.White, shape).padding(10.dp)
                    ) {
                        BasicTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = NVInk, fontSize = 13.sp)
                        )
                    }
                    val privacyLabel = when (editPrivacy) {
                        "followers" -> c.t("Followers", "فالوورز")
                        "private" -> c.t("Only me", "صرف میں")
                        else -> c.t("Public", "پبلک")
                    }
                    NVButton(privacyLabel, Modifier.fillMaxWidth()) {
                        editPrivacy = when (editPrivacy) {
                            "public" -> "followers"
                            "followers" -> "private"
                            else -> "public"
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        c.editPost(post, editText, editPrivacy) { editOpen = false }
                    },
                    enabled = editText.isNotBlank() || !post.photo.isNullOrBlank() || !post.video.isNullOrBlank()
                ) { Text(c.t("Save", "محفوظ کریں"), fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { editOpen = false }) { Text(c.t("Cancel", "منسوخ")) }
            }
        )
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(c.t("Delete post?", "پوسٹ حذف کریں؟"), fontWeight = FontWeight.Black) },
            text = { Text(c.t("This post will be permanently deleted.", "یہ پوسٹ مستقل طور پر حذف ہو جائے گی۔")) },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirm = false
                    c.deletePost(post)
                }) { Text(c.t("Delete", "حذف کریں"), color = NVDanger, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) { Text(c.t("Cancel", "منسوخ")) }
            }
        )
    }
}

@Composable
private fun NativeVideoPlayer(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp))
    )
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
