package com.mychhachh.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun NotificationsScreen(
    items: List<Notice>,
    loading: Boolean,
    error: String?,
    onMarkRead: () -> Unit,
    onProfile: (Long) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (items.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    JellyButton("Mark all read", icon = JellyIcons.Check, onClick = onMarkRead)
                }
            }
        }
        if (loading && items.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && items.isEmpty() && error == null) item { EmptyCard("No notifications yet.", JellyIcons.Bell) }
        items(items, key = { "notice-${it.id}" }) { n ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    n.actor?.let {
                        Avatar(it, 43.dp, Modifier.clickable { onProfile(it.id) })
                    } ?: JellyIcon(JellyIcons.Bell, size = 36.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        n.actor?.let { UserName(it, 13) }
                        Text(n.text, color = JellyInk, fontSize = 11.5f.sp, lineHeight = 16.sp)
                        Text(shortTime(n.createdAt), color = JellyMuted, fontSize = 9.sp)
                    }
                    if (!n.read) Box(Modifier.size(9.dp).clip(RoundedCornerShape(99.dp)).background(JellyPink))
                }
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(
    items: List<Announcement>,
    loading: Boolean,
    error: String?,
    onLike: (Long) -> Unit,
    onPublish: (String, Uri?, File?) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) photoUri = uri
    }

    fun startRecording() {
        val file = File(context.cacheDir, "announcement-${System.currentTimeMillis()}.m4a")
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

    fun stopRecording() {
        val r = recorder ?: return
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        recording = false
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching { startRecording() }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) stopRecording()
            runCatching { recorder?.release() }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(JellyIcons.Announcement, size = 38.dp)
                        Spacer(Modifier.width(7.dp))
                        Column {
                            Text("Make an Announcement", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Record your voice, add text or a photo.", color = JellyMuted, fontSize = 10.sp)
                        }
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write something with the announcement (optional)…") },
                        minLines = 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        JellyButton(
                            if (recording) "Stop" else if (audioFile != null) "Voice ✓" else "Record Voice",
                            Modifier.weight(1f),
                            primary = recording,
                            icon = JellyIcons.Announcement
                        ) {
                            if (recording) {
                                stopRecording()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) runCatching { startRecording() } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        JellyButton(
                            if (photoUri != null) "Photo ✓" else "Add Photo",
                            Modifier.weight(1f),
                            icon = JellyIcons.Photo
                        ) { photoPicker.launch("image/*") }
                    }
                    if (audioFile != null && !recording) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Voice recording ready", Modifier.weight(1f), color = JellyMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            JellyButton("Remove") {
                                runCatching { audioFile?.delete() }
                                audioFile = null
                            }
                        }
                    }
                    JellyButton(
                        "Publish Announcement",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Send,
                        enabled = !recording && (text.isNotBlank() || photoUri != null || audioFile != null)
                    ) {
                        onPublish(text.trim(), photoUri, audioFile)
                        text = ""
                        photoUri = null
                        audioFile = null
                    }
                }
            }
        }
        if (loading && items.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && items.isEmpty() && error == null) item { EmptyCard("No announcements yet.", JellyIcons.Announcement) }
        items(items, key = { "announcement-${it.id}" }) { a ->
            JellyGlass(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        a.author?.let {
                            Avatar(it, 42.dp)
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                UserName(it, 13)
                                Text(shortTime(a.createdAt), color = JellyMuted, fontSize = 9.sp)
                            }
                        } ?: run {
                            JellyIcon(JellyIcons.Announcement, size = 38.dp)
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text("My Chhachh", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text(shortTime(a.createdAt), color = JellyMuted, fontSize = 9.sp)
                            }
                        }
                    }
                    if (a.text.isNotBlank()) Text(a.text, color = JellyInk, fontSize = 13.5f.sp, lineHeight = 19.sp)
                    a.photo?.let {
                        AsyncImage(it, null, Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(17.dp)))
                    }
                    a.audio?.let { InlineAudioPlayer(it) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JellyButton(
                            "${if (a.liked) "Liked" else "Like"} ${a.likes}",
                            primary = a.liked,
                            icon = JellyIcons.Heart
                        ) { onLike(a.id) }
                        if (a.comments > 0) Text("${a.comments} comments", color = JellyMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Composable
fun VotesScreen(items: List<Vote>, loading: Boolean, error: String?, onCast: (Long, Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading && items.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && items.isEmpty() && error == null) item { EmptyCard("No voting available.", JellyIcons.Vote) }
        items(items, key = { "vote-${it.id}" }) { v ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(v.title.ifBlank { "Voting" }, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    val votingOpen = v.status.equals("active", true) || v.status.equals("open", true) || v.status.equals("running", true)
                    Text(if (votingOpen) "OPEN" else "ENDED", color = if (votingOpen) JellyGreen else JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    val total = (v.votes1 + v.votes2).coerceAtLeast(1)
                    val leftShare = v.votes1.toFloat() / total.toFloat()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        VoteSide(v.user1, v.votes1, Modifier.weight(1f), votingOpen) { onCast(v.id, 1) }
                        Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            JellyIcon(JellyIcons.Vote, size = 38.dp)
                            Text("VS", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                        VoteSide(v.user2, v.votes2, Modifier.weight(1f), votingOpen) { onCast(v.id, 2) }
                    }
                    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp))) {
                        Box(Modifier.weight(leftShare.coerceAtLeast(.001f)).fillMaxHeight().background(JellyPink))
                        Box(Modifier.weight((1f - leftShare).coerceAtLeast(.001f)).fillMaxHeight().background(JellyPurple))
                    }
                    Text("${v.votes1 + v.votes2} total votes", color = JellyMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun VoteSide(user: User?, votes: Int, modifier: Modifier, enabled: Boolean, onVote: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (user != null) {
            Avatar(user, 58.dp)
            UserName(user, 10)
        } else JellyIcon(JellyIcons.User, size = 54.dp)
        Text("$votes votes", color = JellyMuted, fontSize = 9.5f.sp)
        JellyButton(if (enabled) "Vote" else "Ended", primary = enabled, enabled = enabled, onClick = onVote)
    }
}

@Composable
fun SavedScreen(
    posts: List<Post>, loading: Boolean, error: String?, onProfile: (Long) -> Unit,
    onLike: (Post) -> Unit, onComment: (Post) -> Unit, onShare: (Post) -> Unit, onSave: (Post) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (loading && posts.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && posts.isEmpty() && error == null) item { EmptyCard("No saved posts yet.", JellyIcons.Save) }
        items(posts, key = { "saved-${it.id}" }) { p ->
            PostCard(p, true, {}, { onProfile(p.user.id) }, onLike, onComment, onShare, onSave)
        }
    }
}

@Composable
fun SearchScreen(
    result: SearchBundle,
    loading: Boolean,
    error: String?,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onProfile: (Long) -> Unit,
    onShop: (Long) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.weight(1f),
                        placeholder = { Text("People, shops or posts") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    JellyButton("Search", icon = JellyIcons.Search, onClick = onSearch)
                }
            }
        }
        if (loading) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onSearch) } }
        if (result.users.isNotEmpty()) item { SectionTitle("People") }
        items(result.users, key = { "su-${it.id}" }) { u ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp, onClick = { onProfile(u.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u, 42.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        UserName(u, 13)
                        Text("@${u.username}", color = JellyMuted, fontSize = 9.5f.sp)
                    }
                }
            }
        }
        if (result.shops.isNotEmpty()) item { SectionTitle("Shops") }
        items(result.shops, key = { "ss-${it.id}" }) { s ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp, onClick = { onShop(s.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JellyIcon(JellyIcons.Shop, size = 38.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("@${s.username}", color = JellyMuted, fontSize = 9.5f.sp)
                    }
                }
            }
        }
        if (result.posts.isNotEmpty()) item { SectionTitle("Posts") }
        items(result.posts, key = { "sp-${it.id}" }) { p ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.clickable { onProfile(p.user.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Avatar(p.user, 34.dp)
                        Spacer(Modifier.width(7.dp))
                        UserName(p.user, 12)
                    }
                    if (p.text.isNotBlank()) Text(p.text.take(260), color = JellyInk, fontSize = 11.5f.sp, lineHeight = 16.sp)
                    p.photo?.let { AsyncImage(it, null, Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(15.dp))) }
                }
            }
        }
        if (!loading && query.isNotBlank() && result.users.isEmpty() && result.shops.isEmpty() && result.posts.isEmpty() && error == null) {
            item { EmptyCard("No results found.", JellyIcons.Search) }
        }
    }
}

