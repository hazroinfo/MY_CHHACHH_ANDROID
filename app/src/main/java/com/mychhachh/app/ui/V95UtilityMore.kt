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
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { V95PageHeading(c.t("Settings", "ترتیبات"), c.t("Profile, privacy and language", "پروفائل، پرائیویسی اور زبان")) }
        item {
            V95GlassCard {
                Text(c.t("Language", "زبان"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V95Button("English", primary = c.language == "en") { c.changeLanguage("en") }
                    V95Button("اردو", primary = c.language == "ur") { c.changeLanguage("ur") }
                }
            }
        }
        if (user != null) item {
            V95GlassCard {
                Text(c.t("Edit profile", "پروفائل ایڈٹ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                V95Field(name, c.t("Name", "نام")) { name = it }
                V95Field(username, c.t("Username", "یوزرنیم")) { username = it }
                V95TextArea(bio, c.t("Bio", "بائیو")) { bio = it }
                V95Button(c.t("Save changes", "تبدیلیاں محفوظ کریں"), primary = true) { c.updateProfile(name, username, bio) { } }
            }
        }
    }
}

@Composable
internal fun V95Admin(c: V95Controller) {
    if (c.user?.isAdmin != true) { V95Empty(c.t("Administrator access required", "ایڈمن رسائی درکار ہے")); return }
    LaunchedEffect(Unit) { c.loadAdmin() }
    val state = c.adminState
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { V95PageHeading(c.t("Admin Center", "ایڈمن سینٹر"), c.t("Site management", "سائٹ مینجمنٹ")) }
        item {
            V95GlassCard {
                Text(c.t("Overview", "خلاصہ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                val keys = listOf("users", "posts", "shops", "reports", "pending_verifications", "messages")
                keys.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { key ->
                            val fallback = state?.optInt(key, 0) ?: 0
                            val v = state?.optJSONObject("counts")?.optInt(key, fallback) ?: fallback
                            V95Metric(key.replace('_', ' ').replaceFirstChar { it.uppercase() }, v.toString(), Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            V95GlassCard {
                Text(c.t("Management", "مینجمنٹ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(c.t("User controls, verification, reports, votes, announcements and activity logs use the same live admin API.", "یوزر کنٹرول، ویریفکیشن، رپورٹس، ووٹس، اعلانات اور ایکٹیویٹی لاگز اسی لائیو ایڈمن API سے چلتے ہیں۔"), color = V95Muted, fontSize = 13.sp)
            }
        }
    }
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
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        V95GlassCard(Modifier.widthIn(max = 420.dp), radius = 26.dp, padding = 22.dp) {
            RainbowBrand(fontSize = 30f)
            Text(if (register) c.t("Create account", "اکاؤنٹ بنائیں") else c.t("Welcome back", "خوش آمدید"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (register) {
                V95Field(name, c.t("Full name", "پورا نام")) { name = it }
                V95Field(username, c.t("Username", "یوزرنیم")) { username = it }
                V95Field(email, c.t("Email", "ای میل")) { email = it }
            } else V95Field(identity, c.t("Email or username", "ای میل یا یوزرنیم")) { identity = it }
            V95Field(password, c.t("Password", "پاس ورڈ")) { password = it }
            if (register) {
                Row(Modifier.fillMaxWidth().clickable { terms = !terms }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (terms) v95PrimaryBrush() else v95InputBrush()).border(1.5.dp, Color.White, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { if (terms) Text("✓", color = Color.White, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(8.dp))
                    Text(c.t("I accept Terms & Conditions", "میں شرائط و ضوابط قبول کرتا ہوں"), color = V95Ink, fontSize = 12.sp)
                }
            }
            V95Button(if (register) c.t("Register", "رجسٹر") else c.t("Login", "لاگ اِن"), Modifier.fillMaxWidth(), primary = true, enabled = password.isNotBlank() && (!register || terms)) {
                if (register) c.register(name, username, email, password) else c.login(identity, password)
            }
            V95Button(if (register) c.t("Already have an account? Login", "اکاؤنٹ ہے؟ لاگ اِن کریں") else c.t("Create new account", "نیا اکاؤنٹ بنائیں"), Modifier.fillMaxWidth()) { register = !register }
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
