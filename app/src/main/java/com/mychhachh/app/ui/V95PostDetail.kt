package com.mychhachh.app.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mychhachh.app.data.Comment

@Composable
internal fun V95PostDetail(c: V95Controller) {
    val post = c.selectedPost
    if (post == null) {
        V95Empty(c.t("Post unavailable", "پوسٹ دستیاب نہیں"))
        return
    }

    var comment by remember(post.id) { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                V95Button(c.t("Back", "واپس")) { c.route = V95Route.HOME }
                Spacer(Modifier.width(8.dp))
                Text(
                    c.t("Post", "پوسٹ"),
                    color = V95Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }

        item { V95PostCard(c, post) }

        item {
            V95GlassCard(radius = 22.dp, padding = 12.dp) {
                Text(
                    c.t("Comments", "کمنٹس"),
                    color = V95Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                if (c.user == null) {
                    V95Button(c.t("Login to comment", "کمنٹ کے لیے لاگ اِن کریں"), primary = true) {
                        c.route = V95Route.AUTH
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V95InputShell(Modifier.weight(1f)) {
                            BasicTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = V95Ink, fontSize = 13.sp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (comment.isBlank()) {
                                            Text(
                                                c.t("Write a comment…", "کمنٹ لکھیں…"),
                                                color = Color(0xFF858EB1),
                                                fontSize = 12.sp
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                        V95Button(
                            c.t("Send", "بھیجیں"),
                            primary = true,
                            enabled = comment.isNotBlank()
                        ) {
                            c.addPostComment(comment) { comment = "" }
                        }
                    }
                }
            }
        }

        if (c.postCommentsList.isEmpty()) {
            item { V95Empty(c.t("No comments yet", "ابھی کوئی کمنٹ نہیں")) }
        } else {
            items(c.postCommentsList, key = { it.id }) { item ->
                V95CommentRow(c, item)
            }
        }
    }
}

@Composable
private fun V95CommentRow(c: V95Controller, item: Comment) {
    V95GlassCard(radius = 20.dp, padding = 10.dp) {
        Row(verticalAlignment = Alignment.Top) {
            V95Avatar(item.user, 38.dp, Modifier.clickable { c.openProfile(item.user) })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.user.name,
                    color = V95Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                if (item.createdAt.isNotBlank()) {
                    Text(
                        item.createdAt.replace('T', ' ').take(16),
                        color = V95Muted,
                        fontSize = 9.sp
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    item.text,
                    color = V95Ink,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
internal fun V95NativeVideo(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
            .border(2.dp, Color.White.copy(alpha = .92f), RoundedCornerShape(22.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Row(
            Modifier.fillMaxWidth().background(Color(0xCCFFFFFF)).padding(5.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            V95Button("−10s") {
                player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
            }
            Spacer(Modifier.width(8.dp))
            V95Button("+10s") {
                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                player.seekTo((player.currentPosition + 10_000L).coerceAtMost(duration))
            }
        }
    }
}
