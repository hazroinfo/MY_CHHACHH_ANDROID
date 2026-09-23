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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.mychhachh.app.data.Conversation
import com.mychhachh.app.data.Message
import com.mychhachh.app.data.User
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import com.mychhachh.app.ui.theme.JellyPurple
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(conversations: List<Conversation>, loading: Boolean, error: String?, onOpen: (Long) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { PageTitle("Messages", "Good conversations build a brighter Chhachh", JellyIcons.Message) }
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
    onSearchLocation: suspend (String) -> List<CheckinPlace>,
    onSend: (String, Uri?, CheckinPlace?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPlace by remember { mutableStateOf<CheckinPlace?>(null) }
    var emojiOpen by remember { mutableStateOf(false) }
    var locationOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) photoUri = uri
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        if (other != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(other, 42.dp)
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (m.text.isNotBlank()) Text(m.text, color = JellyInk, fontSize = 13.5f.sp, lineHeight = 19.sp)
                        m.photo?.let {
                            AsyncImage(
                                it,
                                null,
                                Modifier.widthIn(max = 260.dp).heightIn(max = 320.dp).clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        m.audio?.let { InlineAudioPlayer(it) }
                        if (m.locationLat != null && m.locationLng != null) {
                            JellyButton("Shared location", icon = JellyIcons.Map) {
                                val uri = Uri.parse("geo:${m.locationLat},${m.locationLng}?q=${m.locationLat},${m.locationLng}")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            }
                        }
                        Text(shortTime(m.createdAt), color = JellyMuted, fontSize = 8.sp)
                    }
                }
            }
        }

        if (photoUri != null || selectedPlace != null) {
            JellyGlass(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), padding = 8.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(
                            photoUri?.let { "Photo ready" },
                            selectedPlace?.let { "Location: ${it.name.split(",").firstOrNull().orEmpty()}" }
                        ).joinToString(" · "),
                        Modifier.weight(1f),
                        color = JellyMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    JellyButton("Remove") {
                        photoUri = null
                        selectedPlace = null
                    }
                }
            }
        }

        JellyGlass(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), padding = 7.dp) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                JellyIconButton(JellyIcons.Photo, "Photo") { photoPicker.launch("image/*") }
                Box {
                    JellyIconButton(JellyIcons.Feeling, "Emoji") { emojiOpen = !emojiOpen }
                }
                OutlinedTextField(
                    text,
                    { text = it },
                    Modifier.weight(1f),
                    placeholder = { Text("Send a message…") },
                    shape = RoundedCornerShape(17.dp),
                    maxLines = 4
                )
                JellyIconButton(JellyIcons.Map, "Location") { locationOpen = true }
                JellyIconButton(JellyIcons.Send, "Send") {
                    if (text.isNotBlank() || photoUri != null || selectedPlace != null) {
                        onSend(text.trim(), photoUri, selectedPlace)
                        text = ""
                        photoUri = null
                        selectedPlace = null
                        emojiOpen = false
                    }
                }
            }
        }

        if (emojiOpen) {
            val emojis = listOf("😀","😂","😍","🥰","😊","👍","❤️","🙏","🔥","🎉","😢","😮","🤝","👏","✅","🌹","💯","🤲","☕","🌤️","📍","🛍️","🏠","🚗")
            JellyGlass(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp), padding = 8.dp) {
                androidx.compose.foundation.layout.FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    text += if (text.isBlank()) emoji else " $emoji"
                                    emojiOpen = false
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 20.sp) }
                    }
                }
            }
        }
    }

    if (locationOpen) {
        var query by remember { mutableStateOf("") }
        var places by remember { mutableStateOf<List<CheckinPlace>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        var locationError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { locationOpen = false },
            title = { Text("Send Location", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        query,
                        { query = it },
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
                        scope.launch {
                            searching = true
                            locationError = null
                            try {
                                places = onSearchLocation(query.trim())
                                if (places.isEmpty()) locationError = "Place could not be found."
                            } catch (e: Exception) {
                                places = emptyList()
                                locationError = e.message ?: "Place could not be found."
                            } finally { searching = false }
                        }
                    }
                    locationError?.let { Text(it, color = Color(0xFFB23A55), fontSize = 10.sp) }
                    places.forEach { place ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 8.dp,
                            onClick = {
                                selectedPlace = place
                                locationOpen = false
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
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { locationOpen = false } }
        )
    }
}
