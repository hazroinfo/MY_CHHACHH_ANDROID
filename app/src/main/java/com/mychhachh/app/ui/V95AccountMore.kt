package com.mychhachh.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychhachh.app.data.User
import org.json.JSONObject

@Composable
internal fun V95Saved(c: V95Controller) {
    if (c.user == null) {
        V95RequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { c.loadSaved() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { V95PageHeading(c.t("Saved", "محفوظ"), c.t("Posts saved to your profile", "آپ کی محفوظ پوسٹس")) }
        if (c.saved.isEmpty() && !c.busy) item { V95Empty(c.t("No saved posts yet", "ابھی کوئی محفوظ پوسٹ نہیں")) }
        items(c.saved, key = { it.id }) { V95PostCard(c, it) }
    }
}

@Composable
internal fun V95Relations(c: V95Controller) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V95Button(c.t("Back", "واپس")) { c.route = V95Route.PROFILE }
                Spacer(Modifier.width(8.dp))
                V95PageHeading(c.relationTitle, "")
            }
        }
        if (c.relationUsers.isEmpty() && !c.busy) item { V95Empty(c.t("No users", "کوئی یوزر نہیں")) }
        items(c.relationUsers, key = { it.id }) { person -> V95RelationRow(c, person) }
    }
}

@Composable
private fun V95RelationRow(c: V95Controller, person: User) {
    V95GlassCard(radius = 19.dp, padding = 10.dp) {
        Row(
            Modifier.fillMaxWidth().clickable { c.openProfile(person) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            V95Avatar(person, 46.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("@${person.username}", color = V95Muted, fontSize = 10.sp)
            }
            if (c.user?.id != person.id) {
                V95Button(if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), primary = !person.followed) {
                    if (c.user == null) c.route = V95Route.AUTH else c.follow(person)
                }
            }
        }
    }
}

