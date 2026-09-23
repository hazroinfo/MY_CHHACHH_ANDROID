package com.mychhachh.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import java.io.File

@Composable
fun MessagesScreen(
    conversations: List<Conversation>,
    groups: List<com.mychhachh.app.data.MessageGroup>,
    shopOwnerIds: Set<Long>,
    loading: Boolean,
    error: String?,
    onOpen: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onCreateGroup: (String, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var newGroupOpen by remember { mutableStateOf(false) }
    val q = search.trim().lowercase()
    val directRows = conversations.filter { row ->
        val matches = q.isBlank() || row.user.name.lowercase().contains(q) || row.user.username.lowercase().contains(q) || row.preview.lowercase().contains(q)
        matches && when (filter) {
            "unread" -> row.unread
            "shops" -> shopOwnerIds.contains(row.user.id)
            "groups" -> false
            else -> true
        }
    }
    val groupRows = groups.filter { row ->
        val matches = q.isBlank() || row.name.lowercase().contains(q) || row.preview.lowercase().contains(q)
        matches && (filter == "all" || filter == "groups")
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { PageTitle("Messages", "Good conversations build a brighter Chhachh", JellyIcons.Message) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 9.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JellyGlass(
                        Modifier.weight(1f).height(48.dp),
                        radius = 20.dp,
                        padding = 0.dp,
                        surfaceColor = LiveJellyTheme.inputColor,
                        surfaceOpacity = LiveJellyTheme.inputOpacity
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            JellyIcon(JellyIcons.Search, size = 26.dp)
                            Spacer(Modifier.width(6.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = search,
                                onValueChange = { search = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = JellyInk,
                                    fontSize = 10.5f.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (search.isBlank()) Text("Search conversations…", color = JellyMuted, fontSize = 10.sp, maxLines = 1)
                                        inner()
                                    }
                                }
                            )
                            if (search.isNotBlank()) {
                                JellyIconButton(JellyIcons.Close, "Clear", Modifier.size(32.dp)) { search = "" }
                            }
                        }
                    }
                    JellyButton("New Group", Modifier.width(84.dp), icon = JellyIcons.People) { newGroupOpen = true }
                }
            }
        }
        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    JellyPill("All", filter == "all", Modifier.weight(1f)) { filter = "all" }
                    JellyPill("Unread", filter == "unread", Modifier.weight(1f)) { filter = "unread" }
                    JellyPill("Shops", filter == "shops", Modifier.weight(1f)) { filter = "shops" }
                    JellyPill("Groups", filter == "groups", Modifier.weight(1f)) { filter = "groups" }
                }
            }
        }
        if (loading && conversations.isEmpty() && groups.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }

        if (filter != "groups") {
            items(directRows, key = { "conversation-${it.user.id}" }) { row ->
                JellyGlass(Modifier.fillMaxWidth(), radius = 19.dp, padding = 12.dp, onClick = { onOpen(row.user.id) }) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(row.user, 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                UserName(row.user, 14)
                                Text(row.preview.ifBlank { "Open conversation" }, color = JellyMuted, fontSize = 10.5f.sp, maxLines = 1)
                            }
                            Text(shortTime(row.createdAt), color = JellyMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.Top))
                        }
                        if (row.unread) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(JellyPurple)
                            )
                        }
                    }
                }
            }
        }

        if (filter == "all" || filter == "groups") {
            items(groupRows, key = { "group-${it.id}" }) { group ->
                JellyGlass(Modifier.fillMaxWidth(), radius = 19.dp, padding = 12.dp, onClick = { onOpenGroup(group.id) }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(99.dp)).background(Color(0xFFE9F6FF)),
                            contentAlignment = Alignment.Center
                        ) { JellyIcon(JellyIcons.People, size = 30.dp) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                            Text(group.preview.ifBlank { "Group conversation" }, color = JellyMuted, fontSize = 10.5f.sp, maxLines = 1)
                        }
                        Text(shortTime(group.createdAt), color = JellyMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.Top))
                    }
                }
            }
        }

        if (!loading && directRows.isEmpty() && groupRows.isEmpty() && error == null) {
            item { EmptyCard("No conversations in this filter.", JellyIcons.Message) }
        }
    }

    if (newGroupOpen) {
        var name by remember { mutableStateOf("") }
        var usernames by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newGroupOpen = false },
            title = { Text("Create Group", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Group name") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        usernames,
                        { usernames = it },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("ali123, usman4, sara") },
                        minLines = 2,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Text("Add members by username, separated with commas.", color = JellyMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                JellyButton("Create Group", primary = true, icon = JellyIcons.People, enabled = name.isNotBlank()) {
                    onCreateGroup(name.trim(), usernames.trim())
                    newGroupOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { newGroupOpen = false } }
        )
    }
}

