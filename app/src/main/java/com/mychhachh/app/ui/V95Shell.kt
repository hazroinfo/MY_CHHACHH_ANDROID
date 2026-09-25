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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
fun V95App() {
    val context = LocalContext.current
    val c = remember { V95Controller(context) }
    DisposableEffect(Unit) { onDispose { c.dispose() } }
    LaunchedEffect(Unit) { c.bootstrap() }
    val direction = if (c.language == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr
    val baseDensity = LocalDensity.current
    val cssDensity = Density(baseDensity.density, 1f)

    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalDensity provides cssDensity,
        LocalV95Features provides c.features
    ) {
        V95Theme {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFDFF7FF), Color(0xFFF6F2FF), Color(0xFFFFF1F8))
                        )
                    )
            ) {
                Column(Modifier.fillMaxSize()) {
                    V95Header(c)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (c.route) {
                            V95Route.HOME -> V95Home(c)
                            V95Route.PEOPLE -> V95People(c)
                            V95Route.SHOPS -> V95Shops(c)
                            V95Route.MAP -> V95Map(c)
                            V95Route.MESSAGES -> V95Messages(c)
                            V95Route.VOTES -> V95Votes(c)
                            V95Route.ANNOUNCEMENTS -> V95Announcements(c)
                            V95Route.NOTIFICATIONS -> V95Notifications(c)
                            V95Route.PROFILE -> V95Profile(c)
                            V95Route.SHOP_DETAIL -> V95ShopDetail(c)
                            V95Route.CHAT -> V95Chat(c)
                            V95Route.SEARCH -> V95Search(c)
                            V95Route.WEATHER -> V95Weather(c)
                            V95Route.SETTINGS -> V95Settings(c)
                            V95Route.ADMIN -> V95Admin(c)
                            V95Route.AUTH -> V95Auth(c)
                            V95Route.POST_DETAIL -> V95PostDetail(c)
                            V95Route.SAVED -> V95Saved(c)
                            V95Route.RELATIONS -> V95Relations(c)
                            V95Route.THEME -> V95ThemeBuilder(c)
                            V95Route.ANNOUNCEMENT_DETAIL -> V95AnnouncementDetail(c)
                        }
                        if (c.busy) {
                            Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .28f)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = V95Blue)
                            }
                        }
                    }
                }
                if (c.menuOpen) V95SideMenu(c)
                c.error?.let { msg ->
                    V95ErrorToast(msg, Modifier.align(Alignment.BottomCenter).padding(16.dp)) { c.error = null }
                }
            }
        }
    }
}

@Composable
internal fun V95Header(c: V95Controller) {
    val width = LocalConfiguration.current.screenWidthDp
    val compact = width <= 390
    val user = c.user
    val headerRadius = c.features.optDouble("theme_header_radius", 28.0).toFloat().dp
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = headerRadius, bottomEnd = headerRadius))
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFDAF7FF).copy(.92f), Color(0xFFE4F4FF).copy(.82f), Color(0xFFEEEAFF).copy(.78f))
                )
            )
            .padding(start = 7.dp, end = 7.dp, top = 8.dp, bottom = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(47.dp), verticalAlignment = Alignment.CenterVertically) {
            V95IconButton(V95Icons.Menu, 43.dp, 35.dp) { c.menuOpen = true }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                RainbowBrand(fontSize = (width * .08f).coerceIn(24f, 34f))
            }
            if (user != null) {
                Box(Modifier.size(45.dp).clickable { c.route = V95Route.NOTIFICATIONS; c.loadNotifications() }, contentAlignment = Alignment.Center) {
                    Image(painterResource(V95Icons.Bell), null, Modifier.size(36.dp))
                    if (c.unread > 0) V95Badge(if (c.unread > 99) "99+" else c.unread.toString(), Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.width(4.dp))
                V95Avatar(user, 41.dp, Modifier.clickable { c.openProfile(user) })
            } else {
                val authEnabled = c.features.optInt("theme_guest_auth_buttons", 1) != 0
                val showLogin = authEnabled && c.features.optInt("theme_guest_login_button", 1) != 0
                val showRegister = authEnabled && c.features.optInt("theme_guest_register_button", 1) != 0
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showLogin) {
                        V95TopAuthButton(c.t("Login", "لاگ اِن"), compact = compact) { c.route = V95Route.AUTH }
                    }
                    if (showRegister) {
                        V95TopAuthButton(c.t("Sign up", "رجسٹر"), register = true, compact = compact) { c.route = V95Route.AUTH }
                    }
                }
            }
        }
        V95SearchBar(c)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            V95QuickCard(c.t("Voting", "ووٹنگ"), V95Icons.Vote, Modifier.weight(1f), compact) { c.route = V95Route.VOTES; c.loadVotes() }
            V95QuickCard(c.t("Weather", "موسم"), V95Icons.Weather, Modifier.weight(1f), compact) { c.route = V95Route.WEATHER; c.loadWeather() }
            Box(Modifier.weight(1f)) {
                V95QuickCard(c.t("Announcements", "اعلانات"), V95Icons.Announcement, Modifier.fillMaxWidth(), compact) { c.route = V95Route.ANNOUNCEMENTS; c.loadAnnouncements() }
                if (c.announcementUnread > 0) V95Badge(c.announcementUnread.toString(), Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
        }
        V95Nav(c, compact)
    }
}

