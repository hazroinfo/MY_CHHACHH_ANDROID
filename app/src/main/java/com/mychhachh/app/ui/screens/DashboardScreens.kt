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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@Composable
fun NotificationsScreen(
    items: List<Notice>,
    loading: Boolean,
    error: String?,
    onMarkRead: () -> Unit,
    onProfile: (Long) -> Unit
) {
    var filter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) { onMarkRead() }

    fun category(n: Notice): String {
        val t = n.type.lowercase()
        val txt = n.text.lowercase()
        return when {
            t.contains("shop") || txt.contains("shop") -> "shops"
            t.contains("follow") || txt.contains("follow") -> "follow"
            t.contains("mention") || t.contains("comment") || txt.contains("mention") -> "mentions"
            else -> "all"
        }
    }

    val visible = items.filter { filter == "all" || category(it) == filter }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { PageTitle("Notifications", "Stay updated with your Chhachh community", JellyIcons.Bell) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 999.dp, padding = 4.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JellyPill("All", filter == "all", Modifier.weight(1f)) { filter = "all" }
                    JellyPill("Mentions", filter == "mentions", Modifier.weight(1f)) { filter = "mentions" }
                    JellyPill("Shops", filter == "shops", Modifier.weight(1f)) { filter = "shops" }
                    JellyPill("Follow", filter == "follow", Modifier.weight(1f)) { filter = "follow" }
                }
            }
        }
        if (loading && items.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && visible.isEmpty() && error == null) item { EmptyCard("No notifications in this filter.", JellyIcons.Bell) }
        items(visible, key = { "notice-${it.id}" }) { n ->
            JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 0.dp) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!n.read) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(LiveJellyTheme.activeColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    n.actor?.let {
                        Avatar(it, LiveJellyTheme.notificationAvatarSize.dp, Modifier.clickable { onProfile(it.id) })
                    } ?: Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        JellyIcon(JellyIcons.Bell, size = 34.dp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            n.actor?.let { UserName(it, 13) }
                                ?: Text(n.type.ifBlank { "Notification" }, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text(shortTime(n.createdAt), color = JellyMuted, fontSize = 8.5f.sp)
                        }
                        if (n.text.isNotBlank()) Text(n.text, color = JellyInk, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.width(5.dp))
                    JellyIcon(JellyIcons.Arrow, size = 20.dp)
                }
            }
        }
        item {
            Text(
                "People · Knowledge · Communities · A Brighter Tomorrow",
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = JellyMuted,
                fontSize = 8.5f.sp
            )
        }
    }
}

