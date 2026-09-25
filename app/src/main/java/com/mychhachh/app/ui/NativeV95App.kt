package com.mychhachh.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun NativeV95App() {
    val context = LocalContext.current
    val c = remember { V95Controller(context) }
    DisposableEffect(Unit) { onDispose { c.dispose() } }
    var currentHour by remember { mutableIntStateOf(LocalTime.now().hour) }
    LaunchedEffect(Unit) {
        c.bootstrap()
        c.loadWeather(false)
        while (true) {
            currentHour = LocalTime.now().hour
            delay(60_000)
        }
    }

    val direction = if (c.language == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr
    val weatherNow = c.weather?.optJSONObject("current") ?: c.weather
    val weatherText = weatherNow?.let { it.optString("condition", it.optString("weather", "")) }?.lowercase().orEmpty()
    val liveBackground = when {
        currentHour < 6 || currentHour >= 18 -> Brush.verticalGradient(
            listOf(Color(0xFF18264C), Color(0xFF2C3C6D), Color(0xFF493B6A))
        )
        weatherText.contains("rain") || weatherText.contains("storm") -> Brush.verticalGradient(
            listOf(Color(0xFFD5E3EE), Color(0xFFE5EAF4), Color(0xFFE9E2F2))
        )
        weatherText.contains("fog") || weatherText.contains("mist") -> Brush.verticalGradient(
            listOf(Color(0xFFE3EBEF), Color(0xFFF1F3F6), Color(0xFFECEAF3))
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFFDFF7FF), Color(0xFFF6F2FF), Color(0xFFFFF1F8))
        )
    }
    val baseDensity = LocalDensity.current
    val cssDensity = Density(baseDensity.density, 1f)

    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalDensity provides cssDensity
    ) {
        NativeV95Theme {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(liveBackground)
            ) {
                Column(Modifier.fillMaxSize()) {
                    NativeHeader(c)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (c.route) {
                            V95Route.HOME -> NativeHome(c)
                            V95Route.PEOPLE -> NativePeople(c)
                            V95Route.SHOPS -> NativeShops(c)
                            V95Route.MAP -> NativeMap(c)
                            V95Route.MESSAGES -> NativeMessages(c)
                            V95Route.VOTES -> NativeVotes(c)
                            V95Route.ANNOUNCEMENTS -> NativeAnnouncements(c)
                            V95Route.NOTIFICATIONS -> NativeNotifications(c)
                            V95Route.PROFILE -> NativeProfile(c)
                            V95Route.SHOP_DETAIL -> NativeShopDetail(c)
                            V95Route.CHAT -> NativeChat(c)
                            V95Route.SEARCH -> NativeSearch(c)
                            V95Route.WEATHER -> NativeWeather(c)
                            V95Route.SETTINGS -> NativeSettings(c)
                            V95Route.ADMIN -> NativeAdmin(c)
                            V95Route.AUTH -> NativeAuth(c)
                            V95Route.POST_DETAIL -> NativePostDetail(c)
                            V95Route.SAVED -> NativeSaved(c)
                            V95Route.RELATIONS -> NativeRelations(c)
                            V95Route.THEME -> NativeThemeBuilder(c)
                            V95Route.ANNOUNCEMENT_DETAIL -> NativeAnnouncementDetail(c)
                            V95Route.VOTE_DETAIL -> NativeVoteDetail(c)
                        }
                        if (c.busy) {
                            Box(
                                Modifier.matchParentSize().background(Color.White.copy(.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NVBlue)
                            }
                        }
                    }
                }
                if (c.menuOpen) NativeSideMenu(c)
                c.error?.let { msg ->
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(.95f))
                            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
                            .clickable { c.error = null }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(NVIcons.Info), null, Modifier.size(24.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(msg, color = NVDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeHeader(c: V95Controller) {
    val width = LocalConfiguration.current.screenWidthDp
    val compact = width <= 390
    val user = c.user
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFDAF7FF).copy(.88f),
                        Color(0xFFE4F4FF).copy(.73f),
                        Color(0xFFEEEAFF).copy(.68f)
                    )
                )
            )
            .padding(start = 7.dp, end = 7.dp, top = 8.dp, bottom = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(47.dp), verticalAlignment = Alignment.CenterVertically) {
            NVIconButton(NVIcons.Menu, size = 43.dp, iconSize = 35.dp) { c.menuOpen = true }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val brandName = c.features.optString("site_name", "My Chhachh")
                val brandLogo = c.features.optString("site_icon", "").trim()
                if (brandLogo.isNotBlank()) {
                    AsyncImage(
                        model = brandLogo,
                        contentDescription = brandName,
                        modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    NVBrand(if (compact) 20f else 22f, brandName)
                }
            }
            if (user != null) {
                Box(
                    Modifier.size(43.dp).clickable {
                        c.route = V95Route.NOTIFICATIONS
                        c.loadNotifications()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(NVIcons.Bell), null, Modifier.size(36.dp))
                    if (c.unread > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(999.dp))
                                .background(NVPink)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(if (c.unread > 99) "99+" else c.unread.toString(), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(Modifier.width(3.dp))
                NVAvatar(user, 41.dp, Modifier.clickable { c.openProfile(user) })
            } else {
                val showLogin = c.features.optInt("theme_guest_login_button", 1) != 0
                val showRegister = c.features.optInt("theme_guest_register_button", 1) != 0
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showLogin) NativeTopPill(c.t("Login", "لاگ اِن"), false, compact) { c.route = V95Route.AUTH }
                    if (showRegister) NativeTopPill(c.t("Sign up", "رجسٹر"), true, compact) { c.route = V95Route.AUTH }
                }
            }
        }

        NativeSearchBar(c)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NativeQuickCard(c.t("Voting", "ووٹنگ"), NVIcons.Vote, compact, Modifier.weight(1f)) {
                c.route = V95Route.VOTES
                c.loadVotes()
            }
            NativeQuickCard(c.t("Weather", "موسم"), NVIcons.Weather, compact, Modifier.weight(1f)) {
                c.route = V95Route.WEATHER
                c.loadWeather()
            }
            Box(Modifier.weight(1f)) {
                NativeQuickCard(c.t("Announcements", "اعلانات"), NVIcons.Announcement, compact, Modifier.fillMaxWidth()) {
                    c.route = V95Route.ANNOUNCEMENTS
                    c.loadAnnouncements()
                }
                if (c.announcementUnread > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(NVPink)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(c.announcementUnread.toString(), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        NativeNav(c, compact)
    }
}

@Composable
private fun NativeTopPill(text: String, register: Boolean, compact: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .height(42.dp)
            .clip(shape)
            .background(
                if (register) Brush.linearGradient(listOf(Color.White.copy(.84f), Color(0xFFFFE4F4).copy(.76f)))
                else Brush.linearGradient(listOf(Color.White.copy(.78f), Color.White.copy(.68f)))
            )
            .border(1.5.dp, Color.White, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 7.dp else 10.dp),
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
private fun NativeSearchBar(c: V95Controller) {
    var text by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(nvInputBrush())
            .border(1.5.dp, Color.White, shape)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NVIconButton(NVIcons.Search, size = 38.dp, iconSize = 32.dp) {
            if (text.isNotBlank()) c.search(text)
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = NVInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isBlank()) Text(c.t("Search people, posts, places…", "لوگ، پوسٹس اور جگہیں تلاش کریں…"), color = Color(0xFF858EB1), fontSize = 13.sp)
                    inner()
                }
            }
        )
        NVIconButton(NVIcons.Filter, size = 38.dp, iconSize = 32.dp) {
            if (text.isNotBlank()) c.search(text)
        }
    }
}

