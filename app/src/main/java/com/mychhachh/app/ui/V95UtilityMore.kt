package com.mychhachh.app.ui

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale



@Composable
internal fun V95Search(c: V95Controller) {
    val b = c.searchBundle
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { V95PageHeading(c.t("Search", "تلاش"), if (c.searchText.isBlank()) c.t("Find people, posts and shops", "لوگ، پوسٹس اور دکانیں تلاش کریں") else "\"${c.searchText}\"") }
        if (b == null) item { V95Empty(c.t("Search from the header above", "اوپر ہیڈر سے تلاش کریں")) }
        b?.let { data ->
            if (data.users.isNotEmpty()) item { Text(c.t("People", "لوگ"), fontWeight = FontWeight.Black, color = V95Ink, fontSize = 17.sp) }
            items(data.users) { p -> V95GlassCard(radius = 20.dp, padding = 10.dp) { Row(Modifier.clickable { c.openProfile(p) }, verticalAlignment = Alignment.CenterVertically) { V95Avatar(p, 46.dp); Spacer(Modifier.width(9.dp)); Text(p.name, color = V95Ink, fontWeight = FontWeight.Black) } } }
            if (data.shops.isNotEmpty()) item { Text(c.t("Shops", "دکانیں"), fontWeight = FontWeight.Black, color = V95Ink, fontSize = 17.sp) }
            items(data.shops) { s -> V95GlassCard(radius = 20.dp, padding = 10.dp) { Row(Modifier.clickable { c.openShop(s) }, verticalAlignment = Alignment.CenterVertically) { V95ShopAvatar(s, 46.dp); Spacer(Modifier.width(9.dp)); Text(s.name, color = V95Ink, fontWeight = FontWeight.Black) } } }
            if (data.posts.isNotEmpty()) item { Text(c.t("Posts", "پوسٹس"), fontWeight = FontWeight.Black, color = V95Ink, fontSize = 17.sp) }
            items(data.posts) { V95PostCard(c, it) }
        }
    }
}