@Composable
private fun V95TopAuthButton(
    text: String,
    register: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .height(42.dp)
            .clip(shape)
            .background(
                if (register) {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = .84f), Color(0xFFFFE4F4).copy(alpha = .76f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = .78f), Color.White.copy(alpha = .68f))
                    )
                }
            )
            .border(1.5.dp, Color.White, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 7.dp else 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (register) Color(0xFF5B3D86) else Color(0xFF4D3C82),
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 9.sp else 10.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun V95SearchBar(c: V95Controller) {
    var text by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(shape).background(v95InputBrush()).border(1.5.dp, Color.White, shape).padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V95IconButton(V95Icons.Search, 38.dp, 32.dp) { if (text.isNotBlank()) c.search(text) }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = V95Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isBlank()) Text(c.t("Search people, posts, places…", "لوگ، پوسٹس اور جگہیں تلاش کریں…"), color = Color(0xFF858EB1), fontSize = 13.sp)
                    inner()
                }
            }
        )
        V95IconButton(V95Icons.Filter, 38.dp, 32.dp) { if (text.isNotBlank()) c.search(text) }
    }
}

@Composable
internal fun V95QuickCard(text: String, icon: Int, modifier: Modifier, compact: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(21.dp)
    Column(
        modifier.height(if (compact) 76.dp else 82.dp).clip(shape).background(v95GlassBrush()).border(2.dp, Color.White, shape).clickable(onClick = onClick).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(if (icon == V95Icons.Weather) 37.dp else if (compact) 31.dp else 34.dp))
        Spacer(Modifier.height(3.dp))
        Text(text, color = V95Purple, fontWeight = FontWeight.Black, fontSize = if (compact) 8.sp else 8.8.sp, maxLines = 1)
    }
}

@Composable
internal fun V95Nav(c: V95Controller, compact: Boolean) {
    val shape = RoundedCornerShape(c.features.optDouble("theme_nav_radius", 23.0).toFloat().dp)
    val navHeight = c.features.optDouble("theme_nav_height", 72.0).toFloat().dp
    val configured = c.features.optString("theme_header_items", "home,people,shop,map,messages")
        .split(',').map { it.trim() }.filter { it.isNotBlank() }
    Row(
        Modifier.fillMaxWidth().clip(shape).background(v95GlassBrush()).border(2.dp, Color.White, shape).padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 5.dp)
    ) {
        configured.forEach { key ->
            when (key) {
                "home" -> V95NavItem(c, c.t("Home", "ہوم"), V95Icons.Home, V95Route.HOME, compact, navHeight)
                "people" -> V95NavItem(c, c.t("People", "لوگ"), V95Icons.People, V95Route.PEOPLE, compact, navHeight)
                "shop" -> V95NavItem(c, c.t("Shop", "دکانیں"), V95Icons.Shop, V95Route.SHOPS, compact, navHeight)
                "map" -> V95NavItem(c, c.t("Map", "نقشہ"), V95Icons.Map, V95Route.MAP, compact, navHeight)
                "messages" -> V95NavItem(c, c.t("Messages", "پیغامات"), V95Icons.Message, V95Route.MESSAGES, compact, navHeight)
            }
        }
    }
}

