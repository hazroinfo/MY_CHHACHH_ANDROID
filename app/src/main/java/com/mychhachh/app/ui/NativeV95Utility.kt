package com.mychhachh.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.mychhachh.app.data.CheckinPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs

@Composable
internal fun NativeSearch(c: V95Controller) {
    val bundle = c.searchBundle
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            NVHeading(
                c.t("Search", "تلاش"),
                if (c.searchText.isBlank()) c.t("Find people, posts and shops", "لوگ، پوسٹس اور دکانیں تلاش کریں") else "“" + c.searchText + "”"
            )
        }
        if (bundle == null) item { NVEmpty(c.t("Search from the header above", "اوپر ہیڈر سے تلاش کریں")) }
        bundle?.let { data ->
            if (data.users.isNotEmpty()) item { NativeSectionLabel(c.t("People", "لوگ")) }
            items(data.users, key = { it.id }) { person ->
                NVCard(radius = 20.dp, padding = 10.dp) {
                    Row(Modifier.fillMaxWidth().clickable { c.openProfile(person) }, verticalAlignment = Alignment.CenterVertically) {
                        NVAvatar(person, 44.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text("@" + person.username, color = NVMuted, fontSize = 9.5.sp)
                        }
                    }
                }
            }
            if (data.shops.isNotEmpty()) item { NativeSectionLabel(c.t("Shops", "دکانیں")) }
            items(data.shops, key = { it.id }) { shop ->
                NVCard(radius = 20.dp, padding = 10.dp) {
                    Row(Modifier.fillMaxWidth().clickable { c.openShop(shop) }, verticalAlignment = Alignment.CenterVertically) {
                        NVShopAvatar(shop, 44.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(shop.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(shop.category, color = NVMuted, fontSize = 9.5.sp)
                        }
                    }
                }
            }
            if (data.posts.isNotEmpty()) item { NativeSectionLabel(c.t("Posts", "پوسٹس")) }
            items(data.posts, key = { it.id }) { NativePostCard(c, it, compact = true) }
        }
    }
}