@Composable
fun MapScreen(query: String, onQuery: (String) -> Unit, result: JSONObject?, loading: Boolean, error: String?, onSearch: () -> Unit) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val lat = result?.optDouble("lat", Double.NaN) ?: Double.NaN
    val lng = result?.let { it.optDouble("lng", it.optDouble("lon", Double.NaN)) } ?: Double.NaN
    val label = result?.let { it.optString("display_name", it.optString("name", query)) }.orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Map", "Explore Chhachh and search places", JellyIcons.Map) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp) {
                Column {
                    AndroidView(
                        factory = { mapView },
                        update = { map ->
                            if (!lat.isNaN() && !lng.isNaN()) {
                                val point = GeoPoint(lat, lng)
                                map.overlays.removeAll { it is Marker }
                                map.overlays.add(Marker(map).apply {
                                    position = point
                                    title = label.ifBlank { query }
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                })
                                map.controller.setZoom(16.0)
                                map.controller.animateTo(point)
                                map.invalidate()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(310.dp).clip(RoundedCornerShape(22.dp))
                    )
                    Text("© OpenStreetMap contributors", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = JellyMuted, fontSize = 8.sp)
                }
            }
        }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Village, mohalla, shop or place") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp)
                    )
                    JellyButton("Find place", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Search, enabled = !loading, onClick = onSearch)
                }
            }
        }
        if (loading) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onSearch) } }
        if (!lat.isNaN() && !lng.isNaN()) {
            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(label.ifBlank { query }, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("${"%.5f".format(lat)}, ${"%.5f".format(lng)}", color = JellyMuted, fontSize = 9.5f.sp)
                        JellyButton("Open directions", primary = true, icon = JellyIcons.Map) {
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label.ifBlank { query })})")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(33.88333, 72.36667))
        }
    }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }
    return mapView
}