@Composable
private fun NativeQuickCard(text: String, icon: Int, compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(21.dp)
    Column(
        modifier
            .height(if (compact) 58.dp else 64.dp)
            .clip(shape)
            .background(nvGlassBrush())
            .border(1.5.dp, Color.White, shape)
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(if (compact) 31.dp else 34.dp))
        Spacer(Modifier.height(2.dp))
        Text(text, color = NVPurple, fontWeight = FontWeight.Black, fontSize = if (compact) 8.sp else 8.8.sp, maxLines = 1)
    }
}

@Composable
private fun NativeNav(c: V95Controller, compact: Boolean) {
    val shape = RoundedCornerShape(23.dp)
    val definitions = linkedMapOf(
        "home" to Triple(c.t("Home", "ہوم"), NVIcons.Home, V95Route.HOME),
        "people" to Triple(c.t("People", "لوگ"), NVIcons.People, V95Route.PEOPLE),
        "shop" to Triple(c.t("Shop", "دکانیں"), NVIcons.Shop, V95Route.SHOPS),
        "map" to Triple(c.t("Map", "نقشہ"), NVIcons.Map, V95Route.MAP),
        "messages" to Triple(c.t("Messages", "پیغامات"), NVIcons.Message, V95Route.MESSAGES)
    )
    val rawOrder = c.features.optString("theme_header_items", "home,people,shop,map,messages")
    val orderedItems = rawOrder
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .distinct()
        .mapNotNull { definitions[it] }
        .take(5)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(nvGlassBrush())
            .border(1.5.dp, Color.White, shape)
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 5.dp)
    ) {
        orderedItems.forEach { (label, icon, route) ->
            NativeNavItem(c, label, icon, route, compact)
        }
    }
}