@Composable
internal fun NativeWeather(c: V95Controller) {
    LaunchedEffect(Unit) { c.loadWeather() }
    val w = c.weather
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Live Weather", "لائیو موسم"), c.t("Chhachh weather", "چھچھ کا موسم")) }
        item {
            NVCard {
                val current = w?.optJSONObject("current") ?: w
                val temp = current?.let { it.optString("temperature_2m", it.optString("temperature", "--")) } ?: "--"
                val condition = current?.let { it.optString("condition", it.optString("weather", c.t("Live weather", "لائیو موسم"))) } ?: c.t("Live weather", "لائیو موسم")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(NVIcons.Weather), null, Modifier.size(66.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(temp + "°", color = NVPurple, fontWeight = FontWeight.Black, fontSize = 38.sp)
                        Text(condition, color = NVInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
                Text(c.t("Weather updates from the live service.", "موسم براہِ راست لائیو سروس سے اپڈیٹ ہوتا ہے۔"), color = NVMuted, fontSize = 10.5.sp)
            }
        }
    }
}

@Composable
internal fun NativeSettings(c: V95Controller) {
    val user = c.user
    if (user == null) {
        NativeSettingsGuest(c)
        return
    }
    var name by remember(user.id, user.name) { mutableStateOf(user.name) }
    var username by remember(user.id, user.username) { mutableStateOf(user.username) }
    var phone by remember(user.id, user.phone) { mutableStateOf(user.phone) }
    var email by remember(user.id, user.email) { mutableStateOf(user.email) }
    var bio by remember(user.id, user.bio) { mutableStateOf(user.bio) }
    var avatar by remember(user.id, user.avatar) { mutableStateOf(user.avatar.orEmpty()) }
    var cover by remember(user.id, user.cover) { mutableStateOf(user.cover.orEmpty()) }
    var gender by remember(user.id, user.gender) { mutableStateOf(user.gender) }
    var relationship by remember(user.id, user.relationshipStatus) { mutableStateOf(user.relationshipStatus) }
    var work by remember(user.id, user.work) { mutableStateOf(user.work) }
    var school by remember(user.id, user.school) { mutableStateOf(user.school) }
    var city by remember(user.id, user.city) { mutableStateOf(user.city) }
    var hometown by remember(user.id, user.hometown) { mutableStateOf(user.hometown) }
    var village by remember(user.id, user.village) { mutableStateOf(user.village) }
    var area by remember(user.id, user.area) { mutableStateOf(user.area) }
    var facebook by remember(user.id, user.socialFacebook) { mutableStateOf(user.socialFacebook) }
    var instagram by remember(user.id, user.socialInstagram) { mutableStateOf(user.socialInstagram) }
    var youtube by remember(user.id, user.socialYoutube) { mutableStateOf(user.socialYoutube) }
    var website by remember(user.id, user.socialWebsite) { mutableStateOf(user.socialWebsite) }
    var showEmail by remember(user.id, user.showEmail) { mutableStateOf(user.showEmail) }
    var showPhone by remember(user.id, user.showPhone) { mutableStateOf(user.showPhone) }
    var showLocation by remember(user.id, user.showLocation) { mutableStateOf(user.showLocation) }
    var hideFollowers by remember(user.id, user.hideFollowers) { mutableStateOf(user.hideFollowers) }
    var acceptMessages by remember(user.id, user.acceptMessages) { mutableStateOf(user.acceptMessages) }
    var privateProfile by remember(user.id, user.profileVisibility) { mutableStateOf(user.profileVisibility == "private") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var verificationPhone by remember(user.id, user.phone) { mutableStateOf(user.phone) }
    var verificationType by remember { mutableStateOf("id_card") }
    var verificationFront by remember { mutableStateOf("") }
    var verificationBack by remember { mutableStateOf("") }
    var verificationSelfie by remember { mutableStateOf("") }
    var supportCategory by remember { mutableStateOf("problem") }
    var supportSubject by remember { mutableStateOf("") }
    var supportMessage by remember { mutableStateOf("") }
    var deleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "avatar") { avatar = it }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "profile-cover") { cover = it }
    }

    val verificationFrontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "verification_front") { verificationFront = it }
    }
    val verificationBackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "verification_back") { verificationBack = it }
    }
    val verificationSelfiePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "verification_selfie") { verificationSelfie = it }
    }

    LaunchedEffect(user.id) { c.loadAccountTools(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Settings", "ترتیبات"), c.t("Profile, privacy and language", "پروفائل، پرائیویسی اور زبان")) }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Message, c.t("Language", "زبان"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVButton("English", Modifier.weight(1f), primary = c.language == "en") { c.changeLanguage("en") }
                    NVButton("اردو", Modifier.weight(1f), primary = c.language == "ur") { c.changeLanguage("ur") }
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.User, c.t("Edit profile", "پروفائل ایڈٹ"))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVButton(
                        if (avatar.isBlank()) c.t("Profile photo", "پروفائل فوٹو") else c.t("Photo ✓", "فوٹو ✓"),
                        Modifier.weight(1f),
                        icon = NVIcons.Photo
                    ) { avatarPicker.launch("image/*") }
                    NVButton(
                        if (cover.isBlank()) c.t("Cover photo", "کور فوٹو") else c.t("Cover ✓", "کور ✓"),
                        Modifier.weight(1f),
                        icon = NVIcons.Photo
                    ) { coverPicker.launch("image/*") }
                }

                NVInput(name, c.t("Name", "نام")) { name = it }
                NVInput(username, c.t("Username", "یوزرنیم")) { username = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVInput(phone, c.t("Phone number", "فون نمبر"), Modifier.weight(1f)) { phone = it }
                    NVInput(email, c.t("Email", "ای میل"), Modifier.weight(1f)) { email = it }
                }
                NVInput(bio, c.t("Short bio", "مختصر بایو"), Modifier.fillMaxWidth(), singleLine = false) { bio = it }

                NativeSettingsTitle(NVIcons.Info, c.t("Personal details", "ذاتی تفصیل"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVButton(
                        when (gender) {
                            "male" -> c.t("Male", "مرد")
                            "female" -> c.t("Female", "خاتون")
                            "prefer_not_say" -> c.t("Prefer not to say", "نہیں بتانا")
                            else -> c.t("Gender", "جنس")
                        },
                        Modifier.weight(1f)
                    ) {
                        gender = when (gender) {
                            "" -> "male"
                            "male" -> "female"
                            "female" -> "prefer_not_say"
                            else -> ""
                        }
                    }
                    NVButton(
                        when (relationship) {
                            "single" -> c.t("Single", "سنگل")
                            "married" -> c.t("Married", "شادی شدہ")
                            "engaged" -> c.t("Engaged", "منگنی شدہ")
                            "prefer_not_say" -> c.t("Prefer not to say", "نہیں بتانا")
                            else -> c.t("Relationship", "رشتہ")
                        },
                        Modifier.weight(1f)
                    ) {
                        relationship = when (relationship) {
                            "" -> "single"
                            "single" -> "married"
                            "married" -> "engaged"
                            "engaged" -> "prefer_not_say"
                            else -> ""
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVInput(work, c.t("Work / profession", "کام / پیشہ"), Modifier.weight(1f)) { work = it }
                    NVInput(school, c.t("School / college", "سکول / کالج"), Modifier.weight(1f)) { school = it }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVInput(city, c.t("Current city", "موجودہ شہر"), Modifier.weight(1f)) { city = it }
                    NVInput(hometown, c.t("From / hometown", "آبائی جگہ"), Modifier.weight(1f)) { hometown = it }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    NVInput(village, c.t("Village", "گاؤں"), Modifier.weight(1f)) { village = it }
                    NVInput(area, c.t("Mohalla / area", "محلہ / علاقہ"), Modifier.weight(1f)) { area = it }
                }

                NativeSettingsTitle(NVIcons.Website, c.t("Social links", "سوشل لنکس"))
                NVInput(facebook, c.t("Facebook link", "Facebook لنک")) { facebook = it }
                NVInput(instagram, c.t("Instagram link", "Instagram لنک")) { instagram = it }
                NVInput(youtube, c.t("YouTube link", "YouTube لنک")) { youtube = it }
                NVInput(website, c.t("Website", "ویب سائٹ")) { website = it }

                NVButton(
                    c.t("Save profile", "پروفائل محفوظ کریں"),
                    Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = name.isNotBlank() && username.isNotBlank()
                ) {
                    c.updateProfileFields(
                        JSONObject()
                            .put("name", name.trim())
                            .put("username", username.trim())
                            .put("phone", phone.trim())
                            .put("email", email.trim())
                            .put("bio", bio.trim())
                            .put("avatar", avatar)
                            .put("cover_photo", cover)
                            .put("gender", gender)
                            .put("relationship_status", relationship)
                            .put("work", work.trim())
                            .put("school", school.trim())
                            .put("city", city.trim())
                            .put("hometown", hometown.trim())
                            .put("village", village.trim())
                            .put("area", area.trim())
                            .put("social_facebook", facebook.trim())
                            .put("social_instagram", instagram.trim())
                            .put("social_youtube", youtube.trim())
                            .put("social_website", website.trim())
                    )
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Shield, c.t("Privacy", "پرائیویسی"))
                NativeToggleRow(c.t("Private profile", "پرائیویٹ پروفائل"), privateProfile) { privateProfile = !privateProfile }
                NativeToggleRow(c.t("Show email", "ای میل دکھائیں"), showEmail) { showEmail = !showEmail }
                NativeToggleRow(c.t("Show phone", "نمبر دکھائیں"), showPhone) { showPhone = !showPhone }
                NativeToggleRow(c.t("Show location", "لوکیشن دکھائیں"), showLocation) { showLocation = !showLocation }
                NativeToggleRow(c.t("Hide followers", "فالوورز چھپائیں"), hideFollowers) { hideFollowers = !hideFollowers }
                NativeToggleRow(c.t("Accept messages", "پیغامات قبول کریں"), acceptMessages) { acceptMessages = !acceptMessages }
                NVButton(c.t("Save privacy", "پرائیویسی محفوظ کریں"), Modifier.fillMaxWidth(), primary = true) {
                    c.savePrivacy(
                        if (privateProfile) "private" else "public",
                        showEmail,
                        showPhone,
                        showLocation,
                        hideFollowers,
                        acceptMessages
                    )
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Shield, c.t("Password", "پاس ورڈ"))
                NVPasswordInput(currentPassword, c.t("Current password", "موجودہ پاس ورڈ")) { currentPassword = it }
                NVPasswordInput(newPassword, c.t("New password", "نیا پاس ورڈ")) { newPassword = it }
                NVButton(c.t("Change password", "پاس ورڈ تبدیل کریں"), Modifier.fillMaxWidth(), primary = true) {
                    if (currentPassword.isNotBlank() && newPassword.isNotBlank()) {
                        c.changePassword(currentPassword, newPassword) {
                            currentPassword = ""; newPassword = ""
                        }
                    }
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Shield, c.t("Verification", "ویریفکیشن"))
                val verification = c.verificationState?.optJSONObject("verification") ?: c.verificationState
                val status = verification?.optString("status", "unverified").orEmpty().ifBlank { "unverified" }
                Text(
                    c.t("Status: ", "حالت: ") + status,
                    color = if (status.equals("approved", true)) NVGreen else NVPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                if (!status.equals("approved", true)) {
                    NVInput(verificationPhone, c.t("Phone number", "فون نمبر")) { verificationPhone = it }
                    NVButton(
                        if (verificationType == "passport") c.t("Passport", "پاسپورٹ") else c.t("ID card", "شناختی کارڈ"),
                        Modifier.fillMaxWidth()
                    ) {
                        verificationType = if (verificationType == "id_card") "passport" else "id_card"
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        NVButton(
                            if (verificationFront.isBlank()) c.t("ID front", "کارڈ فرنٹ") else c.t("Front ✓", "فرنٹ ✓"),
                            Modifier.weight(1f),
                            icon = NVIcons.Photo
                        ) { verificationFrontPicker.launch("image/*") }
                        NVButton(
                            if (verificationBack.isBlank()) c.t("ID back", "کارڈ بیک") else c.t("Back ✓", "بیک ✓"),
                            Modifier.weight(1f),
                            icon = NVIcons.Photo
                        ) { verificationBackPicker.launch("image/*") }
                        NVButton(
                            if (verificationSelfie.isBlank()) c.t("Selfie", "سیلفی") else c.t("Selfie ✓", "سیلفی ✓"),
                            Modifier.weight(1f),
                            icon = NVIcons.User
                        ) { verificationSelfiePicker.launch("image/*") }
                    }
                    NVButton(
                        c.t("Submit verification", "ویریفکیشن جمع کریں"),
                        Modifier.fillMaxWidth(),
                        primary = true,
                        enabled = verificationPhone.isNotBlank() && verificationFront.isNotBlank() && verificationBack.isNotBlank() && verificationSelfie.isNotBlank()
                    ) {
                        c.submitVerification(
                            verificationPhone,
                            verificationType,
                            verificationFront,
                            verificationBack,
                            verificationSelfie
                        ) {
                            verificationFront = ""
                            verificationBack = ""
                            verificationSelfie = ""
                        }
                    }
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Comment, c.t("Support", "سپورٹ"))
                NVButton(
                    when (supportCategory) {
                        "safety" -> c.t("Safety", "سیفٹی")
                        "feedback" -> c.t("Feedback", "رائے")
                        else -> c.t("Problem", "مسئلہ")
                    },
                    Modifier.fillMaxWidth()
                ) {
                    supportCategory = when (supportCategory) {
                        "problem" -> "safety"
                        "safety" -> "feedback"
                        else -> "problem"
                    }
                }
                NVInput(supportSubject, c.t("Subject", "عنوان")) { supportSubject = it }
                NVInput(supportMessage, c.t("Describe the issue…", "مسئلہ لکھیں…"), Modifier.fillMaxWidth(), singleLine = false) { supportMessage = it }
                NVButton(
                    c.t("Send to My Chhachh Support", "My Chhachh سپورٹ کو بھیجیں"),
                    Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = supportSubject.isNotBlank() && supportMessage.isNotBlank()
                ) {
                    c.submitSupport(supportCategory, supportSubject, supportMessage) {
                        supportSubject = ""
                        supportMessage = ""
                    }
                }
                if (c.supportTickets.isNotEmpty()) {
                    Text(
                        c.t("Support history: ", "سپورٹ ہسٹری: ") + c.supportTickets.size,
                        color = NVMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    c.supportTickets.take(3).forEach { ticket ->
                        val reply = ticket.optString("admin_reply", "")
                        Text(
                            ticket.optString("subject", c.t("Support ticket", "سپورٹ ٹکٹ")) +
                                if (reply.isNotBlank()) c.t(" · Replied", " · جواب آ گیا") else "",
                            color = if (reply.isNotBlank()) NVGreen else NVInk,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Shield, c.t("Blocked users", "بلاک یوزرز"))
                NVButton(c.t("Load blocked users", "بلاک یوزرز دیکھیں"), Modifier.fillMaxWidth()) { c.loadBlockedUsers() }
                c.blockedUsersList.forEach { person ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        NVAvatar(person, 36.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(person.name, color = NVInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        NVButton(c.t("Unblock", "ان بلاک")) { c.toggleBlock(person) }
                    }
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Delete, c.t("Delete account", "اکاؤنٹ حذف کریں"))
                Text(
                    c.t("Permanently delete your account and sign out.", "اپنا اکاؤنٹ مستقل طور پر حذف کریں اور سائن آؤٹ ہو جائیں۔"),
                    color = NVMuted,
                    fontSize = 10.5.sp
                )
                NVButton(c.t("Delete account", "اکاؤنٹ حذف کریں"), Modifier.fillMaxWidth(), danger = true) {
                    deleteDialog = true
                }
            }
        }
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = {
                deleteDialog = false
                deletePassword = ""
            },
            title = { Text(c.t("Delete account?", "اکاؤنٹ حذف کریں؟"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.t("Enter your password to confirm permanent deletion.", "مستقل حذف کی تصدیق کے لیے پاس ورڈ درج کریں۔"))
                    NVPasswordInput(deletePassword, c.t("Password", "پاس ورڈ")) { deletePassword = it }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deletePassword.isNotBlank(),
                    onClick = {
                        val password = deletePassword
                        deleteDialog = false
                        deletePassword = ""
                        c.deleteAccount(password)
                    }
                ) { Text(c.t("Delete", "حذف کریں"), color = NVDanger, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteDialog = false
                    deletePassword = ""
                }) { Text(c.t("Cancel", "منسوخ")) }
            }
        )
    }
}

@Composable
private fun NativeSettingsGuest(c: V95Controller) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        NVCard {
            NativeSettingsTitle(NVIcons.Message, c.t("Language", "زبان"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NVButton("English", Modifier.weight(1f), primary = c.language == "en") { c.changeLanguage("en") }
                NVButton("اردو", Modifier.weight(1f), primary = c.language == "ur") { c.changeLanguage("ur") }
            }
            NVButton(c.t("Login / Register", "لاگ اِن / رجسٹریشن"), Modifier.fillMaxWidth(), primary = true) { c.route = V95Route.AUTH }
        }
    }
}

@Composable
internal fun NativeSettingsTitle(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(32.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = NVInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
private fun NativeToggleRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 42.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NVInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(width = 46.dp, height = 26.dp).clip(RoundedCornerShape(999.dp))
                .background(if (checked) nvPrimaryBrush() else Brush.linearGradient(listOf(Color(0xFFE2E8F2), Color(0xFFD9E1EC))))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.size(20.dp).clip(RoundedCornerShape(999.dp)).background(Color.White))
        }
    }
}