@Composable
private fun MessageToolButton(icon: Int, description: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        JellyIcon(icon, size = (size.value - 10f).dp, contentDescription = description)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    me: User,
    other: User?,
    messages: List<Message>,
    loading: Boolean,
    error: String?,
    onSearchLocation: suspend (String) -> List<CheckinPlace>,
    onSend: (String, Uri?, File?, CheckinPlace?) -> Unit,
    headerTitle: String? = null,
    headerSubtitle: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<CheckinPlace?>(null) }
    var emojiOpen by remember { mutableStateOf(false) }
    var locationOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) photoUri = uri
    }

    fun attachCurrentLocation() {
        val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.asSequence()
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider ->
                runCatching {
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) manager.getLastKnownLocation(provider) else null
                }.getOrNull()
            }
            .maxByOrNull { it.time }
        if (location != null) {
            selectedPlace = CheckinPlace("My Location", location.latitude, location.longitude)
            locationOpen = false
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) attachCurrentLocation()
    }

    fun startVoiceRecording() {
        val file = File(context.cacheDir, "message-${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128000)
        r.setAudioSamplingRate(44100)
        r.setMaxDuration(300000)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        audioFile = file
        recording = true
    }

    fun stopVoiceRecording() {
        val r = recorder ?: return
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        recording = false
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching { startVoiceRecording() }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) stopVoiceRecording()
            runCatching { recorder?.release() }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        if (headerTitle != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(99.dp)).background(Color(0xFFE9F6FF)),
                    contentAlignment = Alignment.Center
                ) { JellyIcon(JellyIcons.People, size = 30.dp) }
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(headerTitle, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    if (!headerSubtitle.isNullOrBlank()) Text(headerSubtitle, color = JellyMuted, fontSize = 9.sp)
                }
            }
        } else if (other != null) {
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
            if (!loading && messages.isEmpty() && error == null) item { EmptyCard("No messages.", JellyIcons.Message) }
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

        if (photoUri != null || audioFile != null || selectedPlace != null) {
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

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JellyMuted.copy(alpha = .12f))
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MessageToolButton(JellyIcons.Photo, "Photo", 38.dp) { photoPicker.launch("image/*") }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { emojiOpen = !emojiOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Text("☺", color = JellyInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                JellyGlass(
                    Modifier.weight(1f).heightIn(min = 38.dp, max = 120.dp),
                    radius = 17.dp,
                    padding = 0.dp,
                    surfaceColor = LiveJellyTheme.inputColor,
                    surfaceOpacity = LiveJellyTheme.inputOpacity
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = JellyInk,
                            fontSize = 12.sp
                        ),
                        maxLines = 4,
                        decorationBox = { inner ->
                            Box {
                                if (text.isBlank()) Text("Send a message…", color = JellyMuted, fontSize = 11.sp)
                                inner()
                            }
                        }
                    )
                }
                MessageToolButton(JellyIcons.Map, "Location", 38.dp) { locationOpen = true }
                MessageToolButton(JellyIcons.Send, "Send", 42.dp) {
                    if (text.isNotBlank() || photoUri != null || selectedPlace != null) {
                        onSend(text.trim(), photoUri, null, selectedPlace)
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
                    Text("Tap a search result to choose a place, or use your current location.", color = JellyMuted, fontSize = 9.5f.sp)
                    JellyButton("My Location", Modifier.fillMaxWidth(), icon = JellyIcons.Pin) {
                        val granted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) attachCurrentLocation()
                        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                    OutlinedTextField(
                        query,
                        { query = it },
                        placeholder = { Text("Search a place in Chhachh / Hazro") },
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
