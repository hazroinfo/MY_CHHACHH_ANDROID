package com.mychhachh.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.Post
import com.mychhachh.app.data.User
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
    onMode: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onProfile: (Long) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onCreatePost: (String, String, String, String, Uri?, Uri?) -> Unit,
    onLoadMore: () -> Unit
) {
    var composing by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("public") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var feeling by remember { mutableStateOf("") }
    var checkin by remember { mutableStateOf("") }
    var feelingDialog by remember { mutableStateOf(false) }
    var checkinDialog by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { photoUri = uri; videoUri = null }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { videoUri = uri; photoUri = null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 7.dp, end = 7.dp, top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
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
            JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Avatar(user, 40.dp)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = composing,
                            onValueChange = { composing = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("What's on your mind? Use @ to mention", fontSize = 11.sp) },
                            shape = RoundedCornerShape(18.dp),
                            minLines = 2,
                            maxLines = 5
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ComposerTool(JellyIcons.Photo, if (photoUri != null) "Photo ✓" else "Photo") { photoPicker.launch("image/*") }
                        ComposerTool(JellyIcons.Video, if (videoUri != null) "Video ✓" else "Video") { videoPicker.launch("video/*") }
                        ComposerTool(JellyIcons.Feeling, if (feeling.isNotBlank()) "Feeling ✓" else "Feeling") { feelingDialog = true }
                        ComposerTool(JellyIcons.Pin, if (checkin.isNotBlank()) "Check in ✓" else "Check in") { checkinDialog = true }
                        ComposerTool(JellyIcons.Mention, "Mention") {
                            composing = if (composing.isBlank()) "@" else if (composing.endsWith(" ")) composing + "@" else composing + " @"
                        }
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
                            if (composing.isNotBlank() || photoUri != null || videoUri != null) {
                                onCreatePost(composing.trim(), privacy, feeling, checkin, photoUri, videoUri)
                                composing = ""
                                feeling = ""
                                checkin = ""
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
            PostCard(post, user != null, onLogin, { onProfile(post.user.id) }, onLike, onComment, onShare, onSave)
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
        var draft by remember { mutableStateOf(feeling) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { feelingDialog = false },
            title = { Text("Feeling", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Happy, excited, thankful…") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Add", primary = true) {
                    feeling = draft.trim()
                    feelingDialog = false
                }
            },
            dismissButton = { JellyButton("Cancel") { feelingDialog = false } }
        )
    }

    if (checkinDialog) {
        var draft by remember { mutableStateOf(checkin) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { checkinDialog = false },
            title = { Text("Check in", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Village, mohalla, shop or place") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Add", primary = true) {
                    checkin = draft.trim()
                    checkinDialog = false
                }
            },
            dismissButton = { JellyButton("Cancel") { checkinDialog = false } }
        )
    }

}

@Composable
private fun ComposerTool(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 5.dp)
    ) {
        JellyIcon(icon, size = 27.dp)
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
    onSave: (Post) -> Unit
) {
    var videoOpen by remember(post.id) { mutableStateOf(false) }

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
            }

            if (!post.feeling.isNullOrBlank() || !post.checkin.isNullOrBlank()) {
                Row(Modifier.padding(horizontal = 11.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.feeling?.let { Tag(JellyIcons.Feeling, it) }
                    post.checkin?.let { Tag(JellyIcons.Pin, it) }
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
                if (loggedIn) PostAction(JellyIcons.Save, if (post.saved) "Saved" else "Save", 0, Modifier.weight(1f)) { onSave(post) }
            }
        }
    }

    if (videoOpen) post.video?.let { VideoDialog(it) { videoOpen = false } }
}

@Composable
private fun Tag(icon: Int, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(.86f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
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