@Composable
fun AnnouncementsScreen(
    items: List<Announcement>,
    loading: Boolean,
    error: String?,
    meId: Long,
    isAdmin: Boolean,
    onLike: (Long) -> Unit,
    onLoadComments: suspend (Long) -> List<Comment>,
    onAddComment: suspend (Long, String) -> Unit,
    onEdit: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onReport: (Long, String) -> Unit,
    onPublish: (String, Uri?, File?) -> Unit
) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var commentAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var actionAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var editAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var deleteAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var reportAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var announcementComments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentsError by remember { mutableStateOf<String?>(null) }
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
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Announcements", "Voice notices and important updates from people across Chhachh", JellyIcons.Announcement) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(JellyIcons.Announcement, size = 38.dp)
                        Spacer(Modifier.width(7.dp))
                        Column {
                            Text("Make an Announcement", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Record your voice. You can also add a short message or photo.", color = JellyMuted, fontSize = 10.sp)
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
                        JellyIconButton(JellyIcons.More, "Announcement options") { actionAnnouncement = a }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        JellyGlass(radius = 999.dp, padding = 6.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                JellyIcon(
                                    if (a.type.lowercase() in listOf("emergency", "info", "ad")) JellyIcons.Shield else JellyIcons.Announcement,
                                    size = 18.dp
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    when (a.type.lowercase()) {
                                        "emergency" -> "Emergency News"
                                        "info" -> "Information"
                                        "ad" -> "Admin Notice"
                                        else -> "Announcement"
                                    },
                                    color = JellyInk,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5f.sp
                                )
                            }
                        }
                    }
                    if (a.text.isNotBlank()) Text(a.text, color = JellyInk, fontSize = 13.5f.sp, lineHeight = 19.sp)
                    a.photo?.let {
                        AsyncImage(it, null, Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(17.dp)))
                    }
                    a.audio?.let { InlineAudioPlayer(it) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JellyButton(
                            "${if (a.liked) "Liked" else "Like"} ${a.likes}",
                            Modifier.weight(1f).height(44.dp),
                            primary = a.liked,
                            icon = JellyIcons.Heart
                        ) { onLike(a.id) }
                        JellyButton("Comments ${a.comments}", Modifier.weight(1f).height(44.dp), icon = JellyIcons.Comment) {
                            commentAnnouncement = a
                            commentsLoading = true
                            commentsError = null
                            uiScope.launch {
                                try {
                                    announcementComments = onLoadComments(a.id)
                                } catch (e: Exception) {
                                    commentsError = e.message ?: "Comments could not be loaded."
                                } finally {
                                    commentsLoading = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    actionAnnouncement?.let { announcement ->
        val canManage = isAdmin || announcement.author?.id == meId
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { actionAnnouncement = null },
            title = { Text("Announcement options", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canManage) {
                        JellyButton("Edit announcement", Modifier.fillMaxWidth(), icon = JellyIcons.Edit) {
                            editAnnouncement = announcement
                            actionAnnouncement = null
                        }
                        JellyButton("Delete announcement", Modifier.fillMaxWidth(), danger = true, icon = JellyIcons.Delete) {
                            deleteAnnouncement = announcement
                            actionAnnouncement = null
                        }
                    } else {
                        JellyButton("Report announcement", Modifier.fillMaxWidth(), danger = true, icon = JellyIcons.Shield) {
                            reportAnnouncement = announcement
                            actionAnnouncement = null
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { actionAnnouncement = null } }
        )
    }

    editAnnouncement?.let { announcement ->
        var editText by remember(announcement.id) { mutableStateOf(announcement.text) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editAnnouncement = null },
            title = { Text("Edit Announcement", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    editText,
                    { editText = it.take(5000) },
                    Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Save", primary = true, icon = JellyIcons.Check, enabled = editText.isNotBlank() || announcement.photo != null || announcement.audio != null) {
                    onEdit(announcement.id, editText.trim())
                    editAnnouncement = null
                }
            },
            dismissButton = { JellyButton("Cancel") { editAnnouncement = null } }
        )
    }

    deleteAnnouncement?.let { announcement ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteAnnouncement = null },
            title = { Text("Delete Announcement", color = JellyInk, fontWeight = FontWeight.Black) },
            text = { Text("Delete this announcement permanently?", color = JellyMuted) },
            confirmButton = {
                JellyButton("Delete", danger = true, icon = JellyIcons.Delete) {
                    onDelete(announcement.id)
                    deleteAnnouncement = null
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteAnnouncement = null } }
        )
    }

    reportAnnouncement?.let { announcement ->
        var reason by remember(announcement.id) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { reportAnnouncement = null },
            title = { Text("Report Announcement", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    reason,
                    { reason = it.take(3000) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Tell the admin what is wrong…") },
                    minLines = 3,
                    maxLines = 7,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Send report", danger = true, icon = JellyIcons.Shield, enabled = reason.trim().length >= 3) {
                    onReport(announcement.id, reason.trim())
                    reportAnnouncement = null
                }
            },
            dismissButton = { JellyButton("Cancel") { reportAnnouncement = null } }
        )
    }

    commentAnnouncement?.let { announcement ->
        var draft by remember(announcement.id) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { commentAnnouncement = null },
            title = { Text("Announcement Comments", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (commentsLoading) LoadingBlock()
                    commentsError?.let { ErrorCard(it) }
                    if (!commentsLoading && announcementComments.isEmpty()) {
                        Text("No comments yet.", color = JellyMuted, fontSize = 10.5f.sp)
                    }
                    if (announcementComments.isNotEmpty()) {
                        LazyColumn(
                            Modifier.weight(1f, fill = false).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(announcementComments, key = { "announcement-comment-${it.id}" }) { cm ->
                                JellyGlass(Modifier.fillMaxWidth(), radius = 15.dp, padding = 8.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Avatar(cm.user, 32.dp)
                                            Spacer(Modifier.width(6.dp))
                                            UserName(cm.user, 10)
                                        }
                                        Text(cm.text, color = JellyInk, fontSize = 10.5f.sp, lineHeight = 15.sp)
                                        Text(shortTime(cm.createdAt), color = JellyMuted, fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        draft,
                        { draft = it.take(3000) },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Write a comment… Type @ to mention") },
                        minLines = 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(17.dp)
                    )
                }
            },
            confirmButton = {
                JellyButton("Send", primary = true, icon = JellyIcons.Send, enabled = draft.isNotBlank() && !commentsLoading) {
                    commentsLoading = true
                    commentsError = null
                    uiScope.launch {
                        try {
                            onAddComment(announcement.id, draft.trim())
                            draft = ""
                            announcementComments = onLoadComments(announcement.id)
                        } catch (e: Exception) {
                            commentsError = e.message ?: "Comment could not be sent."
                        } finally {
                            commentsLoading = false
                        }
                    }
                }
            },
            dismissButton = { JellyButton("Close") { commentAnnouncement = null } }
        )
    }
}
@Composable
fun VotesScreen(
    items: List<Vote>,
    loading: Boolean,
    error: String?,
    meId: Long,
    initialVoteId: Long = 0L,
    onSearchOpponent: suspend (String) -> List<User>,
    onCreateChallenge: (String, Int, String) -> Unit,
    onCast: (Long, Long) -> Unit,
    onRespond: (Long, String) -> Unit,
    onStart: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onLeave: (Long) -> Unit,
    onShare: (Long) -> Unit,
    onStatement: (Long, String) -> Unit,
    onLoadComments: suspend (Long) -> List<Comment>,
    onAddComment: suspend (Long, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var opponentQuery by remember { mutableStateOf("") }
    var opponentResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedOpponent by remember { mutableStateOf<User?>(null) }
    var searchingOpponent by remember { mutableStateOf(false) }
    var challengeLine by remember { mutableStateOf("") }
    var durationHours by remember { mutableIntStateOf(24) }
    val durationOptions = listOf(1, 6, 12, 24, 48, 72, 168)

    var detailVoteId by remember(initialVoteId) { mutableLongStateOf(initialVoteId) }
    var commentsVoteId by remember { mutableLongStateOf(0L) }
    var voteComments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentsError by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf("") }

    fun openComments(id: Long) {
        commentsVoteId = id
        voteComments = emptyList()
        commentsError = null
        commentsLoading = true
        scope.launch {
            try {
                voteComments = onLoadComments(id)
            } catch (e: Exception) {
                commentsError = e.message ?: "Could not load comments."
            } finally {
                commentsLoading = false
            }
        }
    }

    val waiting = items.filter { it.status == "waiting" || it.status == "ready" }
    val active = items.filter { it.status == "active" }
    val ended = items.filter { it.status != "waiting" && it.status != "ready" && it.status != "active" }
    val detail = items.firstOrNull { it.id == detailVoteId }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (detailVoteId > 0L) {
            item {
                PageTitle("Voting Match", "Invite → Accept → Start → Vote → Winner", JellyIcons.Vote)
            }
            item {
                JellyButton("Back to Voting Arena", icon = JellyIcons.Arrow) { detailVoteId = 0L }
            }
            if (loading && detail == null) item { LoadingBlock() }
            error?.let { item { ErrorCard(it) } }
            if (!loading && detail == null && error == null) item { EmptyCard("Voting match not found.", JellyIcons.Vote) }
            detail?.let { vote ->
                if ((vote.status == "ended" || vote.status == "forfeit") && vote.winnerUserId > 0L) {
                    item(key = "winner-${vote.id}") { V95VoteWinnerCard(vote) }
                }
                item(key = "detail-vote-${vote.id}") {
                    V95VoteCard(
                        vote = vote,
                        meId = meId,
                        detail = true,
                        onCast = onCast,
                        onRespond = onRespond,
                        onStart = onStart,
                        onCancel = onCancel,
                        onLeave = onLeave,
                        onOpen = { },
                        onComments = { openComments(vote.id) },
                        onShare = { onShare(vote.id) },
                        onStatement = onStatement
                    )
                }
            }
        } else {
            item { PageTitle("Voting Arena", "A challenge works like a match lobby: invite a player, wait for acceptance, then the host starts the match.", JellyIcons.Vote) }

            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("How a match starts", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text("A real challenge lobby: invite, wait, accept, start, vote, celebrate.", color = JellyMuted, fontSize = 9.5f.sp)
                            }
                            Text("6 steps", color = JellyMuted, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            VoteHowStep(JellyIcons.People, "1", "Choose player", Modifier.weight(1f))
                            VoteHowStep(JellyIcons.Send, "2", "Send invite", Modifier.weight(1f))
                            VoteHowStep(JellyIcons.Clock, "3", "Waiting room", Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            VoteHowStep(JellyIcons.Check, "4", "Opponent accepts", Modifier.weight(1f))
                            VoteHowStep(JellyIcons.Vote, "5", "Start & vote", Modifier.weight(1f))
                            VoteHowStep(JellyIcons.Crown, "6", "Winner celebration", Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("New Challenge", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Search a user you follow and who follows you back.", color = JellyMuted, fontSize = 10.sp)
                            }
                            Text("Lobby", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        Text("Choose opponent", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        OutlinedTextField(
                            opponentQuery,
                            {
                                opponentQuery = it
                                selectedOpponent = null
                            },
                            Modifier.fillMaxWidth(),
                            placeholder = { Text("Type a name or @username") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp)
                        )
                        JellyButton(
                            if (searchingOpponent) "Searching…" else "Search opponent",
                            Modifier.fillMaxWidth(),
                            primary = true,
                            icon = JellyIcons.Search,
                            enabled = !searchingOpponent && opponentQuery.trim().removePrefix("@").length >= 2
                        ) {
                            scope.launch {
                                searchingOpponent = true
                                try {
                                    opponentResults = onSearchOpponent(opponentQuery.removePrefix("@").trim())
                                        .filter { it.id != meId }
                                        .take(8)
                                } finally {
                                    searchingOpponent = false
                                }
                            }
                        }
                        opponentResults.forEach { person ->
                            JellyGlass(
                                Modifier.fillMaxWidth(),
                                radius = 16.dp,
                                padding = 8.dp,
                                onClick = {
                                    selectedOpponent = person
                                    opponentQuery = person.username
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(person, 40.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Column(Modifier.weight(1f)) {
                                        UserName(person, 11)
                                        Text("@${person.username}", color = JellyMuted, fontSize = 9.sp)
                                    }
                                    Text(if (selectedOpponent?.id == person.id) "Selected" else "Choose", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Voting duration", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                JellyButton(
                                    when {
                                        durationHours < 24 -> "$durationHours hour${if (durationHours == 1) "" else "s"}"
                                        durationHours == 24 -> "24 hours"
                                        else -> "${durationHours / 24} days"
                                    },
                                    Modifier.fillMaxWidth(),
                                    icon = JellyIcons.Clock
                                ) {
                                    val i = durationOptions.indexOf(durationHours).coerceAtLeast(0)
                                    durationHours = durationOptions[(i + 1) % durationOptions.size]
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("My match line", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                OutlinedTextField(
                                    challengeLine,
                                    { challengeLine = it.take(500) },
                                    Modifier.fillMaxWidth(),
                                    placeholder = { Text("Optional short line") },
                                    maxLines = 2,
                                    shape = RoundedCornerShape(18.dp)
                                )
                            }
                        }
                        JellyButton(
                            "Send Challenge Invite",
                            Modifier.fillMaxWidth(),
                            primary = true,
                            icon = JellyIcons.Send,
                            enabled = selectedOpponent != null
                        ) {
                            val opponent = selectedOpponent ?: return@JellyButton
                            onCreateChallenge(opponent.username, durationHours, challengeLine.trim())
                            opponentQuery = ""
                            selectedOpponent = null
                            opponentResults = emptyList()
                            challengeLine = ""
                            durationHours = 24
                        }
                    }
                }
            }

            if (loading && items.isEmpty()) item { LoadingBlock() }
            error?.let { item { ErrorCard(it) } }

            item { VoteSectionTitle("Waiting Room", waiting.size) }
            if (!loading && waiting.isEmpty()) item { EmptyCard("No pending challenges. Send an invite above to start one.", JellyIcons.Clock) }
            items(waiting, key = { "waiting-vote-${it.id}" }) { vote ->
                V95VoteCard(
                    vote, meId, false, onCast, onRespond, onStart, onCancel, onLeave,
                    onOpen = { detailVoteId = vote.id },
                    onComments = { openComments(vote.id) },
                    onShare = { onShare(vote.id) },
                    onStatement = onStatement
                )
            }

            item { VoteSectionTitle("Live Matches", active.size) }
            if (!loading && active.isEmpty()) item { EmptyCard("No live voting right now.", JellyIcons.Vote) }
            items(active, key = { "active-vote-${it.id}" }) { vote ->
                V95VoteCard(
                    vote, meId, false, onCast, onRespond, onStart, onCancel, onLeave,
                    onOpen = { detailVoteId = vote.id },
                    onComments = { openComments(vote.id) },
                    onShare = { onShare(vote.id) },
                    onStatement = onStatement
                )
            }

            item { VoteSectionTitle("Results", ended.size) }
            if (!loading && ended.isEmpty()) item { EmptyCard("No completed matches yet.", JellyIcons.Crown) }
            items(ended, key = { "ended-vote-${it.id}" }) { vote ->
                if (vote.winnerUserId > 0L) {
                    V95VoteWinnerCard(vote, onOpen = { detailVoteId = vote.id })
                    Spacer(Modifier.height(8.dp))
                }
                V95VoteCard(
                    vote, meId, false, onCast, onRespond, onStart, onCancel, onLeave,
                    onOpen = { detailVoteId = vote.id },
                    onComments = { openComments(vote.id) },
                    onShare = { onShare(vote.id) },
                    onStatement = onStatement
                )
            }
        }
    }

    if (commentsVoteId > 0L) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { commentsVoteId = 0L },
            title = { Text("Voting Comments", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(Modifier.heightIn(max = 470.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        commentText,
                        { commentText = it.take(2000) },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Write a comment… Type @ to mention") },
                        shape = RoundedCornerShape(17.dp),
                        maxLines = 3
                    )
                    JellyButton("Comment", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Comment, enabled = commentText.isNotBlank()) {
                        val id = commentsVoteId
                        val text = commentText.trim()
                        scope.launch {
                            try {
                                onAddComment(id, text)
                                commentText = ""
                                voteComments = onLoadComments(id)
                            } catch (e: Exception) {
                                commentsError = e.message ?: "Comment could not be added."
                            }
                        }
                    }
                    if (commentsLoading) LoadingBlock()
                    commentsError?.let { ErrorCard(it) }
                    if (!commentsLoading && voteComments.isEmpty() && commentsError == null) {
                        Text("No comments yet.", color = JellyMuted, fontSize = 10.sp)
                    }
                    voteComments.take(30).forEach { comment ->
                        JellyGlass(Modifier.fillMaxWidth(), radius = 15.dp, padding = 8.dp) {
                            Row(verticalAlignment = Alignment.Top) {
                                Avatar(comment.user, 34.dp)
                                Spacer(Modifier.width(7.dp))
                                Column {
                                    UserName(comment.user, 10)
                                    Text(shortTime(comment.createdAt), color = JellyMuted, fontSize = 8.sp)
                                    Text(comment.text, color = JellyInk, fontSize = 10.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { commentsVoteId = 0L } }
        )
    }
}

@Composable
private fun VoteHowStep(icon: Int, number: String, label: String, modifier: Modifier = Modifier) {
    JellyGlass(modifier.heightIn(min = 86.dp), radius = 17.dp, padding = 7.dp) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            JellyIcon(icon, size = 28.dp)
            Text(number, color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(label, color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 8.sp, maxLines = 2)
        }
    }
}

@Composable
private fun VoteSectionTitle(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
        JellyGlass(radius = 999.dp, padding = 7.dp) {
            Text(count.toString(), color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
        }
    }
}

private fun voteStatusLabelNative(v: Vote): String = when (v.status) {
    "waiting" -> "Waiting for opponent"
    "ready" -> "Ready to start"
    "active" -> "Live voting"
    "ended" -> "Completed"
    "declined" -> "Challenge declined"
    "cancelled" -> "Cancelled"
    "forfeit" -> "Ended by withdrawal"
    else -> v.status.ifBlank { "Voting" }
}

private fun voteEndMillis(raw: String): Long {
    val value = raw.trim()
    if (value.isBlank()) return 0L
    return runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrElse {
        runCatching { java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrElse {
            runCatching {
                java.time.LocalDateTime.parse(value.replace(' ', 'T').take(19))
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrDefault(0L)
        }
    }
}

private fun voteCountdownText(end: String, now: Long): String {
    val target = voteEndMillis(end)
    if (target <= 0L) return "LIVE"
    var secs = ((target - now) / 1000L).coerceAtLeast(0L)
    val days = secs / 86400L
    secs %= 86400L
    val hours = secs / 3600L
    secs %= 3600L
    val mins = secs / 60L
    val sec = secs % 60L
    return "%02dD %02dH %02dM %02dS".format(days, hours, mins, sec)
}

@Composable
private fun VoteStepStrip(v: Vote) {
    val status = v.status
    val acceptDone = status in listOf("ready", "active", "ended", "forfeit")
    val startDone = status in listOf("active", "ended", "forfeit")
    val finishDone = status in listOf("ended", "forfeit", "declined", "cancelled")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        VoteStep("1", "Invite", true, status == "waiting", Modifier.weight(1f))
        VoteStep("2", "Accept", acceptDone, status == "waiting", Modifier.weight(1f))
        VoteStep("3", "Start", startDone, status == "ready", Modifier.weight(1f))
        VoteStep("4", "Result", finishDone, status == "active" || finishDone, Modifier.weight(1f))
    }
}

@Composable
private fun VoteStep(number: String, label: String, done: Boolean, current: Boolean, modifier: Modifier = Modifier) {
    JellyGlass(modifier, radius = 14.dp, padding = 6.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (done) JellyIcon(JellyIcons.Check, size = 19.dp)
            else Text(number, color = if (current) JellyPurple else JellyMuted, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text(label, color = if (current) JellyInk else JellyMuted, fontWeight = FontWeight.Bold, fontSize = 7.5f.sp)
        }
    }
}

@Composable
private fun V95VoteWinnerCard(vote: Vote, onOpen: (() -> Unit)? = null) {
    val winner = when (vote.winnerUserId) {
        vote.leftUserId -> vote.user1
        vote.rightUserId -> vote.user2
        else -> null
    } ?: return
    val votes = if (vote.winnerUserId == vote.leftUserId) vote.votes1 else vote.votes2
    JellyGlass(Modifier.fillMaxWidth(), radius = 28.dp, padding = 14.dp, onClick = onOpen) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            JellyIcon(JellyIcons.Crown, size = 48.dp)
            Avatar(winner, 96.dp)
            Text("CONGRATULATIONS", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
            UserName(winner, 18)
            if (winner.username.isNotBlank()) Text("@${winner.username}", color = JellyMuted, fontSize = 9.5f.sp)
            Text("Winner of the voting match · $votes vote${if (votes == 1) "" else "s"}", color = JellyInk, fontSize = 10.sp)
            if (onOpen != null) Text("View final result →", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
        }
    }
}

@Composable
private fun V95VoteCard(
    vote: Vote,
    meId: Long,
    detail: Boolean,
    onCast: (Long, Long) -> Unit,
    onRespond: (Long, String) -> Unit,
    onStart: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onLeave: (Long) -> Unit,
    onOpen: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onStatement: (Long, String) -> Unit
) {
    val waiting = vote.status == "waiting"
    val ready = vote.status == "ready"
    val active = vote.status == "active"
    val ended = !waiting && !ready && !active
    val isLeft = meId == vote.leftUserId
    val isRight = meId == vote.rightUserId
    val candidate = isLeft || isRight
    val canAccept = waiting && isRight
    val canStart = ready && isLeft

    val winner = when (vote.winnerUserId) {
        vote.leftUserId -> vote.user1
        vote.rightUserId -> vote.user2
        else -> null
    }

    var now by remember(vote.id, vote.endsAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vote.id, vote.status, vote.endsAt) {
        while (vote.status == "active") {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val totalVotes = (vote.votes1 + vote.votes2).coerceAtLeast(0)
    val leftPct = if (totalVotes > 0) (vote.votes1 * 100 / totalVotes) else 50
    val rightPct = 100 - leftPct

    val hint = when {
        waiting && isRight -> "You were invited. Accept to enter the waiting room."
        waiting -> "Invite sent. Waiting for ${vote.user2?.name.orEmpty().ifBlank { "opponent" }} to accept."
        ready && canStart -> "Both players are ready. Press Start Match to begin voting."
        ready -> "Accepted. Waiting for ${vote.user1?.name.orEmpty().ifBlank { "host" }} to press Start Match."
        active -> "The match is live. Visitors can vote once; candidates cannot vote for themselves."
        winner != null -> "Match finished. ${winner.name} won."
        else -> "Match finished."
    }

    JellyGlass(Modifier.fillMaxWidth(), radius = 28.dp, padding = 13.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(voteStatusLabelNative(vote).uppercase(), color = if (active) JellyGreen else JellyMuted, fontWeight = FontWeight.Black, fontSize = 8.5f.sp)
                    Text(
                        "${vote.user1?.name.orEmpty().ifBlank { "Player 1" }}  vs  ${vote.user2?.name.orEmpty().ifBlank { "Player 2" }}",
                        color = JellyInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                Text(
                    when {
                        active -> voteCountdownText(vote.endsAt, now)
                        ready -> "Ready to start"
                        waiting -> "Waiting for acceptance"
                        vote.status == "forfeit" -> "Ended by withdrawal"
                        vote.status == "cancelled" -> "Cancelled"
                        else -> "Results ready"
                    },
                    color = if (active) JellyGreen else JellyMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5f.sp
                )
            }

            VoteStepStrip(vote)

            JellyGlass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 8.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            waiting -> "Step 2"
                            ready -> "Step 3"
                            active -> "Live Match"
                            else -> "Result"
                        },
                        color = JellyPurple,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(hint, color = JellyMuted, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }

            JellyGlass(Modifier.fillMaxWidth(), radius = 26.dp, padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    VoteArenaPlayer(
                        vote = vote,
                        user = vote.user1,
                        side = "left",
                        meId = meId,
                        active = active,
                        waiting = false,
                        ended = ended,
                        modifier = Modifier.weight(1f),
                        onCast = onCast
                    )
                    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (active) Text(voteCountdownText(vote.endsAt, now), color = JellyMuted, fontSize = 6.8f.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Box(
                            Modifier
                                .size(47.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color.White,
                                            Color(0xFFF678EB).copy(alpha = .82f),
                                            Color(0xFF4BA7FF).copy(alpha = .90f)
                                        )
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = .90f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(
                                    when {
                                        waiting -> "WAIT"
                                        ready -> "READY"
                                        active -> "LIVE"
                                        else -> "RESULT"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.sp
                                )
                            }
                        }
                        if (ended) {
                            Text("${vote.votes1} – ${vote.votes2}", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text("$totalVotes total votes", color = JellyMuted, fontSize = 7.5f.sp)
                        }
                    }
                    VoteArenaPlayer(
                        vote = vote,
                        user = vote.user2,
                        side = "right",
                        meId = meId,
                        active = active,
                        waiting = waiting,
                        ended = ended,
                        modifier = Modifier.weight(1f),
                        onCast = onCast
                    )
                }
            }

            if (waiting) {
                JellyGlass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 9.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(JellyIcons.Clock, size = 26.dp)
                        Spacer(Modifier.width(7.dp))
                        Column {
                            Text(if (isRight) "Challenge invitation" else "Waiting room", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            Text(if (isRight) "Accept or decline below." else "The match cannot start until the invited player accepts.", color = JellyMuted, fontSize = 8.5f.sp)
                        }
                    }
                }
            }

            if (ready) {
                JellyGlass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 9.dp) {
                    Column {
                        Text("Both profiles are locked in", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text(if (canStart) "You are the host. Start the match when you are ready." else "The host will start the match.", color = JellyMuted, fontSize = 8.5f.sp)
                    }
                }
            }

            if (active) {
                JellyGlass(Modifier.fillMaxWidth(), radius = 15.dp, padding = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(if (vote.resultRevealed) JellyIcons.Eye else JellyIcons.Lock, size = 24.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (vote.resultRevealed) "Live vote counts are ON."
                            else "Vote totals stay hidden until the timer ends.",
                            color = JellyMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            if (ended) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("$leftPct%  ${vote.user1?.name.orEmpty()}", Modifier.weight(1f), color = JellyMuted, fontSize = 8.5f.sp)
                        Text("${vote.user2?.name.orEmpty()}  $rightPct%", color = JellyMuted, fontSize = 8.5f.sp)
                    }
                    Row(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp))) {
                        Box(Modifier.weight(leftPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(JellyPink))
                        Box(Modifier.weight(rightPct.coerceAtLeast(1).toFloat()).fillMaxHeight().background(JellyPurple))
                    }
                    if (winner == null) {
                        Text(
                            when {
                                vote.tie -> "It’s a tie!"
                                vote.status == "declined" -> "Challenge was declined"
                                vote.status == "cancelled" -> "Voting cancelled"
                                else -> "Match completed"
                            },
                            Modifier.fillMaxWidth(),
                            color = JellyMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            if (canAccept) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    JellyButton("Accept Challenge", Modifier.weight(1f), primary = true, icon = JellyIcons.Check) { onRespond(vote.id, "accept") }
                    JellyButton("Decline", Modifier.weight(1f), danger = true, icon = JellyIcons.Close) { onRespond(vote.id, "decline") }
                }
            }
            if (canStart) {
                JellyButton("Start Match", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Vote) { onStart(vote.id) }
            }
            if (waiting && isLeft) {
                JellyButton("Cancel Invite", Modifier.fillMaxWidth(), danger = true, icon = JellyIcons.Delete) { onCancel(vote.id) }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!detail) {
                    JellyButton("Open Match", Modifier.weight(1f), icon = JellyIcons.Arrow) { onOpen() }
                }
                if (candidate && active) {
                    JellyButton("Leave Match", Modifier.weight(1f), danger = true, icon = JellyIcons.Logout) { onLeave(vote.id) }
                }
                JellyButton("Comments", Modifier.weight(1f), icon = JellyIcons.Comment) { onComments() }
            }
            JellyButton("Share to Profile", Modifier.fillMaxWidth(), icon = JellyIcons.Share) { onShare() }

            if (detail && candidate && (waiting || ready || active)) {
                var line by remember(vote.id, isLeft, vote.leftText, vote.rightText) {
                    mutableStateOf(if (isLeft) vote.leftText else vote.rightText)
                }
                OutlinedTextField(
                    line,
                    { line = it.take(500) },
                    Modifier.fillMaxWidth(),
                    label = { Text("My match line") },
                    placeholder = { Text("Write a short line under your photo…") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp)
                )
                JellyButton("Save my line", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check) {
                    onStatement(vote.id, line.trim())
                }
            }
        }
    }
}

@Composable
private fun VoteArenaPlayer(
    vote: Vote,
    user: User?,
    side: String,
    meId: Long,
    active: Boolean,
    waiting: Boolean,
    ended: Boolean,
    modifier: Modifier,
    onCast: (Long, Long) -> Unit
) {
    val userId = user?.id ?: 0L
    val candidate = meId == vote.leftUserId || meId == vote.rightUserId
    val myVote = vote.myChoice == userId
    val canVote = active && meId > 0L && !candidate && vote.myChoice == 0L && userId > 0L
    val votes = if (side == "left") vote.votes1 else vote.votes2
    val winner = ended && vote.winnerUserId == userId && userId > 0L

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (user != null) {
            Avatar(user, 62.dp)
            UserName(user, 10)
            if (user.username.isNotBlank()) Text("@${user.username}", color = JellyMuted, fontSize = 8.sp)
        } else {
            JellyIcon(JellyIcons.User, size = 54.dp)
        }
        val line = if (side == "left") vote.leftText else vote.rightText
        if (line.isNotBlank()) Text(line, color = JellyInk, fontSize = 8.5f.sp, lineHeight = 11.sp, maxLines = 3)

        if (active && vote.resultRevealed) {
            Text("$votes votes", color = JellyMuted, fontWeight = FontWeight.Black, fontSize = 8.5f.sp)
        } else if (!ended) {
            Text(
                when {
                    waiting -> "Waiting"
                    active -> "Live"
                    else -> "Ready"
                },
                color = if (active) JellyGreen else JellyMuted,
                fontWeight = FontWeight.Black,
                fontSize = 8.sp
            )
        }

        if (winner) Text("Winner", color = JellyGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
        if (myVote) Text("Voted", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 8.5f.sp)
        if (canVote) {
            JellyButton("Vote", primary = true, icon = JellyIcons.Vote) { onCast(vote.id, userId) }
        }
    }
}

@Composable
fun SavedScreen(
    posts: List<Post>, loading: Boolean, error: String?, onProfile: (Long) -> Unit,
    onLike: (Post) -> Unit, onComment: (Post) -> Unit, onShare: (Post) -> Unit, onSave: (Post) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Saved", "Posts and videos you saved for later", JellyIcons.Save) }
        if (loading && posts.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        if (!loading && posts.isEmpty() && error == null) item { EmptyCard("You have not saved any posts or videos yet.", JellyIcons.Save) }
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
    loggedIn: Boolean,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onLogin: () -> Unit,
    onProfile: (Long) -> Unit,
    onShop: (Long) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { PageTitle("Search", "Fast search across My Chhachh", JellyIcons.Search) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 8.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    JellyGlass(
                        Modifier.weight(1f).height(50.dp),
                        radius = 20.dp,
                        padding = 0.dp,
                        surfaceColor = LiveJellyTheme.inputColor,
                        surfaceOpacity = LiveJellyTheme.inputOpacity
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            JellyIcon(JellyIcons.Search, size = 27.dp)
                            Spacer(Modifier.width(6.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = query,
                                onValueChange = onQuery,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = JellyInk,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (query.isBlank()) {
                                            Text("Search people, shops or posts…", color = JellyMuted, fontSize = 10.5f.sp, maxLines = 1)
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                    JellyButton("Search", Modifier.height(50.dp), primary = true, icon = JellyIcons.Search, onClick = onSearch)
                }
            }
        }

        if (query.isBlank() && !loading) {
            item {
                JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Start typing to search", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("Search People, Shops or Posts across My Chhachh.", color = JellyMuted, fontSize = 9.5f.sp)
                    }
                }
            }
        }

        if (loading) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onSearch) } }

        if (result.users.isNotEmpty()) item { SectionTitle("People") }
        items(result.users, key = { "su-${it.id}" }) { u ->
            JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 9.dp, onClick = { onProfile(u.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u, 46.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        UserName(u, 13)
                        if (u.username.isNotBlank()) Text("@${u.username}", color = JellyMuted, fontSize = 9.5f.sp)
                    }
                    JellyIcon(JellyIcons.Arrow, size = 20.dp)
                }
            }
        }

        if (result.shops.isNotEmpty()) item { SectionTitle("Shops") }
        items(result.shops, key = { "ss-${it.id}" }) { s ->
            JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 9.dp, onClick = { onShop(s.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(99.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!s.photo.isNullOrBlank()) {
                            AsyncImage(s.photo, s.name, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        } else {
                            JellyIcon(JellyIcons.Shop, size = 34.dp)
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                        if (s.username.isNotBlank()) Text("@${s.username}", color = JellyMuted, fontSize = 9.5f.sp, maxLines = 1)
                    }
                    Text("View", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
            }
        }

        if (result.posts.isNotEmpty()) item { SectionTitle("Posts") }
        items(result.posts, key = { "sp-${it.id}" }) { p ->
            PostCard(
                post = p,
                loggedIn = loggedIn,
                onLogin = onLogin,
                onProfile = { onProfile(p.user.id) },
                onLike = onLike,
                onComment = onComment,
                onShare = onShare,
                onSave = onSave
            )
        }

        if (!loading && query.isNotBlank() && result.users.isEmpty() && result.shops.isEmpty() && result.posts.isEmpty() && error == null) {
            item { EmptyCard("No results found.", JellyIcons.Search) }
        }
    }
}

@Composable
fun MapScreen(
    shops: List<Shop>,
    onGeocode: suspend (String) -> JSONObject,
    onRoute: suspend (Double, Double, Double, Double) -> JSONObject,
    onOpenShop: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapView = rememberMapViewWithLifecycle()

    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var status by remember { mutableStateOf("Map ready — choose a start and destination.") }
    var loading by remember { mutableStateOf(false) }
    var routeDistance by remember { mutableStateOf<Double?>(null) }
    var routeDuration by remember { mutableStateOf<Double?>(null) }

    fun placeFrom(d: JSONObject, fallback: String): Pair<GeoPoint, String>? {
        val obj = d.optJSONArray("items")?.optJSONObject(0) ?: d.optJSONObject("item") ?: d
        val lat = obj.optDouble("lat", Double.NaN)
        val lng = obj.optDouble("lng", obj.optDouble("lon", Double.NaN))
        if (!lat.isFinite() || !lng.isFinite()) return null
        val label = obj.optString("display_name", obj.optString("name", fallback)).ifBlank { fallback }
        return GeoPoint(lat, lng) to label
    }

    fun openNavigation() {
        val destination = endPoint?.let { "${it.latitude},${it.longitude}" }
            ?: toText.trim().takeIf { it.isNotBlank() }
            ?: return
        val origin = startPoint?.let { "${it.latitude},${it.longitude}" }
            ?: fromText.trim().takeIf { it.isNotBlank() }
        val uri = Uri.parse(
            buildString {
                append("https://www.google.com/maps/dir/?api=1")
                if (!origin.isNullOrBlank()) append("&origin=").append(Uri.encode(origin))
                append("&destination=").append(Uri.encode(destination))
                append("&travelmode=driving")
            }
        )
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    fun planRoute(destinationOverride: String? = null) {
        val destinationText = destinationOverride?.trim().orEmpty().ifBlank { toText.trim() }
        if (destinationText.isBlank()) {
            status = "Enter a destination."
            return
        }
        scope.launch {
            loading = true
            status = "Finding route…"
            routeDistance = null
            routeDuration = null
            try {
                val start = startPoint ?: run {
                    val source = fromText.trim()
                    if (source.isBlank()) throw IllegalArgumentException("Choose starting point or use My location.")
                    val found = placeFrom(onGeocode(source), source) ?: throw IllegalArgumentException("Starting place could not be found.")
                    fromText = found.second
                    found.first.also { startPoint = it }
                }
                val foundEnd = placeFrom(onGeocode(destinationText), destinationText)
                    ?: throw IllegalArgumentException("Destination could not be found.")
                val finish = foundEnd.first
                toText = foundEnd.second
                endPoint = finish

                val d = onRoute(start.latitude, start.longitude, finish.latitude, finish.longitude)
                val route = d.optJSONObject("route")
                val geometry = route?.optJSONObject("geometry")
                val coords = geometry?.optJSONArray("coordinates")
                val points = mutableListOf<GeoPoint>()
                if (coords != null) {
                    for (i in 0 until coords.length()) {
                        val pair = coords.optJSONArray(i) ?: continue
                        val lng = pair.optDouble(0, Double.NaN)
                        val lat = pair.optDouble(1, Double.NaN)
                        if (lat.isFinite() && lng.isFinite()) points += GeoPoint(lat, lng)
                    }
                }
                routePoints = if (points.size >= 2) points else listOf(start, finish)
                routeDistance = route?.optDouble("distance", Double.NaN)?.takeIf { it.isFinite() }
                routeDuration = route?.optDouble("duration", Double.NaN)?.takeIf { it.isFinite() }
                status = if (route != null) {
                    val km = ((routeDistance ?: 0.0) / 1000.0)
                    val mins = kotlin.math.max(1, kotlin.math.round((routeDuration ?: 0.0) / 60.0).toInt())
                    String.format(java.util.Locale.US, "%.1f km · about %d min by road", km, mins)
                } else {
                    "Places found. Open Navigation for live road directions."
                }
            } catch (e: Exception) {
                routePoints = listOfNotNull(startPoint, endPoint)
                status = e.message ?: "Route could not be found. Check the place name and try again."
            } finally {
                loading = false
            }
        }
    }

    fun setCurrentLocation() {
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
            startPoint = GeoPoint(location.latitude, location.longitude)
            fromText = "My current location"
            status = "Starting point set to your current location."
        } else {
            status = "Current location is not available yet."
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) setCurrentLocation()
        else status = "Location permission was denied."
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Chhachh Map", "Plan a route between villages, places and local shops", JellyIcons.Map) }

        item {
            JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            JellyIcon(JellyIcons.Map, size = 34.dp)
                            Spacer(Modifier.width(7.dp))
                            Column {
                                Text("Chhachh Navigation", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(
                                    "Search villages, places and shops around Chhachh / Hazro",
                                    color = JellyMuted,
                                    fontSize = 9.sp,
                                    maxLines = 2
                                )
                            }
                        }
                        JellyPill("Chhachh", true) {}
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("From", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 9.5f.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OutlinedTextField(
                                fromText,
                                {
                                    fromText = it
                                    if (it != "My current location") startPoint = null
                                },
                                Modifier.weight(1f),
                                placeholder = { Text("Village / place or use current location") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp)
                            )
                            JellyButton("My location", icon = JellyIcons.Pin) {
                                val granted =
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (granted) setCurrentLocation()
                                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("To", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 9.5f.sp)
                        OutlinedTextField(
                            toText,
                            {
                                toText = it
                                endPoint = null
                            },
                            Modifier.fillMaxWidth(),
                            placeholder = { Text("Village, place or shop") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JellyButton(
                            if (loading) "Finding Route…" else "Find Route",
                            Modifier.weight(1f),
                            primary = true,
                            icon = JellyIcons.Map,
                            enabled = !loading && toText.isNotBlank()
                        ) { planRoute() }
                        JellyButton(
                            "Open Navigation",
                            Modifier.weight(1f),
                            icon = JellyIcons.Arrow
                        ) { openNavigation() }
                    }

                    Text(status, color = JellyMuted, fontSize = 9.5f.sp, fontWeight = FontWeight.Bold)

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        AndroidView(
                            factory = { mapView },
                            update = { map ->
                                map.overlays.removeAll { it is Marker || it is Polyline }

                                startPoint?.let { point ->
                                    map.overlays.add(Marker(map).apply {
                                        position = point
                                        title = "Start"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    })
                                }
                                endPoint?.let { point ->
                                    map.overlays.add(Marker(map).apply {
                                        position = point
                                        title = "Destination"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    })
                                }
                                if (routePoints.size >= 2) {
                                    map.overlays.add(Polyline().apply { setPoints(routePoints) })
                                    val maxLat = routePoints.maxOf { it.latitude }
                                    val minLat = routePoints.minOf { it.latitude }
                                    val maxLng = routePoints.maxOf { it.longitude }
                                    val minLng = routePoints.minOf { it.longitude }
                                    map.zoomToBoundingBox(BoundingBox(maxLat, maxLng, minLat, minLng), true, 60)
                                } else if (startPoint != null) {
                                    map.controller.setZoom(14.0)
                                    map.controller.animateTo(startPoint)
                                }
                                map.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            Modifier.align(Alignment.TopEnd).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            JellyGlass(Modifier.size(40.dp), radius = 13.dp, onClick = { mapView.controller.zoomOut() }) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("−", color = JellyInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            JellyGlass(Modifier.size(40.dp), radius = 13.dp, onClick = { mapView.controller.zoomIn() }) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("+", color = JellyInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    Text("© OpenStreetMap contributors", color = JellyMuted, fontSize = 8.sp)

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Route to a shop", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(
                                "Choose Route for directions or View to open the shop",
                                color = JellyMuted,
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (shops.isEmpty()) {
                        Text("No shops have been added yet.", color = JellyMuted, fontSize = 10.sp)
                    } else {
                        shops.take(12).forEach { shop ->
                            val destination = listOf(shop.location, shop.area, shop.village, shop.city, "Attock Pakistan")
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            JellyGlass(Modifier.fillMaxWidth(), radius = 20.dp, padding = 10.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(56.dp).clip(RoundedCornerShape(99.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!shop.photo.isNullOrBlank()) {
                                                AsyncImage(
                                                    shop.photo,
                                                    shop.name,
                                                    Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                JellyIcon(JellyIcons.Shop, size = 38.dp)
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(shop.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                                            Text(shop.category.ifBlank { "Local shop" }, color = JellyMuted, fontSize = 10.sp, maxLines = 1)
                                            val place = listOf(shop.area, shop.village, shop.city)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · ")
                                            if (place.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    JellyIcon(JellyIcons.Pin, size = 14.dp)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(place, color = JellyMuted, fontSize = 10.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        JellyButton("Route", Modifier.weight(1f), primary = true, icon = JellyIcons.Map) {
                                            toText = destination
                                            endPoint = null
                                            if (startPoint != null || fromText.isNotBlank()) planRoute(destination)
                                        }
                                        JellyButton("View", Modifier.weight(1f), icon = JellyIcons.Eye) {
                                            onOpenShop(shop.id)
                                        }
                                        if (shop.whatsapp.isNotBlank()) {
                                            Box(Modifier.width(44.dp)) {
                                                JellyButton("", Modifier.fillMaxWidth(), icon = JellyIcons.Whatsapp) {
                                                    val digits = shop.whatsapp.filter(Char::isDigit)
                                                    if (digits.isNotBlank()) runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "Find Route draws the route on this map. Open Navigation opens live turn-by-turn directions in Google Maps.",
                        color = JellyMuted,
                        fontSize = 8.5f.sp
                    )
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
            setUseDataConnection(true)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 9.0
            maxZoomLevel = 19.0
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(33.90977, 72.438))
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
        contentPadding = PaddingValues(LiveJellyTheme.framePadding.dp, 8.dp, LiveJellyTheme.framePadding.dp, 18.dp),
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
                            JellyIcon(JellyIcons.Weather, size = 48.dp)
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