@Composable
internal fun V95ThemeBuilder(c: V95Controller) {
    if (c.user?.isAdmin != true) {
        V95Empty(c.t("Administrator access required", "ایڈمن رسائی درکار ہے"))
        return
    }

    val featuresKey = c.features.toString()
    var siteName by remember(featuresKey) { mutableStateOf(c.features.optString("site_name", "My Chhachh")) }
    var tagline by remember(featuresKey) { mutableStateOf(c.features.optString("site_tagline", "People • Places • Good Vibes")) }
    var cardRadius by remember(featuresKey) { mutableFloatStateOf(c.features.optDouble("theme_card_radius", 28.0).toFloat()) }
    var navHeight by remember(featuresKey) { mutableFloatStateOf(c.features.optDouble("theme_nav_height", 72.0).toFloat()) }
    var buttonHeight by remember(featuresKey) { mutableFloatStateOf(c.features.optDouble("theme_button_height", 44.0).toFloat()) }
    var profileCover by remember(featuresKey) { mutableFloatStateOf(c.features.optDouble("theme_profile_cover_height", 165.0).toFloat()) }
    var profileAvatar by remember(featuresKey) { mutableFloatStateOf(c.features.optDouble("theme_profile_avatar_size", 96.0).toFloat()) }
    var brandBrightness by remember(featuresKey) { mutableFloatStateOf(c.features.optDouble("theme_brand_brightness", 100.0).toFloat()) }
    var guestLogin by remember(featuresKey) { mutableStateOf(c.features.optInt("theme_guest_login_button", 1) != 0) }
    var guestRegister by remember(featuresKey) { mutableStateOf(c.features.optInt("theme_guest_register_button", 1) != 0) }

    val headerAll = remember { listOf("home", "people", "shop", "map", "messages") }
    val menuAll = remember { listOf("votes", "saved", "settings", "theme", "admin", "logout") }

    val headerOrder = remember(featuresKey) {
        mutableStateListOf<String>().apply {
            val saved = c.features.optString("theme_header_items", "home,people,shop,map,messages")
                .split(',').map { it.trim() }.filter { it in headerAll }
            addAll(saved + headerAll.filterNot { it in saved })
        }
    }

    val menuOrder = remember(featuresKey) {
        mutableStateListOf<String>().apply {
            val saved = c.features.optString("theme_menu_items", "votes,saved,settings,theme,admin,logout")
                .split(',').map { it.trim() }.filter { it in menuAll }
            addAll(saved + menuAll.filterNot { it in saved })
        }
    }

    val headerEnabled = remember(featuresKey) {
        mutableStateMapOf<String, Boolean>().apply {
            val active = c.features.optString("theme_header_items", "home,people,shop,map,messages").split(',').toSet()
            headerAll.forEach { put(it, it in active) }
        }
    }

    val menuEnabled = remember(featuresKey) {
        mutableStateMapOf<String, Boolean>().apply {
            val active = c.features.optString("theme_menu_items", "votes,saved,settings,theme,admin,logout").split(',').toSet()
            menuAll.forEach { put(it, it in active) }
        }
    }

    var logoUrl by remember(featuresKey) { mutableStateOf(c.features.optString("site_icon", "")) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            c.upload(uri, "site-brand-icon") { uploaded ->
                logoUrl = uploaded
                c.saveBrandFields(
                    JSONObject()
                        .put("site_name", siteName)
                        .put("site_tagline", tagline)
                        .put("site_icon", uploaded)
                        .put("site_icon_enabled", true)
                        .put("site_tagline_enabled", true)
                        .put("site_icon_fit", "contain")
                        .put("site_icon_size", 40)
                        .put("site_icon_header_blend", true)
                )
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { V95PageHeading(c.t("Theme Builder", "تھیم بلڈر"), c.t("Complete site appearance controls", "مکمل ظاہری کنٹرول")) }

        item {
            V95GlassCard {
                Text(c.t("Website branding & logo", "ویب سائٹ نام اور لوگو"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                V95Field(siteName, c.t("Website name", "ویب سائٹ نام")) { siteName = it }
                V95Field(tagline, c.t("Tagline", "ٹیگ لائن")) { tagline = it }
                if (logoUrl.isNotBlank()) {
                    coil.compose.AsyncImage(logoUrl, null, Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)))
                }
                V95Button(c.t("Change website logo", "ویب سائٹ لوگو تبدیل کریں"), icon = V95Icons.Photo) {
                    logoPicker.launch("image/*")
                }
                V95Button(c.t("Save branding", "برانڈنگ محفوظ کریں"), primary = true) {
                    c.saveBrandFields(
                        JSONObject()
                            .put("site_name", siteName)
                            .put("site_tagline", tagline)
                            .put("site_icon", logoUrl)
                            .put("site_icon_enabled", logoUrl.isNotBlank())
                            .put("site_tagline_enabled", tagline.isNotBlank())
                            .put("site_icon_fit", "contain")
                            .put("site_icon_size", 40)
                            .put("site_icon_header_blend", true)
                    )
                }
            }
        }

        item {
            V95GlassCard {
                Text(c.t("Frame & size controls", "فریم اور سائز کنٹرول"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                V95ThemeSlider(c.t("Card radius", "کارڈ گولائی"), cardRadius, 0f..60f) { cardRadius = it }
                V95ThemeSlider(c.t("Navigation height", "نیویگیشن اونچائی"), navHeight, 44f..100f) { navHeight = it }
                V95ThemeSlider(c.t("Button height", "بٹن اونچائی"), buttonHeight, 32f..72f) { buttonHeight = it }
                V95ThemeSlider(c.t("Profile cover", "پروفائل کور"), profileCover, 90f..300f) { profileCover = it }
                V95ThemeSlider(c.t("Profile avatar", "پروفائل تصویر"), profileAvatar, 56f..160f) { profileAvatar = it }
                V95ThemeSlider(c.t("Brand intensity", "نام کی شدت"), brandBrightness, 40f..180f) { brandBrightness = it }
            }
        }

        item {
            V95GlassCard {
                Text(c.t("Guest header buttons", "گسٹ ہیڈر بٹن"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                V95ThemeToggle(c.t("Show Login", "لاگ اِن دکھائیں"), guestLogin) { guestLogin = !guestLogin }
                V95ThemeToggle(c.t("Show Sign up", "رجسٹر دکھائیں"), guestRegister) { guestRegister = !guestRegister }
            }
        }

        item {
            V95GlassCard {
                Text(c.t("Header buttons", "ہیڈر بٹن"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                V95OrderEditor(c, headerOrder, headerEnabled)
            }
        }

        item {
            V95GlassCard {
                Text(c.t("Menu items", "مینیو آئٹمز"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                V95OrderEditor(c, menuOrder, menuEnabled)
            }
        }

        item {
            V95Button(c.t("Save complete theme", "مکمل تھیم محفوظ کریں"), Modifier.fillMaxWidth(), primary = true, icon = V95Icons.Palette) {
                c.saveThemeFields(
                    JSONObject()
                        .put("theme_card_radius", cardRadius.toInt())
                        .put("theme_nav_height", navHeight.toInt())
                        .put("theme_button_height", buttonHeight.toInt())
                        .put("theme_profile_cover_height", profileCover.toInt())
                        .put("theme_profile_avatar_size", profileAvatar.toInt())
                        .put("theme_brand_brightness", brandBrightness.toInt())
                        .put("theme_guest_auth_buttons", guestLogin || guestRegister)
                        .put("theme_guest_login_button", guestLogin)
                        .put("theme_guest_register_button", guestRegister)
                        .put("theme_header_items", headerOrder.filter { headerEnabled[it] == true }.joinToString(","))
                        .put("theme_menu_items", menuOrder.filter { menuEnabled[it] == true }.joinToString(","))
                )
            }
        }
    }
}

@Composable
private fun V95ThemeSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = V95Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(value.toInt().toString(), color = V95Muted, fontSize = 11.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValue, valueRange = range)
    }
}

@Composable
private fun V95ThemeToggle(label: String, value: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = .42f)).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = V95Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        V95Button(if (value) "ON" else "OFF", primary = value) { onClick() }
    }
}

@Composable
private fun V95OrderEditor(
    c: V95Controller,
    order: SnapshotStateList<String>,
    enabled: MutableMap<String, Boolean>
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        order.forEachIndexed { index, key ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .35f)).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(key.replaceFirstChar { it.uppercase() }, Modifier.weight(1f), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                V95Button(if (enabled[key] == true) c.t("Show", "دکھائیں") else c.t("Hidden", "چھپا"), primary = enabled[key] == true) {
                    enabled[key] = enabled[key] != true
                }
                Spacer(Modifier.width(4.dp))
                V95Button("↑", enabled = index > 0) {
                    if (index > 0) {
                        val item = order.removeAt(index)
                        order.add(index - 1, item)
                    }
                }
                Spacer(Modifier.width(3.dp))
                V95Button("↓", enabled = index < order.lastIndex) {
                    if (index < order.lastIndex) {
                        val item = order.removeAt(index)
                        order.add(index + 1, item)
                    }
                }
            }
        }
    }
}
