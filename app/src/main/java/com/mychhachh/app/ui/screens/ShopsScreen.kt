package com.mychhachh.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.Shop
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import org.json.JSONObject

@Composable
fun ShopsScreen(
    meId: Long,
    shops: List<Shop>,
    loading: Boolean,
    error: String?,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (Long) -> Unit,
    onFollow: (Long) -> Unit,
    onCreate: (JSONObject, Uri?, Uri?) -> Unit
) {
    var createOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val myShop = shops.firstOrNull { it.userId == meId }
    val normalized = query.trim().lowercase()
    val filtered = if (normalized.isBlank()) shops else shops.filter { sh ->
        listOf(sh.name, sh.username, sh.category, sh.city, sh.village, sh.area)
            .joinToString(" ")
            .lowercase()
            .contains(normalized)
    }
    val suggestions = shops
        .sortedWith(
            compareByDescending<Shop> { it.promoted }
                .thenByDescending { if (it.photo.isNullOrBlank()) 0 else 1 }
                .thenByDescending { if (it.whatsapp.isBlank()) 0 else 1 }
                .thenBy { it.name.lowercase() }
        )
        .take(6)

    fun openWhatsApp(number: String) {
        val digits = number.filter(Char::isDigit)
        if (digits.isBlank()) return
        val uri = Uri.parse("https://wa.me/$digits")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Shops", "Discover useful shops and services across Chhachh", JellyIcons.Shop) }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Shop Suggestions", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Local businesses you may want to visit", color = JellyMuted, fontSize = 9.5f.sp)
                        }
                        if (myShop != null) {
                            JellyButton("My Shop", primary = true, icon = JellyIcons.Shop) { onOpen(myShop.id) }
                        } else {
                            JellyButton("Create Shop", primary = true, icon = JellyIcons.Plus) { createOpen = true }
                        }
                    }

                    JellyGlass(
                        Modifier.fillMaxWidth().height(52.dp),
                        radius = 18.dp,
                        padding = 8.dp,
                        surfaceColor = com.mychhachh.app.ui.theme.LiveJellyTheme.inputColor,
                        surfaceOpacity = com.mychhachh.app.ui.theme.LiveJellyTheme.inputOpacity
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            JellyIcon(JellyIcons.Search, size = 26.dp)
                            Spacer(Modifier.width(6.dp))
                            androidx.compose.material3.OutlinedTextField(
                                query,
                                {
                                    onQuery(it)
                                    if (it.isBlank()) onSearch()
                                },
                                Modifier.weight(1f),
                                placeholder = { Text("Search shops, category or village…") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    if (suggestions.isEmpty()) {
                        Text("No shop suggestions right now.", color = JellyMuted, fontSize = 10.sp)
                    } else {
                        suggestions.forEach { sh ->
                            val place = listOf(sh.village, sh.city).filter { it.isNotBlank() }.joinToString(" · ")
                            val reason = when {
                                sh.promoted -> "Featured"
                                sh.village.isNotBlank() -> "Near your village"
                                sh.area.isNotBlank() -> "Near you"
                                sh.city.isNotBlank() -> "In your city"
                                sh.category.isNotBlank() -> sh.category
                                else -> "Suggested shop"
                            }
                            JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 10.dp) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .clickable { onOpen(sh.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!sh.photo.isNullOrBlank()) {
                                            AsyncImage(sh.photo, sh.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            JellyIcon(JellyIcons.Shop, size = 40.dp)
                                        }
                                    }
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f).clickable { onOpen(sh.id) }) {
                                        Text(sh.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        if (sh.username.isNotBlank()) Text("@${sh.username}", color = JellyMuted, fontSize = 9.5f.sp)
                                        val categoryPlace = listOf(sh.category, place).filter { it.isNotBlank() }.joinToString(" · ")
                                        if (categoryPlace.isNotBlank()) Text(categoryPlace, color = JellyMuted, fontSize = 9.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            JellyIcon(if (sh.promoted) JellyIcons.Star else JellyIcons.Pin, size = 16.dp)
                                            Spacer(Modifier.width(3.dp))
                                            Text(reason, color = JellyMuted, fontSize = 8.5f.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        JellyButton("View Shop", icon = JellyIcons.Eye) { onOpen(sh.id) }
                                        if (sh.whatsapp.isNotBlank()) {
                                            JellyButton("WhatsApp", icon = JellyIcons.Whatsapp) { openWhatsApp(sh.whatsapp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("All Shops", Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(filtered.size.toString(), color = JellyMuted, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    if (loading && shops.isEmpty()) {
                        LoadingBlock()
                    } else if (filtered.isEmpty() && error == null) {
                        Text("No shops yet.", color = JellyMuted, fontSize = 10.sp)
                    } else {
                        filtered.forEach { sh ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .clickable { onOpen(sh.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!sh.photo.isNullOrBlank()) {
                                        AsyncImage(sh.photo, sh.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        JellyIcon(JellyIcons.Shop, size = 34.dp)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f).clickable { onOpen(sh.id) }) {
                                    Text(
                                        sh.name + if (sh.promoted) " · Promoted" else "",
                                        color = JellyInk,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    val line = listOf(
                                        if (sh.username.isNotBlank()) "@${sh.username}" else "",
                                        sh.category.ifBlank { "Shop" }
                                    ).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (line.isNotBlank()) Text(line, color = JellyMuted, fontSize = 9.sp)
                                    val place = listOf(sh.village, sh.city).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (place.isNotBlank()) Text(place, color = JellyMuted, fontSize = 8.5f.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    JellyButton("View", icon = JellyIcons.Eye) { onOpen(sh.id) }
                                    if (sh.whatsapp.isNotBlank()) {
                                        JellyButton("WhatsApp", icon = JellyIcons.Whatsapp) { openWhatsApp(sh.whatsapp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        error?.let { item { ErrorCard(it, onSearch) } }
    }

    if (createOpen) {
        var name by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var whatsapp by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var village by remember { mutableStateOf("") }
        var area by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var locationLink by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var photo by remember { mutableStateOf<Uri?>(null) }
        var cover by remember { mutableStateOf<Uri?>(null) }
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) photo = it }
        val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) cover = it }
        val usernameOk = username.matches(Regex("[a-z0-9_]{3,30}"))

        AlertDialog(
            onDismissRequest = { createOpen = false },
            title = { Text("Create Shop", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(
                    Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), placeholder = { Text("Business name") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } }, Modifier.fillMaxWidth(), placeholder = { Text("Shop username") }, singleLine = true, isError = username.isNotBlank() && !usernameOk, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), placeholder = { Text("Category") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.fillMaxWidth(), placeholder = { Text("Phone (+ and digits only)") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(whatsapp, { whatsapp = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.fillMaxWidth(), placeholder = { Text("WhatsApp (+ and country code)") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), placeholder = { Text("City") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(village, { village = it }, Modifier.fillMaxWidth(), placeholder = { Text("Village") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), placeholder = { Text("Mohalla") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), placeholder = { Text("Address") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(locationLink, { locationLink = it }, Modifier.fillMaxWidth(), placeholder = { Text("Google Maps Location Link") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(description, { description = it.take(2500) }, Modifier.fillMaxWidth(), placeholder = { Text("Description") }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(16.dp)) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            JellyButton(if (photo != null) "Photo ✓" else "Upload Profile Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { photoPicker.launch("image/*") }
                            JellyButton(if (cover != null) "Cover ✓" else "Upload Cover Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { coverPicker.launch("image/*") }
                        }
                    }
                }
            },
            confirmButton = {
                JellyButton("Create Shop", primary = true, icon = JellyIcons.Shop, enabled = name.trim().isNotBlank() && usernameOk) {
                    onCreate(
                        JSONObject()
                            .put("name", name.trim())
                            .put("username", username.trim())
                            .put("category", category.trim())
                            .put("phone", phone.trim())
                            .put("whatsapp", whatsapp.trim())
                            .put("city", city.trim())
                            .put("village", village.trim())
                            .put("area", area.trim())
                            .put("location", location.trim())
                            .put("location_link", locationLink.trim())
                            .put("description", description.trim()),
                        photo,
                        cover
                    )
                    createOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { createOpen = false } }
        )
    }
}
