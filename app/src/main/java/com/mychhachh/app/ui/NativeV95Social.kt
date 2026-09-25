package com.mychhachh.app.ui

import android.Manifest
import android.media.MediaRecorder
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import kotlinx.coroutines.delay
import java.io.File
import java.time.Duration
import java.time.Instant

@Composable
internal fun NativePeople(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.people.isEmpty()) c.loadPeople() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("People", "لوگ"), c.t("People you may know", "لوگ جنہیں آپ جانتے ہوں")) }
        if (c.people.isEmpty() && !c.busy) item { NVEmpty(c.t("No people found", "کوئی یوزر نہیں ملا")) }
        items(c.people, key = { it.id }) { person ->
            NVCard(radius = 22.dp, padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NVAvatar(person, 52.dp, Modifier.clickable { c.openProfile(person) })
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { c.openProfile(person) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            if (person.verified) {
                                Spacer(Modifier.width(3.dp))
                                Image(painterResource(NVIcons.Check), null, Modifier.size(16.dp))
                            }
                        }
                        Text("@" + person.username, color = NVMuted, fontSize = 10.sp)
                        val place = listOf(person.village, person.city).filter { it.isNotBlank() }.joinToString(" · ")
                        if (place.isNotBlank()) Text(place, color = NVMuted, fontSize = 9.5.sp)
                    }
                    if (c.user?.id != person.id) {
                        NVButton(
                            if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                            primary = !person.followed,
                            icon = NVIcons.Follow
                        ) {
                            if (c.user == null) c.route = V95Route.AUTH else c.follow(person)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeShops(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.shops.isEmpty()) c.loadShops() }
    var editorOpen by remember { mutableStateOf(false) }
    val myShop = c.user?.let { me -> c.shops.firstOrNull { it.userId == me.id } }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Shops", "دکانیں"), c.t("Local shops and services", "مقامی دکانیں اور سروسز")) }

        if (c.user != null) {
            item {
                NVCard(radius = 22.dp, padding = 10.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(NVIcons.Shop), null, Modifier.size(36.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                myShop?.name ?: c.t("Your shop", "آپ کی دکان"),
                                color = NVInk,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Text(
                                if (myShop == null) c.t("Create one professional shop profile", "ایک پروفیشنل دکان پروفائل بنائیں")
                                else c.t("Manage shop details and posts", "دکان کی تفصیل اور پوسٹس مینیج کریں"),
                                color = NVMuted,
                                fontSize = 9.5.sp
                            )
                        }
                        NVButton(
                            if (myShop == null) c.t("Create", "بنائیں") else c.t("Manage", "مینیج"),
                            primary = true,
                            icon = if (myShop == null) NVIcons.Plus else NVIcons.Edit
                        ) { editorOpen = true }
                    }
                    if (myShop != null) {
                        NVButton(c.t("Open my shop", "میری دکان کھولیں"), Modifier.fillMaxWidth(), icon = NVIcons.Shop) {
                            c.openShop(myShop)
                        }
                    }
                }
            }
        }

        if (editorOpen && c.user != null) {
            item {
                NativeShopEditor(c, myShop) { editorOpen = false }
            }
        }

        if (c.shops.isEmpty() && !c.busy) item { NVEmpty(c.t("No shops found", "کوئی دکان نہیں ملی")) }
        items(c.shops, key = { it.id }) { shop ->
            NVCard(radius = 22.dp, padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NVShopAvatar(shop, 54.dp, Modifier.clickable { c.openShop(shop) })
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { c.openShop(shop) }) {
                        Text(shop.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(shop.category, color = NVMuted, fontSize = 10.sp)
                        val place = listOf(shop.village, shop.city).filter { it.isNotBlank() }.joinToString(" · ")
                        if (place.isNotBlank()) Text(place, color = NVMuted, fontSize = 9.5.sp)
                    }
                    if (c.user?.id == shop.userId) {
                        NVButton(c.t("Edit", "ایڈٹ"), icon = NVIcons.Edit) { editorOpen = true }
                    } else {
                        NVButton(
                            if (shop.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                            primary = !shop.followed
                        ) {
                            if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeShopEditor(c: V95Controller, shop: Shop?, onClose: () -> Unit) {
    val key = shop?.id ?: 0L
    var name by remember(key) { mutableStateOf(shop?.name.orEmpty()) }
    var username by remember(key) { mutableStateOf(shop?.username.orEmpty()) }
    var category by remember(key) { mutableStateOf(shop?.category.orEmpty()) }
    var description by remember(key) { mutableStateOf(shop?.description.orEmpty()) }
    var photo by remember(key) { mutableStateOf(shop?.photo.orEmpty()) }
    var cover by remember(key) { mutableStateOf(shop?.cover.orEmpty()) }
    var city by remember(key) { mutableStateOf(shop?.city.orEmpty()) }
    var village by remember(key) { mutableStateOf(shop?.village.orEmpty()) }
    var area by remember(key) { mutableStateOf(shop?.area.orEmpty()) }
    var address by remember(key) { mutableStateOf(shop?.location.orEmpty()) }
    var phone by remember(key) { mutableStateOf(shop?.phone.orEmpty()) }
    var whatsapp by remember(key) { mutableStateOf(shop?.whatsapp.orEmpty()) }
    var mapUrl by remember(key) { mutableStateOf(shop?.locationUrl.orEmpty()) }
    var mapPickerOpen by remember(key) { mutableStateOf(false) }
    var deleteConfirm by remember(key) { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "shop_photo") { photo = it }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "shop_cover") { cover = it }
    }

    NVCard(radius = 25.dp, padding = 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NativeSettingsTitle(NVIcons.Shop, if (shop == null) c.t("Create your shop", "اپنی دکان بنائیں") else c.t("Edit your shop", "اپنی دکان ایڈٹ کریں"))
            Spacer(Modifier.weight(1f))
            NVIconButton(NVIcons.Close, size = 34.dp, iconSize = 27.dp) { onClose() }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            NVButton(
                if (photo.isBlank()) c.t("Shop photo", "دکان فوٹو") else c.t("Photo ✓", "فوٹو ✓"),
                Modifier.weight(1f),
                icon = NVIcons.Photo
            ) { photoPicker.launch("image/*") }
            NVButton(
                if (cover.isBlank()) c.t("Cover photo", "کور فوٹو") else c.t("Cover ✓", "کور ✓"),
                Modifier.weight(1f),
                icon = NVIcons.Photo
            ) { coverPicker.launch("image/*") }
        }

        if (cover.isNotBlank() || photo.isNotBlank()) {
            Box(
                Modifier.fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x11000000))
            ) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (photo.isNotBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        NVInput(name, c.t("Shop name", "دکان کا نام")) { name = it }
        NVInput(username, c.t("Shop username", "دکان یوزرنیم")) { username = it }
        NVInput(category, c.t("Category", "کیٹیگری")) { category = it }
        NVInput(description, c.t("Shop description", "دکان کی تفصیل"), Modifier.fillMaxWidth(), singleLine = false) { description = it }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            NVInput(village, c.t("Village", "گاؤں"), Modifier.weight(1f)) { village = it }
            NVInput(area, c.t("Area / Mohalla", "علاقہ / محلہ"), Modifier.weight(1f)) { area = it }
        }
        NVInput(city, c.t("City", "شہر")) { city = it }
        NVInput(address, c.t("Full address", "مکمل پتہ"), Modifier.fillMaxWidth(), singleLine = false) { address = it }
        NVInput(phone, c.t("Phone", "فون")) { phone = it }
        NVInput(whatsapp, "WhatsApp") { whatsapp = it }
        NVInput(mapUrl, c.t("Google Maps link", "Google Maps لنک")) { mapUrl = it }
        NVButton(
            if (mapPickerOpen) c.t("Close map picker", "نقشہ بند کریں") else c.t("Pick location on map", "نقشے سے لوکیشن منتخب کریں"),
            Modifier.fillMaxWidth(),
            icon = NVIcons.Map
        ) { mapPickerOpen = !mapPickerOpen }

        if (mapPickerOpen) {
            NativeLocationPicker(
                c = c,
                modifier = Modifier.fillMaxWidth().height(360.dp),
                initialQuery = address.ifBlank { listOf(village, city).filter { it.isNotBlank() }.joinToString(", ") }
            ) { place ->
                address = place.name
                mapUrl = "https://www.google.com/maps/search/?api=1&query=" +
                    String.format(java.util.Locale.US, "%.6f,%.6f", place.lat, place.lng)
            }
        }

        NVButton(
            if (shop == null) c.t("Create shop", "دکان بنائیں") else c.t("Save shop", "دکان محفوظ کریں"),
            Modifier.fillMaxWidth(),
            primary = true,
            enabled = name.isNotBlank() && username.isNotBlank() && !c.busy
        ) {
            val cleanMapUrl = mapUrl.trim().let { raw ->
                when {
                    raw.isBlank() -> ""
                    raw.startsWith("http://", ignoreCase = true) ||
                        raw.startsWith("https://", ignoreCase = true) ||
                        raw.startsWith("geo:", ignoreCase = true) ||
                        raw.startsWith("google.navigation:", ignoreCase = true) -> raw
                    raw.contains("google.", ignoreCase = true) ||
                        raw.contains("maps.", ignoreCase = true) -> "https://$raw"
                    else -> raw
                }
            }
            val fields = org.json.JSONObject()
                .put("name", name.trim())
                .put("username", username.trim().removePrefix("@"))
                .put("category", category.trim())
                .put("description", description.trim())
                .put("photo", photo.trim())
                .put("cover_photo", cover.trim())
                .put("city", city.trim())
                .put("village", village.trim())
                .put("area", area.trim())
                .put("location", address.trim())
                .put("phone", phone.trim())
                .put("whatsapp", whatsapp.trim())
                .put("location_link", cleanMapUrl)
                .put("location_url", cleanMapUrl)
            c.saveShop(shop, fields) { onClose() }
        }

        if (shop != null) {
            NVButton(c.t("Delete shop", "دکان حذف کریں"), Modifier.fillMaxWidth(), danger = true, icon = NVIcons.Delete) {
                deleteConfirm = true
            }
        }
    }

    if (deleteConfirm && shop != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(c.t("Delete shop?", "دکان حذف کریں؟"), fontWeight = FontWeight.Black) },
            text = { Text(c.t("The shop and its management access will be removed.", "دکان اور اس کی مینجمنٹ رسائی حذف ہو جائے گی۔")) },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirm = false
                    c.deleteMyShop(shop) { onClose() }
                }) { Text(c.t("Delete", "حذف کریں"), color = NVDanger, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) { Text(c.t("Cancel", "منسوخ")) }
            }
        )
    }
}