@Composable
internal fun NativeAuth(c: V95Controller) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    var verifyCode by remember { mutableStateOf("") }
    var resetMode by remember { mutableStateOf(false) }
    var resetCode by remember { mutableStateOf("") }
    var resetPassword by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        NVCard(Modifier.widthIn(max = 420.dp), radius = 26.dp, padding = 22.dp) {
            NVBrand(27f)
            Text(
                if (register) c.t("Create account", "اکاؤنٹ بنائیں") else c.t("Welcome back", "خوش آمدید"),
                color = NVInk, fontWeight = FontWeight.Black, fontSize = 19.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )

            c.authInfo?.let { Text(it, color = NVGreen, fontSize = 10.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }

            if (c.pendingVerifyUserId > 0L) {
                NVInput(verifyCode, c.t("Verification code", "ویریفکیشن کوڈ")) { verifyCode = it }
                NVButton(c.t("Verify email", "ای میل ویریفائی کریں"), Modifier.fillMaxWidth(), primary = true) {
                    if (verifyCode.isNotBlank()) c.verifyPendingEmail(verifyCode)
                }
                NVButton(c.t("Resend code", "کوڈ دوبارہ بھیجیں"), Modifier.fillMaxWidth()) { c.resendPendingEmail() }
            } else if (resetMode || c.passwordResetKey.isNotBlank()) {
                if (c.passwordResetKey.isBlank()) {
                    NVInput(email, c.t("Email", "ای میل")) { email = it }
                    NVButton(c.t("Send reset code", "ری سیٹ کوڈ بھیجیں"), Modifier.fillMaxWidth(), primary = true) {
                        if (email.isNotBlank()) c.startPasswordReset(email)
                    }
                } else {
                    NVInput(resetCode, c.t("Reset code", "ری سیٹ کوڈ")) { resetCode = it }
                    NVPasswordInput(resetPassword, c.t("New password", "نیا پاس ورڈ")) { resetPassword = it }
                    NVButton(c.t("Set new password", "نیا پاس ورڈ محفوظ کریں"), Modifier.fillMaxWidth(), primary = true) {
                        if (resetCode.isNotBlank() && resetPassword.isNotBlank()) {
                            c.finishPasswordReset(resetCode, resetPassword) {
                                resetMode = false
                                resetCode = ""
                                resetPassword = ""
                            }
                        }
                    }
                }
                NVButton(c.t("Back to login", "واپس لاگ اِن"), Modifier.fillMaxWidth()) { resetMode = false }
            } else {
                if (register) {
                    NVInput(name, c.t("Full name", "پورا نام")) { name = it }
                    NVInput(username, c.t("Username", "یوزرنیم")) { username = it }
                    NVInput(email, c.t("Email", "ای میل")) { email = it }
                } else {
                    NVInput(identity, c.t("Email or username", "ای میل یا یوزرنیم")) { identity = it }
                }
                NVPasswordInput(password, c.t("Password", "پاس ورڈ")) { password = it }

                if (register) {
                    Row(
                        Modifier.fillMaxWidth().clickable { terms = !terms },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                                .background(if (terms) nvPrimaryBrush() else nvInputBrush())
                                .border(1.5.dp, Color.White, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (terms) Text("✓", color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(c.t("I accept Terms & Conditions", "میں شرائط و ضوابط قبول کرتا ہوں"), color = NVInk, fontSize = 11.sp)
                    }
                }

                NVButton(
                    if (register) c.t("Register", "رجسٹر") else c.t("Login", "لاگ اِن"),
                    Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = password.isNotBlank() && (!register || terms)
                ) {
                    if (register) {
                        if (name.isNotBlank() && username.isNotBlank() && email.isNotBlank()) c.register(name, username, email, password)
                    } else if (identity.isNotBlank()) c.login(identity, password)
                }

                NVButton(
                    if (register) c.t("Already have an account? Login", "اکاؤنٹ ہے؟ لاگ اِن کریں") else c.t("Create new account", "نیا اکاؤنٹ بنائیں"),
                    Modifier.fillMaxWidth()
                ) {
                    register = !register
                    password = ""
                }

                if (!register) {
                    Text(
                        c.t("Forgot password?", "پاس ورڈ بھول گئے؟"),
                        color = NVPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.CenterHorizontally).clickable { resetMode = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun NVPasswordInput(value: String, placeholder: String, onValue: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(shape).background(nvInputBrush()).border(1.5.dp, Color.White, shape).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = NVInk, fontSize = 13.sp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            decorationBox = { inner ->
                if (value.isBlank()) Text(placeholder, color = Color(0xFF858EB1), fontSize = 12.sp)
                inner()
            }
        )
        Text(if (visible) "◉" else "◎", color = NVPurple, fontSize = 17.sp, modifier = Modifier.clickable { visible = !visible })
    }
}

@Composable
internal fun NativeMap(c: V95Controller) {
    var selected by remember { mutableStateOf<CheckinPlace?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        NVHeading(
            c.t("Chhachh Map", "چھچھ نقشہ"),
            when {
                c.mapPickForPost -> c.t("Pick an exact post check-in", "پوسٹ کے لیے درست چیک اِن منتخب کریں")
                c.mapPickForMessage -> c.t("Pick an exact message location", "پیغام کے لیے درست لوکیشن منتخب کریں")
                c.mapPickForGroupMessage -> c.t("Pick an exact group location", "گروپ پیغام کے لیے درست لوکیشن منتخب کریں")
                else -> c.t("Native map, search and exact location picker", "نیٹو نقشہ، تلاش اور درست لوکیشن پکر")
            }
        )

        NativeLocationPicker(
            c = c,
            modifier = Modifier.weight(1f),
            initialQuery = "",
            onSelected = { selected = it }
        )

        selected?.let { place ->
            Spacer(Modifier.height(7.dp))
            NVCard(radius = 20.dp, padding = 9.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(NVIcons.Pin), null, Modifier.size(28.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(place.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 10.5.sp, maxLines = 2)
                        Text(
                            String.format(java.util.Locale.US, "%.6f, %.6f", place.lat, place.lng),
                            color = NVMuted,
                            fontSize = 9.sp
                        )
                    }
                }
                when {
                    c.mapPickForPost -> NVButton(c.t("Use this check-in", "یہ چیک اِن استعمال کریں"), Modifier.fillMaxWidth(), primary = true) {
                        c.setPostCheckin(place)
                    }
                    c.mapPickForMessage -> NVButton(c.t("Send this location", "یہ لوکیشن بھیجیں"), Modifier.fillMaxWidth(), primary = true) {
                        c.setMessageLocation(place)
                    }
                    c.mapPickForGroupMessage -> NVButton(c.t("Send to group", "گروپ میں بھیجیں"), Modifier.fillMaxWidth(), primary = true) {
                        c.setGroupMessageLocation(place)
                    }
                    else -> NVButton(c.t("Open navigation", "نیویگیشن کھولیں"), Modifier.fillMaxWidth(), icon = NVIcons.Map) {
                        c.openCoordinates(place.lat, place.lng)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeLocationPicker(
    c: V95Controller,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    onSelected: (CheckinPlace) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<CheckinPlace>>(emptyList()) }
    var selected by remember { mutableStateOf<CheckinPlace?>(null) }
    var searching by remember { mutableStateOf(false) }

    fun selectPoint(place: CheckinPlace) {
        selected = place
        onSelected(place)
    }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NVInput(query, c.t("Search village, place or shop…", "گاؤں، جگہ یا دکان تلاش کریں…"), Modifier.weight(1f)) { query = it }
            NVButton(c.t("Find", "تلاش"), primary = true, icon = NVIcons.Search, enabled = query.isNotBlank() && !searching) {
                searching = true
                scope.launch {
                    results = runCatching {
                        withContext(Dispatchers.IO) { c.api.geocodePlaces(query) }
                    }.getOrDefault(emptyList())
                    searching = false
                    results.firstOrNull()?.let { selectPoint(it) }
                }
            }
        }

        Spacer(Modifier.height(7.dp))
        NVCard(Modifier.weight(1f).heightIn(min = 190.dp), radius = 22.dp, padding = 5.dp) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(33.90977, 72.48868))

                        overlays.add(
                            MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                                    val initial = CheckinPlace(
                                        String.format(java.util.Locale.US, "%.6f, %.6f", point.latitude, point.longitude),
                                        point.latitude,
                                        point.longitude
                                    )
                                    selectPoint(initial)
                                    scope.launch {
                                        val named = runCatching {
                                            withContext(Dispatchers.IO) { c.api.reverse(point.latitude, point.longitude) }
                                        }.getOrNull()?.let { data ->
                                            val label = data.optString(
                                                "display_name",
                                                data.optString("name", data.optString("address", initial.name))
                                            ).ifBlank { initial.name }
                                            CheckinPlace(label, point.latitude, point.longitude)
                                        }
                                        if (named != null) selectPoint(named)
                                    }
                                    return true
                                }

                                override fun longPressHelper(point: GeoPoint): Boolean {
                                    return singleTapConfirmedHelper(point)
                                }
                            })
                        )
                    }
                },
                update = { map ->
                    map.overlays.removeAll { it is Marker && it.id == "mychhachh-selected" }
                    selected?.let { place ->
                        val marker = Marker(map).apply {
                            id = "mychhachh-selected"
                            position = GeoPoint(place.lat, place.lng)
                            title = place.name
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(marker)
                        map.controller.animateTo(marker.position)
                    }
                    map.invalidate()
                }
            )
        }

        if (results.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            NVCard(radius = 18.dp, padding = 7.dp) {
                results.take(5).forEach { place ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable {
                                query = place.name
                                selectPoint(place)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(NVIcons.Pin), null, Modifier.size(23.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(place.name, color = NVInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeThemeBuilder(c: V95Controller) {
    if (c.user?.isAdmin != true) {
        NVEmpty(c.t("Admin access required", "ایڈمن رسائی درکار ہے"))
        return
    }

    val key = c.features.toString()
    var siteName by remember(key) { mutableStateOf(c.features.optString("site_name", "My Chhachh")) }
    var tagline by remember(key) { mutableStateOf(c.features.optString("site_tagline", "People • Places • Good Vibes")) }
    var logo by remember(key) { mutableStateOf(c.features.optString("site_icon", "")) }
    var logoBlend by remember(key) { mutableStateOf(c.features.optInt("theme_logo_blend", 0) != 0) }
    var guestLogin by remember(key) { mutableStateOf(c.features.optInt("theme_guest_login_button", 1) != 0) }
    var guestRegister by remember(key) { mutableStateOf(c.features.optInt("theme_guest_register_button", 1) != 0) }

    val headerAllowed = listOf("home", "people", "shop", "map", "messages")
    val menuAllowed = listOf("votes", "saved", "settings", "theme", "admin", "logout")
    fun parseOrder(raw: String, allowed: List<String>, fallback: List<String>): List<String> {
        val parsed = raw.split(",")
            .map { it.trim().lowercase() }
            .filter { it in allowed }
            .distinct()
        return if (parsed.isEmpty()) fallback else parsed
    }

    var headerOrder by remember(key) {
        mutableStateOf(
            parseOrder(
                c.features.optString("theme_header_items", "home,people,shop,map,messages"),
                headerAllowed,
                headerAllowed
            )
        )
    }
    var menuOrder by remember(key) {
        mutableStateOf(
            parseOrder(
                c.features.optString("theme_menu_items", "votes,saved,settings,theme,admin,logout"),
                menuAllowed,
                menuAllowed
            )
        )
    }

    val headerLabels = mapOf(
        "home" to c.t("Home", "ہوم"),
        "people" to c.t("People", "لوگ"),
        "shop" to c.t("Shop", "دکانیں"),
        "map" to c.t("Map", "نقشہ"),
        "messages" to c.t("Messages", "پیغامات")
    )
    val menuLabels = mapOf(
        "votes" to c.t("Voting", "ووٹنگ"),
        "saved" to c.t("Saved", "محفوظ"),
        "announcements" to c.t("Announcements", "اعلانات"),
        "notifications" to c.t("Notifications", "اطلاعات"),
        "settings" to c.t("Settings", "ترتیبات"),
        "theme" to c.t("Theme Builder", "تھیم بلڈر"),
        "admin" to c.t("Admin Center", "ایڈمن سینٹر"),
        "logout" to c.t("Logout", "لاگ آؤٹ")
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Theme Builder", "تھیم بلڈر"), c.t("Brand, menu and layout controls", "برانڈ، مینیو اور لے آؤٹ کنٹرول")) }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Palette, c.t("Brand", "برانڈ"))
                NVInput(siteName, c.t("Website name", "ویب سائٹ نام")) { siteName = it }
                NVInput(tagline, c.t("Tagline", "ٹیگ لائن")) { tagline = it }
                NVInput(logo, c.t("Logo URL", "لوگو لنک")) { logo = it }
                NativeToggleRow(c.t("Blend logo into header", "لوگو کو ہیڈر میں بلینڈ کریں"), logoBlend) { logoBlend = !logoBlend }
                if (logo.isNotBlank()) {
                    AsyncImage(
                        model = logo,
                        contentDescription = siteName,
                        modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                NVButton(c.t("Save brand", "برانڈ محفوظ کریں"), Modifier.fillMaxWidth(), primary = true) {
                    c.saveBrandFields(
                        JSONObject()
                            .put("site_name", siteName)
                            .put("site_tagline", tagline)
                            .put("site_icon", logo)
                            .put("theme_logo_blend", if (logoBlend) 1 else 0)
                    )
                }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Menu, c.t("Guest header buttons", "گیسٹ ہیڈر بٹن"))
                NativeToggleRow(c.t("Guest Login button", "گیسٹ لاگ اِن بٹن"), guestLogin) { guestLogin = !guestLogin }
                NativeToggleRow(c.t("Guest Register button", "گیسٹ رجسٹر بٹن"), guestRegister) { guestRegister = !guestRegister }
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Home, c.t("Bottom navigation", "نیچے نیویگیشن"))
                Text(
                    c.t("Long-press and drag, or use arrows. Hide/show any item.", "لانگ پریس کر کے ڈریگ کریں، یا تیر استعمال کریں۔ ہر آئٹم hide/show ہو سکتا ہے۔"),
                    color = NVMuted,
                    fontSize = 10.sp
                )
                NativeThemeOrderEditor(
                    active = headerOrder,
                    allowed = headerAllowed,
                    labels = headerLabels,
                    c = c,
                    onChange = { headerOrder = it }
                )
            }
        }

        item {
            NVCard {
                NativeSettingsTitle(NVIcons.Menu, c.t("Side menu", "سائیڈ مینیو"))
                Text(
                    c.t("Long-press and drag, or use arrows. Hidden items can be restored below.", "لانگ پریس کر کے ڈریگ کریں، یا تیر استعمال کریں۔ hidden آئٹمز نیچے سے واپس لائیں۔"),
                    color = NVMuted,
                    fontSize = 10.sp
                )
                NativeThemeOrderEditor(
                    active = menuOrder,
                    allowed = menuAllowed,
                    labels = menuLabels,
                    c = c,
                    onChange = { menuOrder = it }
                )
            }
        }

        item {
            NVCard {
                NVButton(c.t("Save layout", "لے آؤٹ محفوظ کریں"), Modifier.fillMaxWidth(), primary = true) {
                    c.saveThemeFields(
                        JSONObject()
                            .put("theme_guest_login_button", if (guestLogin) 1 else 0)
                            .put("theme_guest_register_button", if (guestRegister) 1 else 0)
                            .put("theme_header_items", headerOrder.joinToString(","))
                            .put("theme_menu_items", menuOrder.joinToString(","))
                            .put("theme_logo_blend", if (logoBlend) 1 else 0)
                    )
                }
            }
        }
    }
}

@Composable
private fun NativeThemeOrderEditor(
    active: List<String>,
    allowed: List<String>,
    labels: Map<String, String>,
    c: V95Controller,
    onChange: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        active.forEachIndexed { index, item ->
            var dragDistance by remember(item, active) { mutableFloatStateOf(0f) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(nvInputBrush())
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                    .pointerInput(item, active) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragDistance = 0f },
                            onDragEnd = { dragDistance = 0f },
                            onDragCancel = { dragDistance = 0f },
                            onDrag = { _, amount ->
                                dragDistance += amount.y
                                if (abs(dragDistance) > 34f) {
                                    val target = if (dragDistance > 0f) index + 1 else index - 1
                                    if (target in active.indices) {
                                        val changed = active.toMutableList()
                                        val moved = changed.removeAt(index)
                                        changed.add(target, moved)
                                        onChange(changed)
                                    }
                                    dragDistance = 0f
                                }
                            }
                        )
                    }
                    .padding(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("≡", color = NVPurple, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(labels[item] ?: item, color = NVInk, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                NVButton("↑", enabled = index > 0) {
                    if (index > 0) {
                        val changed = active.toMutableList()
                        val moved = changed.removeAt(index)
                        changed.add(index - 1, moved)
                        onChange(changed)
                    }
                }
                NVButton("↓", enabled = index < active.lastIndex) {
                    if (index < active.lastIndex) {
                        val changed = active.toMutableList()
                        val moved = changed.removeAt(index)
                        changed.add(index + 1, moved)
                        onChange(changed)
                    }
                }
                NVButton(c.t("Hide", "چھپائیں"), danger = true) {
                    onChange(active.filterNot { it == item })
                }
            }
        }

        val hidden = allowed.filterNot { it in active }
        if (hidden.isNotEmpty()) {
            Text(c.t("Hidden", "چھپے ہوئے"), color = NVMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            hidden.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { item ->
                        NVButton(
                            c.t("Show ", "دکھائیں ") + (labels[item] ?: item),
                            Modifier.weight(1f),
                            icon = NVIcons.Plus
                        ) { onChange(active + item) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun NativeAdmin(c: V95Controller) {
    if (c.user?.isAdmin != true) {
        NVEmpty(c.t("Admin access required", "ایڈمن رسائی درکار ہے"))
        return
    }
    LaunchedEffect(Unit) { if (c.adminState == null) c.loadAdmin() }

    val sections = listOf(
        "overview" to c.t("Overview", "خلاصہ"),
        "users" to c.t("Users", "یوزرز"),
        "verifications" to c.t("Verification", "ویریفکیشن"),
        "posts" to c.t("Posts", "پوسٹس"),
        "shops" to c.t("Shops", "دکانیں"),
        "votes" to c.t("Votes", "ووٹنگ"),
        "reports" to c.t("Reports", "رپورٹس"),
        "activity" to c.t("Activity", "ایکٹیویٹی"),
        "announcements" to c.t("Announcements", "اعلانات")
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Admin Center", "ایڈمن سینٹر"), c.t("Live site management", "لائیو سائٹ مینجمنٹ")) }
        item {
            NVCard(radius = 22.dp, padding = 9.dp) {
                sections.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { (key, label) ->
                            NVButton(label, Modifier.weight(1f), primary = c.adminSection == key) {
                                if (key == "overview") {
                                    c.adminSection = "overview"
                                    c.adminSectionData = c.adminState
                                } else c.openAdminSection(key)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        if (c.adminSection == "overview") {
            item { NativeAdminOverview(c) }
        } else {
            val data = c.adminSectionData
            val array = data?.optJSONArray("items") ?: JSONArray()
            if (array.length() == 0 && !c.busy) item { NVEmpty(c.t("No records", "کوئی ریکارڈ نہیں")) }
            items(array.length()) { index ->
                val item = array.optJSONObject(index) ?: JSONObject()
                NativeAdminRecord(c, c.adminSection, item)
            }
        }
    }
}

@Composable
private fun NativeAdminOverview(c: V95Controller) {
    val root = c.adminState ?: JSONObject()
    val stats = root.optJSONObject("stats") ?: root
    val metrics = listOf(
        c.t("Users", "یوزرز") to stats.optInt("total_users", 0),
        c.t("Online", "آن لائن") to stats.optInt("online_users", 0),
        c.t("Shops", "دکانیں") to stats.optInt("total_shops", 0),
        c.t("Posts", "پوسٹس") to stats.optInt("total_posts", 0),
        c.t("Votes", "ووٹنگ") to stats.optInt("total_votes", 0),
        c.t("Pending", "زیرِ التوا") to stats.optInt("pending_verifications", 0),
        c.t("Reports", "رپورٹس") to stats.optInt("open_reports", 0),
        c.t("Alerts", "الرٹس") to stats.optInt("admin_alerts", 0)
    )
    NVCard {
        metrics.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(.58f)).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(value.toString(), color = NVPurple, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(label, color = NVMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeAdminRecord(c: V95Controller, section: String, item: JSONObject) {
    val id = item.optLong("id", item.optLong("user_id", 0L))
    NVCard(radius = 22.dp, padding = 11.dp) {
        when (section) {
            "users" -> {
                var warning by remember(id) { mutableStateOf("") }
                var asUserPost by remember(id) { mutableStateOf("") }
                val blocked = item.optBoolean("blocked", false)
                val promoted = item.optBoolean("promoted", false)

                Text(item.optString("name", "User"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("@" + item.optString("username", "") + " · " + item.optString("email", item.optString("phone", "")), color = NVMuted, fontSize = 9.5.sp)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    NVButton(
                        if (blocked) c.t("Unblock", "ان بلاک") else c.t("Block", "بلاک"),
                        Modifier.weight(1f),
                        danger = !blocked
                    ) { c.runAdminAction("toggle_user", id) }
                    NVButton(c.t("Blue tick", "بلیو ٹک"), Modifier.weight(1f)) {
                        c.runAdminAction("user_setting", id, JSONObject().put("key", "verified"))
                    }
                    NVButton(
                        if (promoted) c.t("Stop promotion", "پروموشن بند") else c.t("Promote", "پروموٹ"),
                        Modifier.weight(1f)
                    ) { c.runAdminAction("promote_user", id) }
                }

                listOf(
                    "allow_posts" to c.t("Posts", "پوسٹس"),
                    "allow_photo_upload" to c.t("Photo", "فوٹو"),
                    "allow_video_upload" to c.t("Video", "ویڈیو"),
                    "allow_comments" to c.t("Comments", "کمنٹس"),
                    "allow_likes" to c.t("Likes", "لائکس"),
                    "allow_follows" to c.t("Followers", "فالوورز"),
                    "allow_messages" to c.t("Messages", "پیغامات")
                ).chunked(3).forEach { controls ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        controls.forEach { (key, label) ->
                            NVButton(label, Modifier.weight(1f)) {
                                c.runAdminAction("user_setting", id, JSONObject().put("key", key))
                            }
                        }
                        repeat(3 - controls.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                NVInput(warning, c.t("Write warning…", "وارننگ لکھیں…"), Modifier.fillMaxWidth(), singleLine = false) { warning = it }
                NVButton(
                    c.t("Send warning", "وارننگ بھیجیں"),
                    Modifier.fillMaxWidth(),
                    enabled = warning.isNotBlank()
                ) {
                    c.runAdminAction("warning", id, JSONObject().put("message", warning.trim()))
                    warning = ""
                }

                NVInput(asUserPost, c.t("Publish a post as this user…", "اس یوزر کے نام سے پوسٹ لکھیں…"), Modifier.fillMaxWidth(), singleLine = false) { asUserPost = it }
                NVButton(
                    c.t("Publish as user", "یوزر کے نام سے شائع کریں"),
                    Modifier.fillMaxWidth(),
                    primary = true,
                    enabled = asUserPost.isNotBlank()
                ) {
                    c.runAdminAction("admin_user_post", id, JSONObject().put("text", asUserPost.trim()))
                    asUserPost = ""
                }

                NVButton(c.t("Delete user account", "یوزر اکاؤنٹ حذف کریں"), Modifier.fillMaxWidth(), danger = true, icon = NVIcons.Delete) {
                    c.runAdminAction("delete_user", id)
                }
            }
            "verifications" -> {
                val user = item.optJSONObject("user")
                Text(user?.optString("name", "User") ?: "User", color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(item.optString("status", "pending") + " · " + item.optString("phone", ""), color = NVMuted, fontSize = 9.5.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NVButton(c.t("Approve", "منظور"), Modifier.weight(1f), primary = true) {
                        c.runAdminAction("verification_review", id, JSONObject().put("decision", "approved").put("admin_note", "Approved in Android Admin Center"))
                    }
                    NVButton(c.t("Reject", "مسترد"), Modifier.weight(1f), danger = true) {
                        c.runAdminAction("verification_review", id, JSONObject().put("decision", "rejected").put("admin_note", "Rejected in Android Admin Center"))
                    }
                }
            }
            "posts" -> {
                Text(item.optString("text", "").take(320), color = NVInk, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NVButton(c.t("Promote", "پروموٹ"), Modifier.weight(1f)) { c.runAdminAction("promote_post", id) }
                    NVButton(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) { c.runAdminAction("delete_post", id) }
                }
            }
            "shops" -> {
                Text(item.optString("name", c.t("Shop", "دکان")), color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(item.optString("category", ""), color = NVMuted, fontSize = 9.5.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    NVButton(c.t("Promote", "پروموٹ"), Modifier.weight(1f)) { c.runAdminAction("promote_shop", id) }
                    NVButton(c.t("On/Off", "آن/آف"), Modifier.weight(1f)) { c.runAdminAction("toggle_shop", id) }
                    NVButton(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) { c.runAdminAction("delete_shop", id) }
                }
            }
            "votes" -> {
                Text(item.optString("title", c.t("Voting", "ووٹنگ")), color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(item.optInt("left_votes", item.optInt("votes1", 0)).toString() + " VS " + item.optInt("right_votes", item.optInt("votes2", 0)).toString(), color = NVPurple, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NVButton("L +1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "left").put("delta", 1)) }
                    NVButton("L -1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "left").put("delta", -1)) }
                    NVButton("R +1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "right").put("delta", 1)) }
                    NVButton("R -1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "right").put("delta", -1)) }
                }
            }
            "reports" -> {
                Text(item.optString("reason", item.optString("message", "")).take(420), color = NVInk, fontSize = 11.sp)
                Text(item.optString("status", "open"), color = NVMuted, fontSize = 9.5.sp)
            }
            "activity" -> {
                Text(item.optString("action", "activity"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                Text("ID " + item.optLong("actor_id").toString() + " · " + item.optString("created_at", ""), color = NVMuted, fontSize = 9.sp)
                if (item.optString("ip", "").isNotBlank()) Text(item.optString("ip"), color = NVMuted, fontSize = 9.sp)
            }
            else -> {
                Text(item.optString("text", item.optString("name", item.toString())).take(420), color = NVInk, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun NativeSectionLabel(text: String) {
    Text(text, color = NVInk, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
}
