package com.mychhachh.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

@Composable
internal fun V95MessageComposer(c: V95Controller) {
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var audio by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    fun startRecording() {
        runCatching {
            val file = File(context.cacheDir, "message_${java.lang.System.currentTimeMillis()}.m4a")
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
            c.uploadFile(file, "audio/mp4", "message_audio") { url ->
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
        if (uri != null) c.upload(uri, "message_photo") { photo = it }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) {
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (photo.isNotBlank()) {
            Text(c.t("Photo attached", "فوٹو شامل ہے"), color = V95Green)
        }
        if (audio.isNotBlank()) V95NativeAudio(audio)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V95Button(c.t("Photo", "فوٹو"), icon = V95Icons.Photo) { photoPicker.launch("image/*") }
            V95Button(
                if (recording) c.t("Stop", "روکیں") else c.t("Voice", "وائس"),
                primary = recording,
                icon = V95Icons.Message
            ) {
                if (recording) stopRecording()
                else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
                else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
            V95InputShell(Modifier.weight(1f)) {
                androidx.compose.foundation.text.BasicTextField(
                    text,
                    { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = V95Ink),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isBlank()) Text(c.t("Message…", "پیغام…"), color = V95Muted)
                            inner()
                        }
                    }
                )
            }
            V95Button(
                c.t("Send", "بھیجیں"),
                primary = true,
                enabled = !recording && (text.isNotBlank() || photo.isNotBlank() || audio.isNotBlank()),
                icon = V95Icons.Send
            ) {
                c.sendRichMessage(text, photo, audio) {
                    text = ""
                    photo = ""
                    audio = ""
                }
            }
        }
    }
}
