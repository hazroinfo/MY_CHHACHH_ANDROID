package com.mychhachh.app.ui.screens

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
    val myShop = shops.firstOrNull { it.userId == meId }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PageTitle("Shops", "Discover useful shops and services across Chhachh", JellyIcons.Shop) }
                if (myShop != null) {
                    JellyButton("My Shop", primary = true, icon = JellyIcons.Shop) { onOpen(myShop.id) }
                } else {
                    JellyButton("Create Shop", primary = true, icon = JellyIcons.Plus) { createOpen = true }
                }
            }
        }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.weight(1f),
                        placeholder = { Text("Search shops, category or village…") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    JellyButton("Search", icon = JellyIcons.Search, onClick = onSearch)
                }
            }
        }
        if (loading && shops.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onSearch) } }
        if (!loading && shops.isEmpty() && error == null) item { EmptyCard("No shops found.", JellyIcons.Shop) }
        items(shops, key = { "shop-${it.id}" }) { s ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(RoundedCornerShape(99.dp)).clickable { onOpen(s.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!s.photo.isNullOrBlank()) {
                            AsyncImage(s.photo, s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else JellyIcon(JellyIcons.Shop, size = 42.dp)
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { onOpen(s.id) }) {
                        Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.5f.sp)
                        if (s.username.isNotBlank()) Text("@${s.username}", color = JellyMuted, fontSize = 10.sp)
                        if (s.category.isNotBlank()) Text(s.category, color = JellyMuted, fontSize = 9.5f.sp)
                        val loc = listOf(s.area, s.village, s.city).filter { it.isNotBlank() }.joinToString(" • ")
                        if (loc.isNotBlank()) Text(loc, color = JellyMuted, fontSize = 9.5f.sp)
                    }
                    if (s.userId != meId) {
                        JellyButton(
                            if (s.followed) "Following" else "Follow",
                            primary = !s.followed,
                            icon = JellyIcons.Follow
                        ) { onFollow(s.id) }
                    }
                }
            }
        }
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
                    item { OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.fillMaxWidth(), placeholder = { Text("Phone") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(whatsapp, { whatsapp = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.fillMaxWidth(), placeholder = { Text("WhatsApp with country code") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), placeholder = { Text("City") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(village, { village = it }, Modifier.fillMaxWidth(), placeholder = { Text("Village") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), placeholder = { Text("Mohalla") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), placeholder = { Text("Address") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(locationLink, { locationLink = it }, Modifier.fillMaxWidth(), placeholder = { Text("Google Maps location link") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                    item { OutlinedTextField(description, { description = it.take(2500) }, Modifier.fillMaxWidth(), placeholder = { Text("Description") }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(16.dp)) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            JellyButton(if (photo != null) "Photo ✓" else "Shop Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { photoPicker.launch("image/*") }
                            JellyButton(if (cover != null) "Cover ✓" else "Cover Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { coverPicker.launch("image/*") }
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