@Composable
internal fun NativeMessages(c: V95Controller) {
    if (c.user == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { if (c.conversations.isEmpty()) c.loadMessages() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { NVHeading(c.t("Messages", "پیغامات"), c.t("Your conversations", "آپ کی گفتگو")) }
        if (c.conversations.isEmpty() && !c.busy) item { NVEmpty(c.t("No conversations yet", "ابھی کوئی گفتگو نہیں")) }
        items(c.conversations, key = { it.user.id }) { conv ->
            NVCard(radius = 20.dp, padding = 10.dp) {
                Row(
                    Modifier.fillMaxWidth().clickable { c.openChat(conv.user) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NVAvatar(conv.user, 48.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(conv.user.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(conv.preview.ifBlank { c.t("Open conversation", "گفتگو کھولیں") }, color = NVMuted, fontSize = 10.sp, maxLines = 1)
                    }
                    if (conv.unread) Box(Modifier.size(9.dp).clip(CircleShape).background(NVPink))
                }
            }
        }
    }
}

@Composable
internal fun NativeChat(c: V95Controller) {
    val other = c.selectedChatUser
    if (c.user == null || other == null) {
        NativeRequireLogin(c)
        return
    }
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var audio by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "message_photo") { photo = it }
    }

    fun startVoiceMessage() {
        val file = File(context.cacheDir, "message_${System.currentTimeMillis()}.m4a")
        val nextRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
        }
        runCatching {
            nextRecorder.prepare()
            nextRecorder.start()
        }.onSuccess {
            recorder = nextRecorder
            recordingFile = file
            recording = true
        }.onFailure {
            runCatching { nextRecorder.release() }
            c.error = c.t("Voice message could not start.", "وائس پیغام ریکارڈ نہیں ہو سکا۔")
        }
    }

    fun stopVoiceMessage() {
        val active = recorder
        val file = recordingFile
        recorder = null
        recordingFile = null
        recording = false
        if (active != null) {
            val stopped = runCatching { active.stop() }.isSuccess
            runCatching { active.release() }
            if (stopped && file != null && file.exists() && file.length() > 0L) {
                c.uploadFile(file, "audio/mp4", "message_audio") { url ->
                    audio = url
                    runCatching { file.delete() }
                }
            } else {
                runCatching { file?.delete() }
                c.error = c.t("Voice message recording failed.", "وائس پیغام کی ریکارڈنگ ناکام ہو گئی۔")
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceMessage()
        else c.error = c.t("Microphone permission is required.", "مائیکروفون کی اجازت ضروری ہے۔")
    }

    DisposableEffect(Unit) {
        onDispose {
            val active = recorder
            recorder = null
            recording = false
            if (active != null) {
                runCatching { active.stop() }
                runCatching { active.release() }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        NVHeading(other.name, "@" + other.username)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(c.chatMessages, key = { it.id }) { msg ->
                val mine = msg.senderId == c.user?.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier
                            .fillMaxWidth(.78f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (mine) nvPrimaryBrush() else nvGlassBrush())
                            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
                            .padding(10.dp)
                    ) {
                        if (msg.text.isNotBlank()) Text(msg.text, color = if (mine) Color.White else NVInk, fontSize = 12.sp)
                        if (!msg.photo.isNullOrBlank()) AsyncImage(msg.photo, null, Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        if (!msg.audio.isNullOrBlank()) NativeAnnouncementAudioPlayer(msg.audio!!)
                        if (msg.locationLat != null && msg.locationLng != null) {
                            NVButton(c.t("Open location", "لوکیشن کھولیں"), Modifier.fillMaxWidth(), icon = NVIcons.Pin) {
                                c.openCoordinates(msg.locationLat, msg.locationLng)
                            }
                        }
                        Text(msg.createdAt, color = if (mine) Color.White.copy(.82f) else NVMuted, fontSize = 8.sp)
                    }
                }
            }
        }

        if (recording || photo.isNotBlank() || audio.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        recording -> "● " + c.t("Recording voice…", "وائس ریکارڈ ہو رہی ہے…")
                        photo.isNotBlank() -> c.t("Photo attached", "فوٹو منسلک ہے")
                        else -> c.t("Voice attached", "وائس منسلک ہے")
                    },
                    color = if (recording) NVDanger else NVGreen,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                if (!recording) {
                    Text(c.t("Remove", "ہٹائیں"), color = NVPink, fontSize = 10.sp, modifier = Modifier.clickable { photo = ""; audio = "" })
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            NVIconButton(NVIcons.Photo, size = 40.dp, iconSize = 28.dp) { photoPicker.launch("image/*") }
            NVIconButton(NVIcons.Pin, size = 40.dp, iconSize = 28.dp) { c.startMessageLocation() }
            NVIconButton(NVIcons.Message, size = 40.dp, iconSize = 28.dp) {
                if (recording) {
                    stopVoiceMessage()
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startVoiceMessage()
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            NVInput(text, c.t("Message…", "پیغام…"), Modifier.weight(1f)) { text = it }
            NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 32.dp) {
                if (text.isNotBlank() || photo.isNotBlank() || audio.isNotBlank()) {
                    c.sendRichMessage(text, photo, audio) {
                        text = ""; photo = ""; audio = ""
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeVotes(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.votes.isEmpty()) c.loadVotes() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Voting", "ووٹنگ"), c.t("Live community voting", "لائیو کمیونٹی ووٹنگ")) }
        if (c.user != null) item { NativeVoteCreator(c) }
        if (c.votes.isEmpty() && !c.busy) item { NVEmpty(c.t("No active voting", "کوئی ووٹنگ موجود نہیں")) }
        items(c.votes, key = { it.id }) { vote -> NativeVoteCard(c, vote) }
    }
}

@Composable
private fun NativeVoteCreator(c: V95Controller) {
    var opponent by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(24) }
    var expanded by remember { mutableStateOf(false) }

    NVCard(radius = 24.dp, padding = 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(NVIcons.Vote), null, Modifier.size(34.dp))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(c.t("Create voting match", "ووٹنگ مقابلہ بنائیں"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(c.t("Challenge another My Chhachh user", "دوسرے My Chhachh یوزر کو چیلنج کریں"), color = NVMuted, fontSize = 9.5.sp)
            }
            NVButton(if (expanded) c.t("Close", "بند") else c.t("Create", "بنائیں"), primary = !expanded) {
                expanded = !expanded
            }
        }
        if (expanded) {
            NVInput(opponent, c.t("Opponent username, e.g. @name", "مقابل یوزرنیم، مثلاً @name")) { opponent = it }
            NVInput(statement, c.t("Your statement / challenge", "آپ کا بیان / چیلنج"), Modifier.fillMaxWidth(), singleLine = false) { statement = it }
            NVButton(c.t("Duration: ", "مدت: ") + duration + c.t(" hours", " گھنٹے"), Modifier.fillMaxWidth()) {
                duration = when (duration) {
                    24 -> 48
                    48 -> 72
                    else -> 24
                }
            }
            NVButton(
                c.t("Send challenge", "چیلنج بھیجیں"),
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = opponent.trim().removePrefix("@").isNotBlank()
            ) {
                c.createVote(opponent, duration, statement) {
                    opponent = ""
                    statement = ""
                    duration = 24
                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun NativeVoteCard(c: V95Controller, vote: Vote, detail: Boolean = false) {
    var now by remember(vote.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vote.id, vote.endsAt, vote.status) {
        while (vote.status.lowercase() in setOf("active", "started", "live")) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timer = nativeVoteRemaining(vote.endsAt, now, c)

    NVCard(modifier = Modifier.clickable { c.openVote(vote) }, radius = 28.dp, padding = 14.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vote.status.uppercase(), color = NVMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(vote.title.ifBlank { (vote.user1?.name ?: "") + " VS " + (vote.user2?.name ?: "") }, color = NVInk, fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(timer, color = NVPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (vote.resultRevealed && vote.winnerUserId > 0L) {
                val winnerName = when (vote.winnerUserId) {
                    vote.leftUserId -> vote.user1?.name
                    vote.rightUserId -> vote.user2?.name
                    else -> null
                }.orEmpty()
                if (winnerName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFFF0C7), Color(0xFFFFE3F3))))
                            .border(1.5.dp, Color.White, RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("🎉 🏆 " + c.t("Winner: ", "فاتح: ") + winnerName + " 🎈", color = NVPurple, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NativeVoteSide(c, vote.user1, vote.leftText, vote.votes1, vote.leftUserId, vote, Modifier.weight(1f))
            Box(Modifier.size(47.dp).clip(CircleShape).background(nvPrimaryBrush()).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            NativeVoteSide(c, vote.user2, vote.rightText, vote.votes2, vote.rightUserId, vote, Modifier.weight(1f))
        }
        if (detail) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NVButton(c.t("Comments", "کمنٹس"), Modifier.weight(1f), icon = NVIcons.Comment) { c.openVote(vote) }
                NVButton(c.t("Share", "شیئر"), Modifier.weight(1f), primary = true, icon = NVIcons.Share) {
                    if (c.user == null) c.route = V95Route.AUTH else c.shareVote(vote)
                }
            }
        }
    }
}

@Composable
private fun NativeVoteSide(c: V95Controller, person: User?, statement: String, count: Int, id: Long, vote: Vote, modifier: Modifier) {
    Column(modifier.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (person != null) NVAvatar(person, 62.dp) else Box(Modifier.size(62.dp).clip(CircleShape).background(nvInputBrush()))
        Spacer(Modifier.height(4.dp))
        Text(person?.name ?: statement.ifBlank { c.t("Option", "آپشن") }, color = NVInk, fontWeight = FontWeight.Black, fontSize = 10.5.sp, maxLines = 2, textAlign = TextAlign.Center)
        Text(count.toString(), color = NVPurple, fontWeight = FontWeight.Black, fontSize = 15.sp)
        val canVote = vote.status.lowercase() in setOf("active", "started", "live") && id > 0L
        NVButton(
            if (vote.myChoice == id && id > 0L) c.t("Voted", "ووٹ دیا") else c.t("Vote", "ووٹ"),
            primary = vote.myChoice != id,
            enabled = canVote
        ) {
            if (c.user == null) c.route = V95Route.AUTH else c.castVote(vote, id)
        }
    }
}

@Composable
internal fun NativeVoteDetail(c: V95Controller) {
    val vote = c.selectedVote
    if (vote == null) {
        NVEmpty(c.t("Voting unavailable", "ووٹنگ دستیاب نہیں"))
        return
    }
    var comment by remember { mutableStateOf("") }
    var statementEdit by remember(vote.id, vote.updatedAt) {
        mutableStateOf(
            if (c.user?.id == vote.rightUserId) vote.rightText else vote.leftText
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NativeVoteCard(c, vote, detail = true) }

        val meId = c.user?.id ?: 0L
        val status = vote.status.lowercase()
        val participant = meId > 0L && (meId == vote.leftUserId || meId == vote.rightUserId)
        val pending = status in setOf("pending", "invited", "challenge", "challenged", "requested")
        val ready = status in setOf("accepted", "ready")
        val live = status in setOf("active", "started", "live")

        if (participant) {
            item {
                NVCard(radius = 22.dp, padding = 10.dp) {
                    if (pending && meId == vote.rightUserId) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            NVButton(c.t("Accept challenge", "چیلنج قبول کریں"), Modifier.weight(1f), primary = true) {
                                c.respondVote(vote, "accept")
                            }
                            NVButton(c.t("Reject", "مسترد"), Modifier.weight(1f), danger = true) {
                                c.respondVote(vote, "reject")
                            }
                        }
                    }
                    if (ready && meId == vote.leftUserId) {
                        NVButton(c.t("Start voting", "ووٹنگ شروع کریں"), Modifier.fillMaxWidth(), primary = true, icon = NVIcons.Vote) {
                            c.startVote(vote)
                        }
                    }
                    if (pending || ready) {
                        if (meId == vote.leftUserId) {
                            NVButton(c.t("Cancel challenge", "چیلنج منسوخ کریں"), Modifier.fillMaxWidth(), danger = true) {
                                c.cancelVote(vote)
                            }
                        }
                    }
                    if (live) {
                        NVButton(c.t("Leave voting", "ووٹنگ چھوڑیں"), Modifier.fillMaxWidth(), danger = true) {
                            c.leaveVote(vote)
                        }
                    }
                    NVInput(statementEdit, c.t("Your statement", "آپ کا بیان"), Modifier.fillMaxWidth(), singleLine = false) { statementEdit = it }
                    NVButton(c.t("Update statement", "بیان اپڈیٹ کریں"), Modifier.fillMaxWidth()) {
                        c.updateVoteStatement(vote, statementEdit)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NVInput(comment, c.t("Write a comment…", "کمنٹ لکھیں…"), Modifier.weight(1f)) { comment = it }
                NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 31.dp) {
                    if (comment.isNotBlank()) c.addVoteComment(comment) { comment = "" }
                }
            }
        }
        items(c.voteCommentsList, key = { it.id }) { NativeCommentCard(it) }
    }
}

@Composable
internal fun NativeAnnouncements(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.announcements.isEmpty()) c.loadAnnouncements() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Announcements", "اعلانات"), c.t("Voice and community notices", "وائس اور کمیونٹی اعلانات")) }
        if (c.user != null) item { NativeAnnouncementComposer(c) }
        if (c.announcements.isEmpty() && !c.busy) item { NVEmpty(c.t("No announcements", "کوئی اعلان نہیں")) }
        items(c.announcements, key = { it.id }) { item -> NativeAnnouncementCard(c, item) }
    }
}

@Composable
private fun NativeAnnouncementComposer(c: V95Controller) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var audio by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "announcement_photo") { photo = it }
    }

    fun startRecording() {
        val file = File(context.cacheDir, "announcement_${System.currentTimeMillis()}.m4a")
        val nextRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
        }
        runCatching {
            nextRecorder.prepare()
            nextRecorder.start()
        }.onSuccess {
            recorder = nextRecorder
            recordingFile = file
            recording = true
        }.onFailure {
            runCatching { nextRecorder.release() }
            c.error = c.t("Voice recording could not start.", "وائس ریکارڈنگ شروع نہیں ہو سکی۔")
        }
    }

    fun stopRecordingAndUpload() {
        val active = recorder
        val file = recordingFile
        recorder = null
        recordingFile = null
        recording = false
        if (active != null) {
            val stopped = runCatching { active.stop() }.isSuccess
            runCatching { active.release() }
            if (stopped && file != null && file.exists() && file.length() > 0L) {
                c.uploadFile(file, "audio/mp4", "announcement_audio") { url ->
                    audio = url
                    runCatching { file.delete() }
                }
            } else {
                runCatching { file?.delete() }
                c.error = c.t("Voice recording failed.", "وائس ریکارڈنگ ناکام ہو گئی۔")
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
        else c.error = c.t("Microphone permission is required.", "مائیکروفون کی اجازت ضروری ہے۔")
    }

    DisposableEffect(Unit) {
        onDispose {
            val active = recorder
            recorder = null
            recording = false
            if (active != null) {
                runCatching { active.stop() }
                runCatching { active.release() }
            }
        }
    }

    NVCard(radius = 28.dp, padding = 12.dp) {
        Text(c.t("New announcement", "نیا اعلان"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
        NVInput(text, c.t("Write an announcement…", "اعلان لکھیں…"), Modifier.fillMaxWidth(), singleLine = false) { text = it }

        if (recording) {
            Text("● " + c.t("Recording… tap Stop when finished", "ریکارڈنگ جاری ہے… مکمل ہونے پر اسٹاپ دبائیں"), color = NVDanger, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        } else if (audio.isNotBlank()) {
            Text(c.t("Voice ready to post", "وائس پوسٹ کے لیے تیار ہے"), color = NVGreen, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NVButton(c.t("Photo", "فوٹو"), Modifier.weight(1f), icon = NVIcons.Photo) { photoPicker.launch("image/*") }
            NVButton(
                if (recording) c.t("Stop", "اسٹاپ") else c.t("Record", "ریکارڈ"),
                Modifier.weight(1f),
                primary = recording,
                danger = recording,
                icon = NVIcons.Message
            ) {
                if (recording) {
                    stopRecordingAndUpload()
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            NVButton(c.t("Post", "پوسٹ"), Modifier.weight(1f), primary = true, enabled = !recording) {
                if (text.isNotBlank() || photo.isNotBlank() || audio.isNotBlank()) {
                    c.createAnnouncement(text, photo, audio) { text = ""; photo = ""; audio = "" }
                }
            }
        }
    }
}

@Composable
private fun NativeAnnouncementCard(c: V95Controller, item: Announcement) {
    var editOpen by remember(item.id, item.text) { mutableStateOf(false) }
    var editText by remember(item.id, item.text) { mutableStateOf(item.text) }
    var deleteConfirm by remember(item.id) { mutableStateOf(false) }
    val canManage = c.user?.isAdmin == true || (c.user?.id != null && c.user?.id == item.author?.id)

    NVCard(modifier = Modifier.clickable { c.openAnnouncement(item) }, radius = 22.dp, padding = 11.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val author = item.author
            if (author != null) NVAvatar(author, 42.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(author?.name ?: c.t("Announcement", "اعلان"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                Text(item.createdAt, color = NVMuted, fontSize = 9.sp)
            }
            Image(painterResource(NVIcons.Announcement), null, Modifier.size(28.dp))
        }
        if (item.text.isNotBlank()) Text(item.text, color = NVInk, fontSize = 12.sp, lineHeight = 18.sp)
        if (!item.photo.isNullOrBlank()) AsyncImage(item.photo, null, Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
        if (!item.audio.isNullOrBlank()) {
            NativeAnnouncementAudioPlayer(item.audio!!)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            NativeSmallAction((if (item.liked) c.t("Liked", "لائکڈ") else c.t("Like", "لائک")) + " " + item.likes, NVIcons.Heart) {
                if (c.user == null) c.route = V95Route.AUTH else c.likeAnnouncement(item)
            }
            NativeSmallAction(c.t("Comments", "کمنٹس") + " " + item.comments, NVIcons.Comment) { c.openAnnouncement(item) }
            if (canManage) {
                NativeSmallAction(c.t("Edit", "ایڈٹ"), NVIcons.Edit) {
                    editText = item.text
                    editOpen = true
                }
                NativeSmallAction(c.t("Delete", "حذف"), NVIcons.Delete) {
                    deleteConfirm = true
                }
            } else {
                NativeSmallAction(c.t("Report", "رپورٹ"), NVIcons.More) {
                    if (c.user == null) c.route = V95Route.AUTH else c.reportAnnouncement(item)
                }
            }
        }
    }

    if (editOpen) {
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text(c.t("Edit announcement", "اعلان ایڈٹ کریں"), fontWeight = FontWeight.Black) },
            text = {
                NVInput(editText, c.t("Announcement text", "اعلان کا متن"), Modifier.fillMaxWidth(), singleLine = false) {
                    editText = it
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { c.editAnnouncement(item, editText) { editOpen = false } },
                    enabled = editText.isNotBlank() || !item.photo.isNullOrBlank() || !item.audio.isNullOrBlank()
                ) { Text(c.t("Save", "محفوظ کریں"), fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { editOpen = false }) { Text(c.t("Cancel", "منسوخ")) }
            }
        )
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(c.t("Delete announcement?", "اعلان حذف کریں؟"), fontWeight = FontWeight.Black) },
            text = { Text(c.t("This announcement will be permanently deleted.", "یہ اعلان مستقل طور پر حذف ہو جائے گا۔")) },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirm = false
                    c.deleteAnnouncement(item)
                }) { Text(c.t("Delete", "حذف کریں"), color = NVDanger, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) { Text(c.t("Cancel", "منسوخ")) }
            }
        )
    }
}

@Composable
private fun NativeAnnouncementAudioPlayer(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
    )
}

@Composable
internal fun NativeAnnouncementDetail(c: V95Controller) {
    val item = c.selectedAnnouncement
    if (item == null) {
        NVEmpty(c.t("Announcement unavailable", "اعلان دستیاب نہیں"))
        return
    }
    var comment by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NativeAnnouncementCard(c, item) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NVInput(comment, c.t("Write a comment…", "کمنٹ لکھیں…"), Modifier.weight(1f)) { comment = it }
                NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 31.dp) {
                    if (comment.isNotBlank()) c.addAnnouncementComment(comment) { comment = "" }
                }
            }
        }
        items(c.announcementCommentsList, key = { it.id }) { NativeCommentCard(it) }
    }
}

@Composable
internal fun NativeNotifications(c: V95Controller) {
    if (c.user == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { c.loadNotifications() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { NVHeading(c.t("Notifications", "اطلاعات")) }
        if (c.notices.isEmpty() && !c.busy) item { NVEmpty(c.t("No notifications", "کوئی اطلاع نہیں")) }
        items(c.notices, key = { it.id }) { notice ->
            NVCard(radius = 22.dp, padding = 11.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    notice.actor?.let { NVAvatar(it, 40.dp) }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(notice.text, color = NVInk, fontWeight = if (notice.read) FontWeight.SemiBold else FontWeight.Black, fontSize = 11.5.sp)
                        Text(notice.createdAt, color = NVMuted, fontSize = 9.sp)
                    }
                    if (!notice.read) Box(Modifier.size(9.dp).clip(CircleShape).background(NVPink))
                }
            }
        }
    }
}

@Composable
internal fun NativeProfile(c: V95Controller) {
    val person = c.selectedUser ?: c.user
    if (person == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(person.id) { c.loadProfile(person, false) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            NVCard(radius = 28.dp, padding = 0.dp) {
                Box(
                    Modifier.fillMaxWidth().height(145.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))
                ) {
                    if (!person.cover.isNullOrBlank()) AsyncImage(person.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-44).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NVAvatar(person, 90.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        if (person.verified) {
                            Spacer(Modifier.width(4.dp))
                            Image(painterResource(NVIcons.Check), null, Modifier.size(19.dp))
                        }
                    }
                    Text("@" + person.username, color = NVMuted, fontSize = 11.sp)
                    if (person.bio.isNotBlank()) Text(person.bio, color = NVInk, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(7.dp))
                    val mine = c.user?.id == person.id
                    if (mine) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            NVButton(c.t("Edit profile", "پروفائل ایڈٹ"), Modifier.weight(1f), primary = true, icon = NVIcons.Edit) { c.route = V95Route.SETTINGS }
                            NVButton(c.t("Settings", "ترتیبات"), Modifier.weight(1f), icon = NVIcons.Gear) { c.route = V95Route.SETTINGS }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            NVButton(
                                if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                                Modifier.weight(1f),
                                primary = !person.followed,
                                icon = NVIcons.Follow
                            ) {
                                if (c.user == null) c.route = V95Route.AUTH else c.follow(person)
                            }
                            NVButton(c.t("Message", "پیغام"), Modifier.weight(1f), icon = NVIcons.Message) {
                                if (c.user == null) c.route = V95Route.AUTH else c.openChat(person)
                            }
                        }
                    }
                }
            }
        }
        item {
            val details = listOf(
                NVIcons.Village to person.village,
                NVIcons.Mohalla to person.area,
                NVIcons.City to person.city,
                NVIcons.Hometown to person.hometown,
                NVIcons.Gender to person.gender,
                NVIcons.Heart to person.relationshipStatus,
                NVIcons.Work to person.work,
                NVIcons.School to person.school,
                NVIcons.Phone to if (person.showPhone) person.phone else "",
                NVIcons.Mail to if (person.showEmail) person.email else ""
            ).filter { it.second.isNotBlank() }

            if (details.isNotEmpty()) {
                NVCard(radius = 22.dp, padding = 12.dp) {
                    details.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { (icon, value) ->
                                Row(
                                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.54f)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(painterResource(icon), null, Modifier.size(25.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(value, color = NVInk, fontSize = 9.5.sp, maxLines = 2)
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            val socials = listOf(
                Triple(NVIcons.Facebook, "Facebook", "facebook" to person.socialFacebook),
                Triple(NVIcons.Instagram, "Instagram", "instagram" to person.socialInstagram),
                Triple(NVIcons.Youtube, "YouTube", "youtube" to person.socialYoutube),
                Triple(NVIcons.Website, c.t("Website", "ویب سائٹ"), "website" to person.socialWebsite)
            ).filter { it.third.second.isNotBlank() }
            if (socials.isNotEmpty()) {
                NVCard(radius = 22.dp, padding = 10.dp) {
                    socials.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { (icon, label, target) ->
                                NVButton(label, Modifier.weight(1f), icon = icon) {
                                    c.openSocial(target.first, target.second)
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NVButton(c.t("Followers", "فالوورز"), Modifier.weight(1f)) { c.openRelations(person, "followers") }
                NVButton(c.t("Following", "فالوونگ"), Modifier.weight(1f)) { c.openRelations(person, "following") }
            }
        }
        item {
            NVHeading(c.t("Posts", "پوسٹس"), c.t("Latest profile posts", "تازہ پروفائل پوسٹس"))
        }
        if (c.profilePosts.isEmpty() && !c.busy) {
            item { NVEmpty(c.t("No posts yet", "ابھی کوئی پوسٹ نہیں")) }
        }
        items(c.profilePosts, key = { "profile-" + it.id }) { post ->
            NativePostCard(c, post)
        }
    }
}

@Composable
internal fun NativeShopDetail(c: V95Controller) {
    val shop = c.selectedShop
    if (shop == null) {
        NVEmpty(c.t("Shop unavailable", "دکان دستیاب نہیں"))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            NVCard(radius = 28.dp, padding = 0.dp) {
                Box(
                    Modifier.fillMaxWidth().height(145.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))
                ) {
                    if (!shop.cover.isNullOrBlank()) AsyncImage(shop.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-50).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NVShopAvatar(shop, 90.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(shop.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(shop.category, color = NVMuted, fontSize = 11.sp)
                    if (shop.description.isNotBlank()) Text(shop.description, color = NVInk, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (c.user?.id == shop.userId || c.user?.isAdmin == true) {
                            NVButton(c.t("Manage", "مینیج"), Modifier.weight(1f), primary = true, icon = NVIcons.Edit) {
                                c.route = V95Route.SHOPS
                            }
                        } else {
                            NVButton(if (shop.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), Modifier.weight(1f), primary = !shop.followed, icon = NVIcons.Follow) {
                                if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop)
                            }
                        }
                        NVButton(c.t("Navigate", "راستہ"), Modifier.weight(1f), icon = NVIcons.Map) { c.navigateToShop(shop) }
                    }
                }
            }
        }
        item {
            NVCard(radius = 22.dp, padding = 12.dp) {
                val details = listOf(
                    Triple(NVIcons.Village, c.t("Village", "گاؤں"), shop.village),
                    Triple(NVIcons.Mohalla, c.t("Area", "علاقہ"), shop.area),
                    Triple(NVIcons.City, c.t("City", "شہر"), shop.city),
                    Triple(NVIcons.Phone, c.t("Phone", "فون"), shop.phone),
                    Triple(NVIcons.Whatsapp, "WhatsApp", shop.whatsapp),
                    Triple(NVIcons.Address, c.t("Address", "پتہ"), shop.location),
                    Triple(NVIcons.User, c.t("Username", "یوزرنیم"), shop.username)
                ).filter { it.third.isNotBlank() }
                details.forEach { (icon, label, value) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(icon), null, Modifier.size(25.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = NVMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(value, color = NVInk, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (shop.phone.isNotBlank()) {
                    NVButton(c.t("Call shop", "دکان کو کال کریں"), Modifier.fillMaxWidth(), icon = NVIcons.Phone) { c.dialPhone(shop.phone) }
                }
                if (shop.whatsapp.isNotBlank()) {
                    NVButton("WhatsApp", Modifier.fillMaxWidth(), icon = NVIcons.Whatsapp) { c.openWhatsApp(shop.whatsapp) }
                }
                if (shop.locationUrl.isNotBlank() || shop.location.isNotBlank()) {
                    NVButton(c.t("Start navigation", "نیویگیشن شروع کریں"), Modifier.fillMaxWidth(), primary = true, icon = NVIcons.Map) { c.navigateToShop(shop) }
                }
                NVButton(c.t("Followers", "فالوورز"), Modifier.fillMaxWidth()) { c.openShopFollowers(shop) }
            }
        }
        if (c.user?.id == shop.userId || c.user?.isAdmin == true) {
            item { NativeShopPostComposer(c, shop) }
        }
        item {
            NVHeading(c.t("Shop posts", "دکان کی پوسٹس"), c.t("Latest posts from this shop", "اس دکان کی تازہ پوسٹس"))
        }
        if (c.selectedShopPosts.isEmpty() && !c.busy) {
            item { NVEmpty(c.t("No shop posts yet", "ابھی دکان کی کوئی پوسٹ نہیں")) }
        }
        items(c.selectedShopPosts, key = { "shop-" + it.id }) { post ->
            NativePostCard(c, post)
        }
    }
}

@Composable
private fun NativeShopPostComposer(c: V95Controller, shop: Shop) {
    var text by remember(shop.id) { mutableStateOf("") }
    var photo by remember(shop.id) { mutableStateOf("") }
    var video by remember(shop.id) { mutableStateOf("") }
    var privacy by remember(shop.id) { mutableStateOf("public") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "shop_post_photo") { photo = it }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "shop_post_video") { video = it }
    }

    NVCard(radius = 24.dp, padding = 12.dp) {
        NativeSettingsTitle(NVIcons.Plus, c.t("Create shop post", "دکان کی پوسٹ بنائیں"))
        NVInput(text, c.t("Write for your shop…", "اپنی دکان کے لیے لکھیں…"), Modifier.fillMaxWidth(), singleLine = false) { text = it }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NVButton(
                if (photo.isBlank()) c.t("Photo", "فوٹو") else c.t("Photo ✓", "فوٹو ✓"),
                Modifier.weight(1f),
                icon = NVIcons.Photo
            ) { photoPicker.launch("image/*") }
            NVButton(
                if (video.isBlank()) c.t("Video", "ویڈیو") else c.t("Video ✓", "ویڈیو ✓"),
                Modifier.weight(1f),
                icon = NVIcons.Video
            ) { videoPicker.launch("video/*") }
            NVButton(
                when (privacy) {
                    "followers" -> c.t("Followers", "فالوورز")
                    "private" -> c.t("Only me", "صرف میں")
                    else -> c.t("Public", "پبلک")
                },
                Modifier.weight(1f)
            ) {
                privacy = when (privacy) {
                    "public" -> "followers"
                    "followers" -> "private"
                    else -> "public"
                }
            }
        }

        if (photo.isNotBlank()) {
            AsyncImage(
                photo,
                null,
                Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }
        if (video.isNotBlank()) {
            Text(c.t("Video ready", "ویڈیو تیار ہے"), color = NVGreen, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }

        NVButton(
            c.t("Publish shop post", "دکان پوسٹ شائع کریں"),
            Modifier.fillMaxWidth(),
            primary = true,
            enabled = text.isNotBlank() || photo.isNotBlank() || video.isNotBlank()
        ) {
            c.createShopPost(shop, text, privacy, photo, video) {
                text = ""
                photo = ""
                video = ""
                privacy = "public"
            }
        }
    }
}

@Composable
internal fun NativeSaved(c: V95Controller) {
    if (c.user == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { if (c.saved.isEmpty()) c.loadSaved() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Saved", "محفوظ"), c.t("Posts saved to your profile", "آپ کی محفوظ پوسٹس")) }
        if (c.saved.isEmpty() && !c.busy) item { NVEmpty(c.t("Nothing saved yet", "ابھی کچھ محفوظ نہیں")) }
        items(c.saved, key = { it.id }) { NativePostCard(c, it) }
    }
}

@Composable
internal fun NativeRelations(c: V95Controller) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { NVHeading(c.relationTitle) }
        items(c.relationUsers, key = { it.id }) { person ->
            NVCard(radius = 20.dp, padding = 10.dp) {
                Row(Modifier.fillMaxWidth().clickable { c.openProfile(person) }, verticalAlignment = Alignment.CenterVertically) {
                    NVAvatar(person, 46.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                        Text("@" + person.username, color = NVMuted, fontSize = 9.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativePostDetail(c: V95Controller) {
    val post = c.selectedPost
    if (post == null) {
        NVEmpty(c.t("Post unavailable", "پوسٹ دستیاب نہیں"))
        return
    }
    var comment by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NativePostCard(c, post) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NVInput(comment, c.t("Write a comment…", "کمنٹ لکھیں…"), Modifier.weight(1f)) { comment = it }
                NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 31.dp) {
                    if (comment.isNotBlank()) c.addPostComment(comment) { comment = "" }
                }
            }
        }
        items(c.postCommentsList, key = { it.id }) { NativeCommentCard(it) }
    }
}

@Composable
private fun NativeCommentCard(item: Comment) {
    NVCard(radius = 18.dp, padding = 9.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NVAvatar(item.user, 34.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(item.user.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                Text(item.createdAt, color = NVMuted, fontSize = 8.sp)
            }
            if (item.likes > 0) Text(item.likes.toString(), color = NVMuted, fontSize = 8.sp)
        }
        Text(item.text, color = NVInk, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun NativeSmallAction(text: String, icon: Int, onClick: () -> Unit) {
    Column(Modifier.height(42.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(icon), null, Modifier.size(23.dp))
        Text(text, color = NVPurple, fontWeight = FontWeight.Black, fontSize = 8.2.sp, maxLines = 1)
    }
}

@Composable
private fun NativeRequireLogin(c: V95Controller) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        NVCard {
            Text(c.t("Login required", "لاگ اِن درکار ہے"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(c.t("Login to use this feature.", "یہ فیچر استعمال کرنے کے لیے لاگ اِن کریں۔"), color = NVMuted, fontSize = 11.sp)
            NVButton(c.t("Login / Register", "لاگ اِن / رجسٹریشن"), Modifier.fillMaxWidth(), primary = true) { c.route = V95Route.AUTH }
        }
    }
}

private fun nativeVoteRemaining(raw: String, nowMs: Long, c: V95Controller): String {
    if (raw.isBlank()) return c.t("Live", "لائیو")
    val end = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    val remaining = Duration.between(Instant.ofEpochMilli(nowMs), end)
    if (remaining.isNegative || remaining.isZero) return c.t("Ended", "ختم")
    val total = remaining.seconds
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