@Composable
private fun RowScope.NativeNavItem(c: V95Controller, text: String, icon: Int, target: V95Route, compact: Boolean) {
    val active = c.route == target
    Column(
        Modifier
            .weight(1f)
            .height(if (compact) 64.dp else 68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) Brush.linearGradient(listOf(Color(0xFFFFF0F9).copy(.68f), Color.White.copy(.45f)))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
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
        Text(text, color = NVPurple, fontWeight = FontWeight.Black, fontSize = if (compact) 8.4.sp else 9.sp, maxLines = 1)
        if (active) Box(Modifier.width(34.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFF67BA), Color(0xFFFF43AA)))))
    }
}

@Composable
private fun NativeSideMenu(c: V95Controller) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x3D0A203A))
            .clickable { c.menuOpen = false }
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(.86f)
                .widthIn(max = 330.dp)
                .background(nvGlassBrush())
                .padding(top = 14.dp, start = 12.dp, end = 12.dp, bottom = 22.dp)
                .clickable(enabled = false) {},
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                NVIconButton(NVIcons.Close) { c.menuOpen = false }
            }
            val menuDefinitions = linkedMapOf(
                "home" to Triple(c.t("Home", "ہوم"), NVIcons.Home, V95Route.HOME),
                "votes" to Triple(c.t("Voting", "ووٹنگ"), NVIcons.Vote, V95Route.VOTES),
                "saved" to Triple(c.t("Saved", "محفوظ"), NVIcons.Save, V95Route.SAVED),
                "announcements" to Triple(c.t("Announcements", "اعلانات"), NVIcons.Announcement, V95Route.ANNOUNCEMENTS),
                "notifications" to Triple(c.t("Notifications", "اطلاعات"), NVIcons.Bell, V95Route.NOTIFICATIONS),
                "settings" to Triple(c.t("Settings", "ترتیبات"), NVIcons.Gear, V95Route.SETTINGS),
                "theme" to Triple(c.t("Theme Builder", "تھیم بلڈر"), NVIcons.Palette, V95Route.THEME),
                "admin" to Triple(c.t("Admin Center", "ایڈمن سینٹر"), NVIcons.Shield, V95Route.ADMIN)
            )
            val rawMenuOrder = c.features.optString(
                "theme_menu_items",
                "votes,saved,announcements,notifications,settings,theme,admin,logout"
            )
            val menuKeys = rawMenuOrder
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
            val showLogout = "logout" in menuKeys
            val menu = menuKeys.mapNotNull { menuDefinitions[it] }
            menu.forEach { (label, icon, route) ->
                if (route != V95Route.ADMIN || c.user?.isAdmin == true) {
                    if (route != V95Route.PROFILE || c.user != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    c.menuOpen = false
                                    c.route = route
                                    when (route) {
                                        V95Route.HOME -> c.loadFeed()
                                        V95Route.VOTES -> c.loadVotes()
                                        V95Route.SAVED -> c.loadSaved()
                                        V95Route.ANNOUNCEMENTS -> c.loadAnnouncements()
                                        V95Route.NOTIFICATIONS -> c.loadNotifications()
                                        V95Route.PROFILE -> c.user?.let(c::openProfile)
                                        V95Route.ADMIN -> c.loadAdmin()
                                        else -> Unit
                                    }
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(icon), null, Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(label, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (c.user != null && showLogout) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            c.menuOpen = false
                            c.logout()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(NVIcons.Logout), null, Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(c.t("Logout", "لاگ آؤٹ"), color = NVDanger, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            } else {
                NVButton(c.t("Login / Register", "لاگ اِن / رجسٹریشن"), Modifier.fillMaxWidth(), primary = true) {
                    c.menuOpen = false
                    c.route = V95Route.AUTH
                }
            }
        }
    }
}