@Composable
internal fun RowScope.V95NavItem(c: V95Controller, text: String, icon: Int, target: V95Route, compact: Boolean, navHeight: androidx.compose.ui.unit.Dp) {
    val active = c.route == target
    Column(
        Modifier.weight(1f).height(navHeight).clip(RoundedCornerShape(20.dp))
            .background(if (active) Brush.linearGradient(listOf(Color(0xFFFFF0F9).copy(.7f), Color.White.copy(.45f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable {
                c.route = target
                when (target) {
                    V95Route.HOME -> c.loadFeed()
                    V95Route.PEOPLE -> c.loadPeople()
                    V95Route.SHOPS -> c.loadShops()
                    V95Route.MESSAGES -> c.loadMessages()
                    else -> Unit
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(if (compact) 30.dp else 32.dp))
        Text(text, color = V95Purple, fontWeight = FontWeight.Black, fontSize = if (compact) 8.4.sp else 9.sp, maxLines = 1)
        if (active) Box(Modifier.width(34.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFF67BA), Color(0xFFFF43AA)))))
    }
}

@Composable
internal fun V95SideMenu(c: V95Controller) {
    Box(Modifier.fillMaxSize().background(Color(0x3D0A203A)).clickable { c.menuOpen = false }) {
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(.88f).widthIn(max = 350.dp).background(v95GlassBrush()).padding(14.dp).clickable(enabled = false) {},
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { V95IconButton(V95Icons.Close) { c.menuOpen = false } }
            val configuredMenu = c.features.optString("theme_menu_items", "votes,saved,settings,theme,admin,logout")
                .split(',').map { it.trim() }.filter { it.isNotBlank() }
            val entries = buildList {
                configuredMenu.forEach { key ->
                    when (key) {
                        "votes" -> add(Triple(c.t("Voting", "ووٹنگ"), V95Icons.Vote, V95Route.VOTES))
                        "saved" -> add(Triple(c.t("Saved", "محفوظ"), V95Icons.Save, V95Route.SAVED))
                        "settings" -> add(Triple(c.t("Settings", "ترتیبات"), V95Icons.Gear, V95Route.SETTINGS))
                        "theme" -> if (c.user?.isAdmin == true && c.features.optInt("theme_theme_icon_enabled", 1) != 0) add(Triple(c.t("Theme Builder", "تھیم بلڈر"), V95Icons.Palette, V95Route.THEME))
                        "admin" -> if (c.user?.isAdmin == true) add(Triple(c.t("Admin Center", "ایڈمن سینٹر"), V95Icons.Shield, V95Route.ADMIN))
                        "logout" -> Unit
                    }
                }
                add(Triple(c.t("Announcements", "اعلانات"), V95Icons.Announcement, V95Route.ANNOUNCEMENTS))
                add(Triple(c.t("Notifications", "اطلاعات"), V95Icons.Bell, V95Route.NOTIFICATIONS))
                if (c.user != null) add(Triple(c.t("Profile", "پروفائل"), V95Icons.User, V95Route.PROFILE))
            }
            entries.forEach { (label, icon, route) ->
                V95MenuRow(label, icon) {
                    c.menuOpen = false
                    c.route = route
                    when (route) {
                        V95Route.HOME -> c.loadFeed()
                        V95Route.VOTES -> c.loadVotes()
                        V95Route.ANNOUNCEMENTS -> c.loadAnnouncements()
                        V95Route.NOTIFICATIONS -> c.loadNotifications()
                        V95Route.PROFILE -> c.user?.let(c::openProfile)
                        V95Route.SAVED -> c.loadSaved()
                        V95Route.ADMIN -> c.loadAdmin()
                        else -> Unit
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (c.user != null) V95MenuRow(c.t("Logout", "لاگ آؤٹ"), V95Icons.Logout, true) { c.menuOpen = false; c.logout() }
            else V95MenuRow(c.t("Login / Register", "لاگ اِن / رجسٹریشن"), V95Icons.User) { c.menuOpen = false; c.route = V95Route.AUTH }
        }
    }
}

@Composable
internal fun V95MenuRow(text: String, icon: Int, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(34.dp))
        Spacer(Modifier.width(13.dp))
        Text(text, color = if (danger) V95Danger else V95Purple, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}