@Composable
internal fun V95Weather(c: V95Controller) {
    LaunchedEffect(Unit) { c.loadWeather() }
    val w = c.weather
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { V95PageHeading(c.t("Live Weather", "لائیو موسم"), c.t("Chhachh weather", "چھچھ کا موسم")) }
        item {
            V95GlassCard {
                val current = w?.optJSONObject("current") ?: w
                val temp = current?.let { it.optString("temperature_2m", it.optString("temperature", "--")) } ?: "--"
                Text("$temp°", color = V95Purple, fontWeight = FontWeight.Black, fontSize = 44.sp)
                val condition = current?.let { it.optString("condition", it.optString("weather", c.t("Live weather", "لائیو موسم"))) } ?: c.t("Live weather", "لائیو موسم")
                Text(condition, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(c.t("Weather updates directly from the live service.", "موسم براہِ راست لائیو سروس سے اپڈیٹ ہوتا ہے۔"), color = V95Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun V95Settings(c: V95Controller) {
    val user = c.user
    var name by remember(user?.id) { mutableStateOf(user?.name.orEmpty()) }
    var username by remember(user?.id) { mutableStateOf(user?.username.orEmpty()) }
    var bio by remember(user?.id) { mutableStateOf(user?.bio.orEmpty()) }

    var profilePrivate by remember(user?.id) { mutableStateOf(user?.profileVisibility == "private") }
    var showEmail by remember(user?.id) { mutableStateOf(user?.showEmail ?: false) }
    var showPhone by remember(user?.id) { mutableStateOf(user?.showPhone ?: false) }
    var showLocation by remember(user?.id) { mutableStateOf(user?.showLocation ?: false) }
    var hideFollowers by remember(user?.id) { mutableStateOf(user?.hideFollowers ?: false) }
    var acceptMessages by remember(user?.id) { mutableStateOf(user?.acceptMessages ?: true) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { V95PageHeading(c.t("Settings", "ترتیبات"), c.t("Profile, privacy and language", "پروفائل، پرائیویسی اور زبان")) }

        item {
            V95GlassCard {
                Text(c.t("Language", "زبان"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V95Button("English", Modifier.weight(1f), primary = c.language == "en") { c.changeLanguage("en") }
                    V95Button("اردو", Modifier.weight(1f), primary = c.language == "ur") { c.changeLanguage("ur") }
                }
            }
        }

        if (user != null) {
            item {
                V95GlassCard {
                    Text(c.t("Edit profile", "پروفائل ایڈٹ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    V95Field(name, c.t("Name", "نام")) { name = it }
                    V95Field(username, c.t("Username", "یوزرنیم")) { username = it }
                    V95TextArea(bio, c.t("Bio", "بائیو")) { bio = it }
                    V95Button(c.t("Save changes", "تبدیلیاں محفوظ کریں"), Modifier.fillMaxWidth(), primary = true) {
                        c.updateProfile(name, username, bio) { }
                    }
                }
            }

            item {
                V95GlassCard {
                    Text(c.t("Privacy", "پرائیویسی"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    V95SettingToggleRow(c.t("Private profile", "پرائیویٹ پروفائل"), profilePrivate) { profilePrivate = !profilePrivate }
                    V95SettingToggleRow(c.t("Show email", "ای میل دکھائیں"), showEmail) { showEmail = !showEmail }
                    V95SettingToggleRow(c.t("Show phone", "فون دکھائیں"), showPhone) { showPhone = !showPhone }
                    V95SettingToggleRow(c.t("Show location", "لوکیشن دکھائیں"), showLocation) { showLocation = !showLocation }
                    V95SettingToggleRow(c.t("Hide followers", "فالوورز چھپائیں"), hideFollowers) { hideFollowers = !hideFollowers }
                    V95SettingToggleRow(c.t("Accept messages", "پیغامات قبول کریں"), acceptMessages) { acceptMessages = !acceptMessages }

                    V95Button(c.t("Save privacy", "پرائیویسی محفوظ کریں"), Modifier.fillMaxWidth(), primary = true) {
                        c.savePrivacy(
                            if (profilePrivate) "private" else "public",
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
                V95GlassCard {
                    Text(c.t("Password", "پاس ورڈ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    V95Field(currentPassword, c.t("Current password", "موجودہ پاس ورڈ")) { currentPassword = it }
                    V95Field(newPassword, c.t("New password", "نیا پاس ورڈ")) { newPassword = it }
                    V95Field(confirmPassword, c.t("Confirm new password", "نیا پاس ورڈ دوبارہ")) { confirmPassword = it }
                    V95Button(
                        c.t("Change password", "پاس ورڈ تبدیل کریں"),
                        Modifier.fillMaxWidth(),
                        primary = true,
                        enabled = currentPassword.isNotBlank() && newPassword.length >= 6 && newPassword == confirmPassword
                    ) {
                        c.changePassword(currentPassword, newPassword) {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    }
                }
            }

            item {
                V95GlassCard {
                    Text(c.t("Blocked users", "بلاک کیے گئے یوزرز"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    V95Button(c.t("Load blocked users", "بلاک یوزرز دکھائیں"), icon = V95Icons.People) { c.loadBlockedUsers() }
                    c.blockedUsersList.forEach { person ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V95Avatar(person, 38.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(person.name, Modifier.weight(1f), color = V95Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            V95Button(c.t("Unblock", "ان بلاک"), danger = true) { c.toggleBlock(person) }
                        }
                    }
                }
            }

            item {
                V95GlassCard {
                    Text(c.t("Delete account", "اکاؤنٹ حذف کریں"), color = V95Danger, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    if (!showDelete) {
                        V95Button(c.t("Open delete account", "اکاؤنٹ حذف کرنے کا آپشن"), danger = true) { showDelete = true }
                    } else {
                        Text(
                            c.t("Enter your password to permanently delete this account.", "اکاؤنٹ مستقل حذف کرنے کے لیے پاس ورڈ درج کریں۔"),
                            color = V95Muted,
                            fontSize = 11.sp
                        )
                        V95Field(deletePassword, c.t("Password", "پاس ورڈ")) { deletePassword = it }
                        V95Button(
                            c.t("Delete permanently", "مستقل حذف کریں"),
                            Modifier.fillMaxWidth(),
                            danger = true,
                            enabled = deletePassword.isNotBlank()
                        ) {
                            c.deleteAccount(deletePassword)
                        }
                    }
                }
            }
        } else {
            item { V95RequireLogin(c) }
        }
    }
}

@Composable
private fun V95SettingToggleRow(label: String, value: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = .32f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = V95Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        V95Button(if (value) "ON" else "OFF", primary = value) { onClick() }
    }
}

@Composable
internal fun V95Admin(c: V95Controller) {
    V95AdminCenter(c)
}

@Composable
internal fun V95Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(v95InputBrush()).border(1.5.dp, Color.White, RoundedCornerShape(20.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = V95Purple, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text(label, color = V95Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun V95Auth(c: V95Controller) {
    var register by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        V95GlassCard(Modifier.widthIn(max = 420.dp), radius = 26.dp, padding = 22.dp) {
            RainbowBrand(fontSize = 30f)

            c.authInfo?.let {
                Text(
                    it,
                    color = V95Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            when {
                c.pendingVerifyUserId > 0L -> {
                    Text(
                        c.t("Verify your email", "اپنی ای میل ویریفائی کریں"),
                        color = V95Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        c.t("Enter the 6-digit code sent to your email.", "ای میل پر بھیجا گیا 6 ہندسوں کا کوڈ درج کریں۔"),
                        color = V95Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    V95Field(code, c.t("Verification code", "ویریفکیشن کوڈ")) { code = it.filter(Char::isDigit).take(6) }
                    V95Button(
                        c.t("Verify email", "ای میل ویریفائی کریں"),
                        Modifier.fillMaxWidth(),
                        primary = true,
                        enabled = code.length == 6
                    ) { c.verifyPendingEmail(code) }
                    V95Button(c.t("Resend code", "کوڈ دوبارہ بھیجیں"), Modifier.fillMaxWidth()) {
                        c.resendPendingEmail()
                    }
                }

                c.passwordResetKey.isNotBlank() -> {
                    Text(
                        c.t("Reset password", "پاس ورڈ ری سیٹ"),
                        color = V95Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    V95Field(code, c.t("Reset code", "ری سیٹ کوڈ")) { code = it.filter(Char::isDigit).take(6) }
                    V95PasswordField(password, c.t("New password", "نیا پاس ورڈ")) { password = it }
                    V95PasswordField(confirmPassword, c.t("Confirm password", "پاس ورڈ دوبارہ")) { confirmPassword = it }
                    V95Button(
                        c.t("Set new password", "نیا پاس ورڈ محفوظ کریں"),
                        Modifier.fillMaxWidth(),
                        primary = true,
                        enabled = code.length == 6 && password.length >= 6 && password == confirmPassword
                    ) {
                        c.finishPasswordReset(code, password) {
                            forgot = false
                            code = ""
                            password = ""
                            confirmPassword = ""
                        }
                    }
                }

                forgot -> {
                    Text(
                        c.t("Forgot password", "پاس ورڈ بھول گئے"),
                        color = V95Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    V95Field(email, c.t("Email address", "ای میل ایڈریس")) { email = it }
                    V95Button(
                        c.t("Send reset code", "ری سیٹ کوڈ بھیجیں"),
                        Modifier.fillMaxWidth(),
                        primary = true,
                        enabled = email.contains("@")
                    ) { c.startPasswordReset(email) }
                    V95Button(c.t("Back to login", "واپس لاگ اِن"), Modifier.fillMaxWidth()) { forgot = false }
                }

                else -> {
                    Text(
                        if (register) c.t("Create account", "اکاؤنٹ بنائیں") else c.t("Welcome back", "خوش آمدید"),
                        color = V95Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    if (register) {
                        V95Field(name, c.t("Full name", "پورا نام")) { name = it }
                        V95Field(username, c.t("Username", "یوزرنیم")) { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(30) }
                        V95Field(email, c.t("Email", "ای میل")) { email = it.trim() }
                    } else {
                        V95Field(identity, c.t("Email, username or phone", "ای میل، یوزرنیم یا فون")) { identity = it }
                    }

                    V95PasswordField(password, c.t("Password", "پاس ورڈ")) { password = it }

                    if (register) {
                        Row(
                            Modifier.fillMaxWidth().clickable { terms = !terms },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                                    .background(if (terms) v95PrimaryBrush() else v95InputBrush())
                                    .border(1.5.dp, Color.White, RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (terms) Text("✓", color = Color.White, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(c.t("I accept Terms & Conditions", "میں شرائط و ضوابط قبول کرتا ہوں"), color = V95Ink, fontSize = 11.sp)
                        }
                    }

                    V95Button(
                        if (register) c.t("Register", "رجسٹر") else c.t("Login", "لاگ اِن"),
                        Modifier.fillMaxWidth(),
                        primary = true,
                        enabled = password.length >= 6 &&
                            if (register) name.length >= 2 && username.length >= 3 && email.contains("@") && terms
                            else identity.isNotBlank()
                    ) {
                        if (register) c.register(name, username, email, password)
                        else c.login(identity, password)
                    }

                    if (!register) {
                        V95Button(c.t("Forgot password?", "پاس ورڈ بھول گئے؟"), Modifier.fillMaxWidth()) { forgot = true }
                    }

                    V95Button(
                        if (register) c.t("Already have an account? Login", "اکاؤنٹ ہے؟ لاگ اِن کریں")
                        else c.t("Create new account", "نیا اکاؤنٹ بنائیں"),
                        Modifier.fillMaxWidth()
                    ) { register = !register }
                }
            }
        }
    }
}

@Composable
private fun V95PasswordField(
    value: String,
    placeholder: String,
    onValue: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    V95InputShell(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = V95Ink, fontSize = 13.sp),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) Text(placeholder, color = Color(0xFF858EB1), fontSize = 12.sp)
                        inner()
                    }
                }
            )
            V95IconButton(V95Icons.Eye, 32.dp, 24.dp) { visible = !visible }
        }
    }
}

@Composable
internal fun V95Map(c: V95Controller) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<GeoPoint?>(null) }
    Configuration.getInstance().userAgentValue = context.packageName
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V95PageHeading(c.t("Chhachh Map", "چھچھ نقشہ"), c.t("Real native map", "حقیقی نیٹو نقشہ"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            V95Field(query, c.t("Search destination", "منزل تلاش کریں"), Modifier.weight(1f)) { query = it }
            V95Button(c.t("Find", "تلاش"), primary = true, icon = V95Icons.Search) {
                if (query.isNotBlank()) {
                    c.busy = true
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val places = withContext(Dispatchers.IO) { c.api.geocodePlaces(query) }
                            target = places.firstOrNull()?.let { GeoPoint(it.lat, it.lng) }
                        } catch (e: Throwable) { c.error = e.message } finally { c.busy = false }
                    }
                }
            }
        }
        V95GlassCard(Modifier.weight(1f), padding = 6.dp) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        controller.setCenter(GeoPoint(33.90977, 72.438))
                    }
                },
                update = { map ->
                    target?.let { p ->
                        map.overlays.removeAll { it is Marker }
                        val marker = Marker(map).apply { position = p; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }
                        map.overlays.add(marker)
                        map.controller.animateTo(p)
                        map.controller.setZoom(16.0)
                        map.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
            )
        }
    }
}

@Composable
internal fun V95PageHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(title, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 25.sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = V95Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun V95Field(value: String, placeholder: String, modifier: Modifier = Modifier, onValue: (String) -> Unit) {
    V95InputShell(modifier.fillMaxWidth()) {
        BasicTextField(value, onValue, Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = V95Ink, fontSize = 14.sp), decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) Text(placeholder, color = Color(0xFF858EB1), fontSize = 13.sp)
                inner()
            }
        })
    }
}

@Composable
internal fun V95Avatar(user: User, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFEAFBFF), Color(0xFFE9DEFF)))).border(3.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        if (!user.avatar.isNullOrBlank()) AsyncImage(user.avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(user.name.take(1).uppercase(), color = V95Purple, fontWeight = FontWeight.Black, fontSize = (size.value * .36f).sp)
    }
}

@Composable
internal fun V95ShopAvatar(shop: Shop, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFEAFBFF), Color(0xFFE9DEFF)))).border(3.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        if (!shop.photo.isNullOrBlank()) AsyncImage(shop.photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Image(painterResource(V95Icons.Shop), null, Modifier.size(size * .58f))
    }
}

@Composable
internal fun V95Empty(text: String) {
    V95GlassCard { Text(text, Modifier.fillMaxWidth(), color = V95Muted, fontSize = 14.sp, textAlign = TextAlign.Center) }
}

@Composable
internal fun V95RequireLogin(c: V95Controller) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        V95GlassCard {
            Text(c.t("Login required", "لاگ اِن درکار ہے"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(c.t("Please login to use this feature.", "اس فیچر کے لیے لاگ اِن کریں۔"), color = V95Muted)
            V95Button(c.t("Login / Register", "لاگ اِن / رجسٹریشن"), primary = true) { c.route = V95Route.AUTH }
        }
    }
}

@Composable
internal fun V95ErrorToast(message: String, modifier: Modifier, onDismiss: () -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFFDE9EF)).border(1.5.dp, Color.White, RoundedCornerShape(18.dp)).clickable(onClick = onDismiss).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(V95Icons.More), null, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text(message, Modifier.weight(1f), color = V95Danger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
