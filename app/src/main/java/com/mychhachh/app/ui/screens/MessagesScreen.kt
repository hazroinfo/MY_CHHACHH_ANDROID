package com.mychhachh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.mychhachh.app.data.Conversation
import com.mychhachh.app.data.Message
import com.mychhachh.app.data.User
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import com.mychhachh.app.ui.theme.JellyPurple

@Composable
fun MessagesScreen(conversations: List<Conversation>, loading: Boolean, error: String?, onOpen: (Long) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { PageTitle("Messages", "Your conversations", JellyIcons.Message) }
        if (loading && conversations.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && conversations.isEmpty() && error == null) item { EmptyCard("No conversations yet.", JellyIcons.Message) }
        items(conversations, key = { "conversation-${it.user.id}" }) { c ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp, onClick = { onOpen(c.user.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.user, 50.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        UserName(c.user, 14)
                        Text(c.preview.ifBlank { "Open conversation" }, color = JellyMuted, fontSize = 10.5f.sp, maxLines = 1)
                        Text(shortTime(c.createdAt), color = JellyMuted, fontSize = 8.5f.sp)
                    }
                    if (c.unread) Box(Modifier.size(9.dp).clip(RoundedCornerShape(99.dp)).background(JellyPurple))
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    me: User,
    other: User?,
    messages: List<Message>,
    loading: Boolean,
    error: String?,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        if (other != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(other, 38.dp)
                Spacer(Modifier.width(7.dp))
                Column {
                    UserName(other, 14)
                    if (other.username.isNotBlank()) Text("@${other.username}", color = JellyMuted, fontSize = 9.sp)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (loading) item { LoadingBlock() }
            error?.let { item { ErrorCard(it) } }
            items(messages, key = { "message-${it.id}" }) { m ->
                val mine = m.senderId == me.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier
                            .widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (mine) Brush.horizontalGradient(listOf(Color(0xFFFFE5F3), Color(0xFFEDE5FF)))
                                else Brush.linearGradient(listOf(Color.White, Color(0xFFEAF9FF)))
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (m.text.isNotBlank()) Text(m.text, color = JellyInk, fontSize = 13.5f.sp, lineHeight = 19.sp)
                        m.photo?.let {
                            AsyncImage(it, null, Modifier.widthIn(max = 260.dp).heightIn(max = 320.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        }
                        m.audio?.let { InlineAudioPlayer(it) }
                        Text(shortTime(m.createdAt), color = JellyMuted, fontSize = 8.sp)
                    }
                }
            }
        }

        JellyGlass(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), padding = 7.dp) {
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    text,
                    { text = it },
                    Modifier.weight(1f),
                    placeholder = { Text("Write a message") },
                    shape = RoundedCornerShape(17.dp),
                    maxLines = 4
                )
                Spacer(Modifier.width(6.dp))
                JellyButton("Send", primary = true, icon = JellyIcons.Send, enabled = text.isNotBlank()) {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                }
            }
        }
    }
}
