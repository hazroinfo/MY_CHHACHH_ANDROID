package com.mychhachh.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.mychhachh.app.data.Announcement
import com.mychhachh.app.data.Comment
import java.io.File

@Composable
internal fun V95AnnouncementComposer(c: V95Controller) {
    if (c.user == null) return

    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var audio by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    fun startRecording() {
        runCatching {
            val file = File(context.cacheDir, "announcement_${java.lang.System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = file
            recording = true
        }.onFailure { c.error = it.message ?: c.t("Could not start recording", "ریکارڈنگ شروع نہیں ہوئی") }
    }

    fun stopRecording() {
        val r = recorder
        recorder = null
        recording = false
        runCatching { r?.stop() }
        runCatching { r?.release() }
        val file = recordingFile
        if (file != null && file.exists() && file.length() > 0) {
            c.uploadFile(file, "audio/mp4", "announcement_audio") { url ->
                audio = url
                runCatching { file.delete() }
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
        else c.error = c.t("Microphone permission is required.", "مائیکروفون کی اجازت درکار ہے۔")
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "announcement_photo") { photo = it }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) {
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
            }
        }
    }

    V95GlassCard {
        Text(c.t("Create announcement", "اعلان بنائیں"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
        V95TextArea(text, c.t("Write an optional message…", "اختیاری پیغام لکھیں…")) { text = it }

        if (photo.isNotBlank()) {
            AsyncImage(photo, null, Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
        }
        if (audio.isNotBlank()) V95NativeAudio(audio)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            V95Button(c.t("Photo", "فوٹو"), Modifier.weight(1f), icon = V95Icons.Photo) {
                photoPicker.launch("image/*")
            }
            V95Button(
                if (recording) c.t("Stop", "روکیں") else c.t("Voice", "وائس"),
                Modifier.weight(1f),
                primary = recording,
                icon = V95Icons.Message
            ) {
                if (recording) {
                    stopRecording()
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            V95Button(
                c.t("Publish", "شائع کریں"),
                Modifier.weight(1f),
                primary = true,
                enabled = !recording && (text.isNotBlank() || photo.isNotBlank() || audio.isNotBlank())
            ) {
                c.createAnnouncement(text, photo, audio) {
                    text = ""
                    photo = ""
                    audio = ""
                }
            }
        }
    }
}

@Composable
internal fun V95AnnouncementDetail(c: V95Controller) {
    val item = c.selectedAnnouncement
    if (item == null) {
        V95Empty(c.t("Announcement unavailable", "اعلان دستیاب نہیں"))
        return
    }

    var comment by remember(item.id) { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V95Button(c.t("Back", "واپس")) { c.route = V95Route.ANNOUNCEMENTS }
                Spacer(Modifier.width(8.dp))
                V95PageHeading(c.t("Announcement", "اعلان"), "")
            }
        }

        item { V95AnnouncementCard(c, item, detail = true) }

        item {
            V95GlassCard(radius = 22.dp, padding = 12.dp) {
                Text(c.t("Comments", "کمنٹس"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                if (c.user == null) {
                    V95Button(c.t("Login to comment", "کمنٹ کے لیے لاگ اِن کریں"), primary = true) { c.route = V95Route.AUTH }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        V95InputShell(Modifier.weight(1f)) {
                            BasicTextField(
                                comment,
                                { comment = it },
                                Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = V95Ink, fontSize = 13.sp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (comment.isBlank()) Text(c.t("Write a comment…", "کمنٹ لکھیں…"), color = V95Muted, fontSize = 12.sp)
                                        inner()
                                    }
                                }
                            )
                        }
                        V95Button(c.t("Send", "بھیجیں"), primary = true, enabled = comment.isNotBlank()) {
                            c.addAnnouncementComment(comment) { comment = "" }
                        }
                    }
                }
            }
        }

        if (c.announcementCommentsList.isEmpty()) {
            item { V95Empty(c.t("No comments yet", "ابھی کوئی کمنٹ نہیں")) }
        } else {
            items(c.announcementCommentsList, key = { it.id }) { V95AnnouncementComment(c, it) }
        }
    }
}

@Composable
internal fun V95AnnouncementCard(c: V95Controller, item: Announcement, detail: Boolean = false) {
    V95GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.author?.let { V95Avatar(it, 42.dp) }
            if (item.author != null) Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.author?.name ?: c.t("My Chhachh", "مائی چھچھ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(item.createdAt.replace('T', ' ').take(16), color = V95Muted, fontSize = 9.sp)
            }
            if (!detail) V95Button(c.t("Open", "کھولیں")) { c.openAnnouncement(item) }
        }

        if (item.text.isNotBlank()) Text(item.text, color = V95Ink, fontSize = 13.sp, lineHeight = 19.sp)
        if (!item.photo.isNullOrBlank()) {
            AsyncImage(item.photo, null, Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
        }
        if (!item.audio.isNullOrBlank()) V95NativeAudio(item.audio)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            V95PostAction(c.t("Like", "لائک"), V95Icons.Heart, active = item.liked, count = item.likes) {
                if (c.user == null) c.route = V95Route.AUTH else c.likeAnnouncement(item)
            }
            V95PostAction(c.t("Comment", "کمنٹ"), V95Icons.Comment, count = item.comments) { c.openAnnouncement(item) }
            if (c.user?.id == item.author?.id || c.user?.isAdmin == true) {
                V95PostAction(c.t("Delete", "حذف"), V95Icons.More) { c.deleteAnnouncement(item) }
            } else {
                V95PostAction(c.t("Report", "رپورٹ"), V95Icons.Shield) {
                    if (c.user == null) c.route = V95Route.AUTH else c.reportAnnouncement(item)
                }
            }
        }
    }
}

@Composable
private fun V95AnnouncementComment(c: V95Controller, item: Comment) {
    V95GlassCard(radius = 19.dp, padding = 10.dp) {
        Row(verticalAlignment = Alignment.Top) {
            V95Avatar(item.user, 36.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.user.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(item.text, color = V95Ink, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
internal fun V95NativeAudio(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    var playing by remember(player) { mutableStateOf(false) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .52f)).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        V95Button(if (playing) "❚❚" else "▶", primary = playing) {
            if (player.isPlaying) {
                player.pause()
                playing = false
            } else {
                player.play()
                playing = true
            }
        }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = true; this.player = player } },
            update = { it.player = player },
            modifier = Modifier.weight(1f).height(48.dp)
        )
    }
}