@Composable
fun WeatherScreen(data: JSONObject?, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Live conditions for Chhachh", Modifier.weight(1f), color = JellyMuted, fontSize = 11.sp)
                JellyButton("Refresh", onClick = onRefresh)
            }
        }
        if (loading) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onRefresh) } }
        data?.optJSONObject("current")?.let { c ->
            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${c.optDouble("temperature_2m", 0.0).toInt()}°", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 45.sp)
                                Text(weatherLabel(c.optInt("weather_code", -1)), color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Feels like ${c.optDouble("apparent_temperature", 0.0).toInt()}°", color = JellyMuted, fontSize = 11.sp)
                            }
                            JellyIcon(JellyIcons.Pin, size = 48.dp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeatherMetric("Humidity", "${c.optInt("relative_humidity_2m", 0)}%", Modifier.weight(1f))
                            WeatherMetric("Wind", "${c.optDouble("wind_speed_10m", 0.0)} km/h", Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeatherMetric("Clouds", "${c.optInt("cloud_cover", 0)}%", Modifier.weight(1f))
                            WeatherMetric("Rain", "${c.optDouble("precipitation", 0.0)} mm", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun weatherLabel(code: Int): String = when (code) {
    0 -> "Clear"
    1, 2 -> "Mostly clear"
    3 -> "Cloudy"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
    71, 73, 75, 77, 85, 86 -> "Snow"
    95, 96, 99 -> "Thunderstorm"
    else -> "Current weather"
}

@Composable
private fun WeatherMetric(label: String, value: String, modifier: Modifier) {
    JellyGlass(modifier, radius = 17.dp, padding = 10.dp) {
        Column {
            Text(label, color = JellyMuted, fontSize = 9.5f.sp)
            Text(value, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}
