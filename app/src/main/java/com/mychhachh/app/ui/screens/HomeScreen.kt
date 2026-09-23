package com.mychhachh.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.CheckinPlace
import com.mychhachh.app.data.Comment
import com.mychhachh.app.data.Post
import com.mychhachh.app.data.Shop
import com.mychhachh.app.data.User
import kotlinx.coroutines.launch
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.*

@Composable
fun HomeScreen(
    user: User?,
    mode: String,
    posts: List<Post>,
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    peopleSuggestions: List<User>,
    shopSuggestions: List<Shop>,
    onMode: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onProfile: (Long) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenShop: (Long) -> Unit,
    onFollowSuggestion: (Long) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onEditPost: (Post, String, String) -> Unit,
    onDeletePost: (Post) -> Unit,
    onReportPost: (Post, String) -> Unit,
    onSearchCheckin: suspend (String) -> List<CheckinPlace>,
    onSearchMentions: suspend (String) -> List<User>,
    onCreatePost: (String, String, String, String, Double?, Double?, Uri?, Uri?) -> Unit,
    onLoadMore: () -> Unit
) {
    var composing by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("public") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var feeling by remember { mutableStateOf("") }
    var checkin by remember { mutableStateOf("") }
    var checkinLat by remember { mutableStateOf<Double?>(null) }
    var checkinLng by remember { mutableStateOf<Double?>(null) }
    var feelingDialog by remember { mutableStateOf(false) }
    var checkinDialog by remember { mutableStateOf(false) }
    var mentionDialog by remember { mutableStateOf(false) }
    val uiScope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { photoUri = uri; videoUri = null }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { videoUri = uri; photoUri = null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 0.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(154.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFE8F6FF),
                                    Color(0xFFF6F8FB),
                                    Color(0xFFEAF4EE)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(
                        Modifier.align(Alignment.CenterStart),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Welcome to", color = JellyInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Brand(name = "My Chhachh", fontSize = 28)
                        Text(
                            "Connect with people for information",
                            color = JellyMuted,
                            fontSize = 10.5f.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(
                        Modifier.align(Alignment.CenterEnd),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("Good People", color = JellyInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("Brighter Days", color = LiveJellyTheme.activeColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (user != null && peopleSuggestions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("People You May Know", Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("See All ›", color = LiveJellyTheme.activeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenPeople() })
                    }
                    peopleSuggestions.filter { it.id != user.id }.take(2).forEach { person ->
                        JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 10.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(person, 42.dp)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    UserName(person, 12)
                                    Text("@${person.username}", color = JellyMuted, fontSize = 9.sp)
                                }
                                JellyButton(
                                    if (person.followed) "Following" else "Follow",
                                    primary = !person.followed
                                ) { onFollowSuggestion(person.id) }
                            }
                        }
                    }
                }
            }
        }

        if (shopSuggestions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Shop Suggestions", Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("See All ›", color = LiveJellyTheme.activeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenShops() })
                    }
                    shopSuggestions.take(2).forEach { shop ->
                        JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 10.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!shop.photo.isNullOrBlank()) {
                                    AsyncImage(
                                        shop.photo,
                                        shop.name,
                                        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    JellyIcon(JellyIcons.Shop, size = 38.dp)
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(shop.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text("@${shop.username}", color = JellyMuted, fontSize = 9.sp)
                                }
                                JellyButton("View Shop") { onOpenShop(shop.id) }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JellyPill("For You", mode == "global", Modifier.weight(1f)) { onMode("global") }
                if (user != null) JellyPill("Following", mode == "following", Modifier.weight(1f)) { onMode("following") }
                JellyPill("Shop Posts", mode == "shops", Modifier.weight(1f)) { onMode("shops") }
            }
        }

        if (user == null) {
            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Join My Chhachh", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Login to post, follow, comment and save.", color = JellyMuted, fontSize = 11.sp)
                        }
                        JellyButton("Login", onClick = onLogin)
                        Spacer(Modifier.width(6.dp))
                        JellyButton("Sign up", primary = true, onClick = onRegister)
                    }
                }
            }
        }

        if (user != null && mode != "shops") item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Avatar(user, 40.dp)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = composing,
                            onValueChange = { composing = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("What's on your mind? Use @ to mention", fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp),
                            minLines = 1,
                            maxLines = 4
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ComposerTool(JellyIcons.Photo, if (photoUri != null) "Photo ✓" else "Photo") { photoPicker.launch("image/*") }
                        ComposerTool(JellyIcons.Video, if (videoUri != null) "Video ✓" else "Video") { videoPicker.launch("video/*") }
                        ComposerTool(JellyIcons.Feeling, if (feeling.isNotBlank()) "Feeling ✓" else "Feeling") { feelingDialog = true }
                        ComposerTool(JellyIcons.Pin, if (checkin.isNotBlank()) "Check in ✓" else "Check in") { checkinDialog = true }
                        ComposerTool(JellyIcons.Mention, "Mention") { mentionDialog = true }
                    }
                    if (feeling.isNotBlank() || checkin.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (feeling.isNotBlank()) Tag(JellyIcons.Feeling, feeling)
                            if (checkin.isNotBlank()) Tag(JellyIcons.Pin, checkin)
                        }
                    }
                    if (photoUri != null || videoUri != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (photoUri != null) "Photo ready" else "Video ready",
                                Modifier.weight(1f),
                                color = JellyMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { photoUri = null; videoUri = null }) {
                                Text("Remove", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyButton(
                            if (privacy == "public") "Everyone" else "Followers",
                            icon = JellyIcons.Eye
                        ) { privacy = if (privacy == "public") "followers" else "public" }
                        Spacer(Modifier.weight(1f))
                        JellyButton("Post", primary = true, icon = JellyIcons.Send) {
                            if (composing.isNotBlank() || feeling.isNotBlank() || checkin.isNotBlank() || photoUri != null || videoUri != null) {
                                onCreatePost(composing.trim(), privacy, feeling, checkin, checkinLat, checkinLng, photoUri, videoUri)
                                composing = ""
                                feeling = ""
                                checkin = ""
                                checkinLat = null
                                checkinLng = null
                                photoUri = null
                                videoUri = null
                            }
                        }
                    }
                }
            }
        }

        if (loading && posts.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        items(posts, key = { "post-${it.id}" }) { post ->
            PostCard(
                post = post,
                loggedIn = user != null,
                onLogin = onLogin,
                onProfile = { onProfile(post.user.id) },
                onLike = onLike,
                onComment = onComment,
                onShare = onShare,
                onSave = onSave,
                onEdit = if ((user?.id == post.user.id || user?.isAdmin == true) && post.shopId == 0L) onEditPost else null,
                onDelete = if ((user?.id == post.user.id || user?.isAdmin == true) && post.shopId == 0L) onDeletePost else null,
                onReport = if (user != null && user.id != post.user.id && !user.isAdmin && post.shopId == 0L) {
                    { p, reason -> onReportPost(p, reason) }
                } else null
            )
        }
        if (!loading && posts.isEmpty() && error == null) {
            item { EmptyCard(if (mode == "shops") "No shop posts yet." else "No posts yet.") }
        }
        if (hasMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    JellyButton(if (loading) "Loading…" else "Load more", enabled = !loading, onClick = onLoadMore)
                }
            }
        }
    }
    if (feelingDialog) {
        val feelings = listOf(
            "😊" to "Happy",
            "❤️" to "Loved",
            "🎉" to "Celebrating",
            "🙏" to "Grateful",
            "🤔" to "Thinking",
            "😢" to "Sad",
            "😡" to "Angry"
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { feelingDialog = false },
            title = { Text("Feeling", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    feelings.forEach { (emoji, label) ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 9.dp,
                            onClick = {
                                feeling = label
                                feelingDialog = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 22.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(label, color = JellyInk, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (feeling.isNotBlank()) {
                        JellyButton("Remove feeling", Modifier.fillMaxWidth()) {
                            feeling = ""
                            feelingDialog = false
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { feelingDialog = false } }
        )
    }

    if (mentionDialog) {
        var query by remember { mutableStateOf("") }
        var users by remember { mutableStateOf<List<User>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        var searchError by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mentionDialog = false },
            title = { Text("Mention someone", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search name or username") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                    JellyButton(
                        if (searching) "Searching…" else "Search people",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Search,
                        enabled = !searching && query.isNotBlank()
                    ) {
                        uiScope.launch {
                            searching = true
                            searchError = null
                            try {
                                users = onSearchMentions(query.trim())
                                if (users.isEmpty()) searchError = "No people found."
                            } catch (e: Exception) {
                                users = emptyList()
                                searchError = e.message ?: "Could not search people."
                            } finally {
                                searching = false
                            }
                        }
                    }
                    searchError?.let { Text(it, color = Color(0xFFB23A55), fontSize = 10.sp) }
                    users.take(8).forEach { person ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 8.dp,
                            onClick = {
                                val mention = "@${person.username}"
                                composing = when {
                                    composing.isBlank() -> mention
                                    composing.endsWith(" ") -> composing + mention
                                    else -> composing + " " + mention
                                }
                                mentionDialog = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(person, 34.dp)
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    UserName(person, 11)
                                    Text("@${person.username}", color = JellyMuted, fontSize = 8.5f.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { mentionDialog = false } }
        )
    }

    if (checkinDialog) {
        var query by remember { mutableStateOf(checkin) }
        var places by remember { mutableStateOf<List<CheckinPlace>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        var searchError by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { checkinDialog = false },
            title = { Text("Check in", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a real place in Chhachh / Hazro.", color = JellyMuted, fontSize = 10.5f.sp)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search a place") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                    JellyButton(
                        if (searching) "Searching…" else "Find place",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Search,
                        enabled = !searching && query.isNotBlank()
                    ) {
                        uiScope.launch {
                            searching = true
                            searchError = null
                            try {
                                places = onSearchCheckin(query.trim())
                                if (places.isEmpty()) searchError = "Place could not be found."
                            } catch (e: Exception) {
                                places = emptyList()
                                searchError = e.message ?: "Place could not be found."
                            } finally {
                                searching = false
                            }
                        }
                    }
                    searchError?.let { Text(it, color = Color(0xFFB23A55), fontSize = 10.sp) }
                    places.forEach { place ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 8.dp,
                            onClick = {
                                checkin = place.name.split(",").take(3).joinToString(", ").trim()
                                checkinLat = place.lat
                                checkinLng = place.lng
                                checkinDialog = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                JellyIcon(JellyIcons.Pin, size = 24.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(place.name, Modifier.weight(1f), color = JellyInk, fontSize = 10.sp, maxLines = 2)
                                Text("Select", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                        }
                    }
                    if (checkin.isNotBlank()) {
                        JellyButton("Remove check-in", Modifier.fillMaxWidth()) {
                            checkin = ""
                            checkinLat = null
                            checkinLng = null
                            checkinDialog = false
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { checkinDialog = false } }
        )
    }

}

@Composable
private fun ComposerTool(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .heightIn(min = 46.dp).padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        JellyIcon(icon, size = 24.dp)
        Text(label, color = JellyInk, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
fun PostCard(
    post: Post,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onProfile: () -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onEdit: ((Post, String, String) -> Unit)? = null,
    onDelete: ((Post) -> Unit)? = null,
    onReport: ((Post, String) -> Unit)? = null
) {
    var videoOpen by remember(post.id) { mutableStateOf(false) }
    var moreOpen by remember(post.id) { mutableStateOf(false) }
    var editOpen by remember(post.id) { mutableStateOf(false) }
    var deleteConfirm by remember(post.id) { mutableStateOf(false) }
    var reportOpen by remember(post.id) { mutableStateOf(false) }
    val context = LocalContext.current

    JellyGlass(Modifier.fillMaxWidth(), radius = 28.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(post.user, 42.dp, Modifier.clickable { onProfile() })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f).clickable { onProfile() }) {
                    UserName(post.user, 14)
                    Text(
                        "${shortTime(post.createdAt)}  •  ${if (post.privacy == "followers") "Followers" else "Everyone"}",
                        color = JellyMuted,
                        fontSize = 9.5f.sp
                    )
                }
                if (loggedIn) {
                    JellyIconButton(JellyIcons.More, "More", onClick = { moreOpen = true })
                }
            }

            if (!post.feeling.isNullOrBlank() || !post.checkin.isNullOrBlank()) {
                Row(Modifier.padding(horizontal = 11.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.feeling?.let { Tag(JellyIcons.Feeling, it) }
                    post.checkin?.let { label ->
                        val lat = post.checkinLat
                        val lng = post.checkinLng
                        Tag(
                            JellyIcons.Pin,
                            label,
                            onClick = if (lat != null && lng != null) {
                                {
                                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                }
                            } else null
                        )
                    }
                }
            }

            if (post.text.isNotBlank()) {
                Text(
                    post.text,
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = JellyInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            post.photo?.let { url ->
                AsyncImage(
                    url,
                    null,
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 7.dp)
                        .heightIn(min = 170.dp, max = 520.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }

            post.video?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(7.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFEAF9FF), Color(0xFFF0E9FF))))
                        .clickable { videoOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        JellyIcon(JellyIcons.Video, size = 48.dp)
                        Text("Play video", color = JellyInk, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        Text("Opens inside the app", color = JellyMuted, fontSize = 9.sp)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PostAction(JellyIcons.Heart, if (post.liked) "Liked" else "Like", post.likes, Modifier.weight(1f)) { if (loggedIn) onLike(post) else onLogin() }
                PostAction(JellyIcons.Comment, "Comment", post.comments, Modifier.weight(1f)) { if (loggedIn) onComment(post) else onLogin() }
                PostStat(JellyIcons.Eye, "Views", post.views, Modifier.weight(1f))
                PostAction(JellyIcons.Share, "Share", post.shares, Modifier.weight(1f)) { if (loggedIn) onShare(post) else onLogin() }
            }
        }
    }

    if (videoOpen) post.video?.let { VideoDialog(it) { videoOpen = false } }

    if (moreOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { moreOpen = false },
            title = { Text("Post options", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.shopId == 0L) {
                        JellyButton(
                            if (post.saved) "Remove from Saved" else "Save Post",
                            Modifier.fillMaxWidth(),
                            primary = !post.saved,
                            icon = JellyIcons.Save
                        ) {
                            onSave(post)
                            moreOpen = false
                        }
                    }
                    if (onEdit != null) {
                        JellyButton("Edit Post", Modifier.fillMaxWidth(), icon = JellyIcons.Edit) {
                            moreOpen = false
                            editOpen = true
                        }
                    }
                    if (onDelete != null) {
                        JellyButton("Delete Post", Modifier.fillMaxWidth(), icon = JellyIcons.Delete) {
                            moreOpen = false
                            deleteConfirm = true
                        }
                    }
                    if (onReport != null) {
                        JellyButton("Report Post", Modifier.fillMaxWidth(), icon = JellyIcons.Shield, danger = true) {
                            moreOpen = false
                            reportOpen = true
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { moreOpen = false } }
        )
    }

    if (editOpen && onEdit != null) {
        var editText by remember(post.id) { mutableStateOf(post.text) }
        var editPrivacy by remember(post.id) { mutableStateOf(post.privacy) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text("Edit Post", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 7,
                        shape = RoundedCornerShape(18.dp)
                    )
                    JellyButton(
                        if (editPrivacy == "followers") "Followers" else "Everyone",
                        icon = JellyIcons.Eye
                    ) {
                        editPrivacy = if (editPrivacy == "followers") "public" else "followers"
                    }
                }
            },
            confirmButton = {
                JellyButton("Save", primary = true, icon = JellyIcons.Check) {
                    onEdit(post, editText.trim(), editPrivacy)
                    editOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { editOpen = false } }
        )
    }

    if (deleteConfirm && onDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("Delete Post", color = JellyInk, fontWeight = FontWeight.Black) },
            text = { Text("Delete this post permanently?", color = JellyInk) },
            confirmButton = {
                JellyButton("Delete", icon = JellyIcons.Delete, danger = true) {
                    onDelete(post)
                    deleteConfirm = false
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteConfirm = false } }
        )
    }

    if (reportOpen && onReport != null) {
        var reason by remember(post.id) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("Report Post", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    reason,
                    { reason = it.take(3000) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Explain the problem…") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Send Report", primary = true, icon = JellyIcons.Shield, enabled = reason.trim().length >= 3) {
                    onReport(post, reason.trim())
                    reportOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { reportOpen = false } }
        )
    }
}

@Composable
private fun Tag(icon: Int, text: String, onClick: (() -> Unit)? = null) {
    var modifier = Modifier
        .clip(RoundedCornerShape(13.dp))
        .background(Color.White.copy(.86f))
    if (onClick != null) modifier = modifier.clickable { onClick() }
    Row(
        modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JellyIcon(icon, size = 18.dp)
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 9.5f.sp, color = JellyInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PostAction(icon: Int, label: String, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    JellyGlass(modifier.height(55.dp), radius = 18.dp, padding = 3.dp, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            JellyIcon(icon, size = 23.dp)
            Text(
                if (count > 0) "$label $count" else label,
                color = JellyInk,
                fontSize = 7.7f.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PostStat(icon: Int, label: String, count: Int, modifier: Modifier = Modifier) {
    JellyGlass(modifier.height(55.dp), radius = 18.dp, padding = 3.dp) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            JellyIcon(icon, size = 23.dp)
            Text(
                if (count > 0) "$label $count" else label,
                color = JellyInk,
                fontSize = 7.7f.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}


@Composable
fun PostDiscussionDialog(
    post: Post,
    comments: List<Comment>,
    likesUsers: List<User>,
    loading: Boolean,
    error: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSend: (String, Long) -> Unit,
    onLikeComment: (Long) -> Unit,
    onProfile: (Long) -> Unit
) {
    var mode by remember(post.id) { mutableStateOf("comments") }
    var text by remember(post.id) { mutableStateOf("") }
    var replyTo by remember(post.id) { mutableStateOf<Comment?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JellyPill("Comments ${post.comments}", mode == "comments", Modifier.weight(1f)) { mode = "comments" }
                JellyPill("Likes ${post.likes}", mode == "likes", Modifier.weight(1f)) { mode = "likes" }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) LoadingBlock()
                error?.let { ErrorCard(it) }

                if (!loading && mode == "comments") {
                    val roots = comments.filter { it.parentId == 0L }
                    if (roots.isEmpty()) {
                        Text("No comments yet.", color = JellyMuted, fontSize = 11.sp)
                    } else {
                        LazyColumn(
                            Modifier.weight(1f, fill = false).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(roots, key = { "comment-${it.id}" }) { comment ->
                                CommentRow(comment, false, onProfile, onLikeComment) { replyTo = comment }
                                comments.filter { it.parentId == comment.id }.forEach { reply ->
                                    Spacer(Modifier.height(3.dp))
                                    CommentRow(reply, true, onProfile, onLikeComment) { replyTo = comment }
                                }
                            }
                        }
                    }

                    replyTo?.let {
                        JellyGlass(Modifier.fillMaxWidth(), radius = 14.dp, padding = 7.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Replying to @${it.user.username}", Modifier.weight(1f), color = JellyMuted, fontSize = 9.5f.sp)
                                JellyButton("Cancel") { replyTo = null }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            text,
                            { text = it.take(3000) },
                            Modifier.weight(1f),
                            placeholder = { Text(if (replyTo == null) "Write a comment…" else "Write a reply…") },
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        JellyIconButton(JellyIcons.Send, "Send") {
                            if (!busy && text.isNotBlank()) {
                                onSend(text.trim(), replyTo?.id ?: 0L)
                                text = ""
                                replyTo = null
                            }
                        }
                    }
                }

                if (!loading && mode == "likes") {
                    if (likesUsers.isEmpty()) {
                        Text(
                            if (post.shopId > 0) "Like list is not exposed by the shop-post API." else "No likes yet.",
                            color = JellyMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        LazyColumn(
                            Modifier.weight(1f, fill = false).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(likesUsers, key = { "like-user-${it.id}" }) { user ->
                                JellyGlass(
                                    Modifier.fillMaxWidth(),
                                    radius = 15.dp,
                                    padding = 8.dp,
                                    onClick = { onProfile(user.id) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Avatar(user, 38.dp)
                                        Spacer(Modifier.width(7.dp))
                                        Column {
                                            UserName(user, 11)
                                            if (user.username.isNotBlank()) Text("@${user.username}", color = JellyMuted, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { JellyButton("Close", onClick = onDismiss) }
    )
}

@Composable
private fun CommentRow(
    comment: Comment,
    reply: Boolean,
    onProfile: (Long) -> Unit,
    onLike: (Long) -> Unit,
    onReply: () -> Unit
) {
    JellyGlass(
        Modifier.fillMaxWidth().padding(start = if (reply) 26.dp else 0.dp),
        radius = 16.dp,
        padding = 8.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(comment.user, if (reply) 30.dp else 34.dp, Modifier.clickable { onProfile(comment.user.id) })
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    UserName(comment.user, if (reply) 10 else 11)
                    Text(shortTime(comment.createdAt), color = JellyMuted, fontSize = 8.sp)
                }
            }
            Text(comment.text, color = JellyInk, fontSize = if (reply) 10.5f.sp else 11.5f.sp, lineHeight = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                JellyButton(if (comment.liked) "Liked ${comment.likes}" else "Like ${comment.likes}", icon = JellyIcons.Heart) {
                    onLike(comment.id)
                }
                JellyButton("Reply", icon = JellyIcons.Reply, onClick = onReply)
            }
        }
    }
}
