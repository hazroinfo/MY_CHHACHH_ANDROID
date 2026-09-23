package com.mychhachh.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import org.json.JSONObject

@Composable
fun ProfileScreen(
    data: JSONObject?,
    loading: Boolean,
    error: String?,
    meId: Long,
    onFollow: (Long) -> Unit,
    onMessage: (Long) -> Unit,
    onEdit: () -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit
) {
    val user = data?.optJSONObject("user")?.toUser()
    val posts = data?.optJSONArray("posts")?.posts().orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (loading && user == null) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        user?.let { u ->
            item {
                JellyGlass(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFE3F8FF), Color(0xFFF0E8FF))))
                        ) {
                            if (!u.cover.isNullOrBlank()) AsyncImage(u.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Column(Modifier.padding(horizontal = 14.dp).offset(y = (-30).dp)) {
                            Avatar(u, 76.dp)
                            Spacer(Modifier.height(5.dp))
                            UserName(u, 19)
                            if (u.username.isNotBlank()) Text("@${u.username}", color = JellyMuted, fontSize = 11.sp)
                            if (u.bio.isNotBlank()) Text(u.bio, color = JellyInk, fontSize = 12.5f.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 7.dp))
                            val loc = listOf(u.area, u.village, u.city).filter { it.isNotBlank() }.joinToString(" • ")
                            if (loc.isNotBlank()) {
                                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                    JellyIcon(JellyIcons.Pin, size = 21.dp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(loc, color = JellyMuted, fontSize = 10.sp)
                                }
                            }

                            val followers = data.optLong("followers", -1)
                            val following = data.optLong("following_count", -1)
                            if (followers >= 0 || following >= 0) {
                                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (followers >= 0) StatChip("$followers", "Followers")
                                    if (following >= 0) StatChip("$following", "Following")
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            if (u.id == meId) {
                                JellyButton("Edit profile", primary = true, icon = JellyIcons.Edit, onClick = onEdit)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton(
                                        if (data.optBoolean("following", false)) "Following" else "Follow",
                                        primary = !data.optBoolean("following", false),
                                        icon = JellyIcons.Follow
                                    ) { onFollow(u.id) }
                                    JellyButton("Message", icon = JellyIcons.Message) { onMessage(u.id) }
                                }
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Posts") }
            items(posts, key = { "profile-post-${it.id}" }) { p ->
                PostCard(p, true, {}, {}, onLike, onComment, onShare, onSave)
            }
            if (posts.isEmpty()) item { EmptyCard("No posts to show.", JellyIcons.Home) }
        }
    }
}

@Composable
private fun StatChip(number: String, label: String) {
    JellyGlass(radius = 14.dp, padding = 8.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(label, color = JellyMuted, fontSize = 8.5f.sp)
        }
    }
}

@Composable
fun ShopDetailScreen(
    data: JSONObject?,
    loading: Boolean,
    error: String?,
    meId: Long,
    onFollow: (Long) -> Unit,
    onProfile: (Long) -> Unit,
    onMessage: (Long) -> Unit,
    onUpdate: (Long, JSONObject, Uri?, Uri?) -> Unit,
    onDelete: (Long) -> Unit,
    onCreatePost: (Long, String, String, Uri?, Uri?) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit
) {
    val context = LocalContext.current
    val shop = data?.optJSONObject("shop")?.toShop() ?: data?.takeIf { it.has("name") }?.toShop()
    val posts = data?.optJSONArray("posts")?.posts().orEmpty()
    val followers = data?.optInt("followers", shop?.followers ?: 0) ?: 0
    val views = data?.optJSONObject("shop")?.optInt("views", 0) ?: 0
    var editOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }

    fun shareShop(id: Long, name: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://chhachh.pages.dev/shops.php?shop=$id")
        runCatching { context.startActivity(Intent.createChooser(intent, "Share $name")) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (loading && shop == null) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }

        shop?.let { s ->
            val own = s.userId == meId
            item {
                JellyGlass(Modifier.fillMaxWidth()) {
                    Column {
                        Box(
                            Modifier.fillMaxWidth().height(132.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFE3F8FF), Color(0xFFFFEAF5))))
                        ) {
                            s.cover?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            Row(
                                Modifier.align(Alignment.TopStart).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                JellyIcon(JellyIcons.Shop, size = 24.dp)
                                Spacer(Modifier.width(5.dp))
                                Text(s.category.ifBlank { "Local business" }, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }

                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-34).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(82.dp).clip(RoundedCornerShape(99.dp)).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!s.photo.isNullOrBlank()) AsyncImage(s.photo, s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else JellyIcon(JellyIcons.Shop, size = 54.dp)
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            if (s.username.isNotBlank()) Text("@${s.username}", color = JellyMuted, fontSize = 10.5f.sp)
                            if (s.category.isNotBlank()) Text(s.category, color = JellyMuted, fontSize = 10.5f.sp)
                            if (s.description.isNotBlank()) {
                                Text(
                                    s.description,
                                    Modifier.padding(top = 7.dp),
                                    color = JellyInk,
                                    fontSize = 12.5f.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatChip(posts.size.toString(), "Products")
                                StatChip(followers.toString(), "Followers")
                                StatChip(views.toString(), "Views")
                            }

                            val loc = listOf(s.location, s.area, s.village, s.city).filter { it.isNotBlank() }.joinToString(" • ")
                            if (loc.isNotBlank()) {
                                Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    JellyIcon(JellyIcons.Pin, size = 21.dp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(loc, color = JellyMuted, fontSize = 10.sp)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            if (own) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton("Edit Shop", Modifier.weight(1f), primary = true, icon = JellyIcons.Edit) { editOpen = true }
                                    JellyButton("Share", Modifier.weight(1f), icon = JellyIcons.Share) { shareShop(s.id, s.name) }
                                }
                                Spacer(Modifier.height(6.dp))
                                JellyButton("Delete Shop", Modifier.fillMaxWidth(), icon = JellyIcons.Delete) { deleteOpen = true }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton(
                                        if (data?.optBoolean("following", s.followed) == true) "Following" else "Follow Shop",
                                        Modifier.weight(1f),
                                        primary = data?.optBoolean("following", s.followed) != true,
                                        icon = JellyIcons.Follow
                                    ) { onFollow(s.id) }
                                    JellyButton("Message", Modifier.weight(1f), icon = JellyIcons.Message) { onMessage(s.userId) }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    if (s.whatsapp.isNotBlank()) {
                                        JellyButton("WhatsApp", Modifier.weight(1f), icon = JellyIcons.Whatsapp) {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${s.whatsapp.filter { it.isDigit() }}"))) }
                                        }
                                    }
                                    JellyButton("Share", Modifier.weight(1f), icon = JellyIcons.Share) { shareShop(s.id, s.name) }
                                }
                            }

                            if (s.phone.isNotBlank() || s.locationUrl.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    if (s.phone.isNotBlank()) {
                                        JellyButton("Call", Modifier.weight(1f), icon = JellyIcons.Phone) {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${s.phone}"))) }
                                        }
                                    }
                                    if (s.locationUrl.isNotBlank()) {
                                        JellyButton("Location", Modifier.weight(1f), icon = JellyIcons.Map) {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.locationUrl))) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (own) {
                item {
                    ShopPostComposer(s.id, onCreatePost)
                }
            }

            item { SectionTitle("Shop Posts") }
            if (posts.isEmpty()) item { EmptyCard("No shop posts yet.", JellyIcons.Shop) }
            items(posts, key = { "shop-post-${it.id}" }) { p ->
                PostCard(p, true, {}, { onProfile(p.user.id) }, onLike, onComment, onShare, onSave)
            }

            if (editOpen) {
                item {
                    ShopEditDialog(
                        shop = s,
                        onDismiss = { editOpen = false },
                        onSave = { fields, photo, cover ->
                            onUpdate(s.id, fields, photo, cover)
                            editOpen = false
                        }
                    )
                }
            }

            if (deleteOpen) {
                item {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { deleteOpen = false },
                        title = { Text("Delete Shop", color = JellyInk, fontWeight = FontWeight.Black) },
                        text = { Text("Delete this shop and all of its shop posts?", color = JellyInk) },
                        confirmButton = {
                            JellyButton("Delete Shop", primary = true, icon = JellyIcons.Delete) {
                                onDelete(s.id)
                                deleteOpen = false
                            }
                        },
                        dismissButton = { JellyButton("Cancel") { deleteOpen = false } }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopPostComposer(
    shopId: Long,
    onCreatePost: (Long, String, String, Uri?, Uri?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("public") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var video by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { photo = uri; video = null }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { video = uri; photo = null }
    }

    JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Shop Post", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("Publish an offer, product or update.", color = JellyMuted, fontSize = 10.sp)
            OutlinedTextField(
                text,
                { text = it },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Write shop post…") },
                minLines = 3,
                maxLines = 7,
                shape = RoundedCornerShape(18.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JellyButton(if (photo != null) "Photo ✓" else "Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { photoPicker.launch("image/*") }
                JellyButton(if (video != null) "Video ✓" else "Video", Modifier.weight(1f), icon = JellyIcons.Video) { videoPicker.launch("video/*") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                JellyButton(if (privacy == "public") "Everyone" else "Followers", icon = JellyIcons.Eye) {
                    privacy = if (privacy == "public") "followers" else "public"
                }
                Spacer(Modifier.weight(1f))
                JellyButton("Publish Shop Post", primary = true, icon = JellyIcons.Send, enabled = text.isNotBlank() || photo != null || video != null) {
                    onCreatePost(shopId, text.trim(), privacy, photo, video)
                    text = ""
                    photo = null
                    video = null
                }
            }
        }
    }
}

@Composable
private fun ShopEditDialog(
    shop: Shop,
    onDismiss: () -> Unit,
    onSave: (JSONObject, Uri?, Uri?) -> Unit
) {
    var name by remember(shop.id) { mutableStateOf(shop.name) }
    var username by remember(shop.id) { mutableStateOf(shop.username) }
    var category by remember(shop.id) { mutableStateOf(shop.category) }
    var phone by remember(shop.id) { mutableStateOf(shop.phone) }
    var whatsapp by remember(shop.id) { mutableStateOf(shop.whatsapp) }
    var city by remember(shop.id) { mutableStateOf(shop.city) }
    var village by remember(shop.id) { mutableStateOf(shop.village) }
    var area by remember(shop.id) { mutableStateOf(shop.area) }
    var location by remember(shop.id) { mutableStateOf(shop.location) }
    var locationLink by remember(shop.id) { mutableStateOf(shop.locationUrl) }
    var description by remember(shop.id) { mutableStateOf(shop.description) }
    var photo by remember(shop.id) { mutableStateOf<Uri?>(null) }
    var cover by remember(shop.id) { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) photo = it }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) cover = it }
    val usernameOk = username.matches(Regex("[a-z0-9_]{3,30}"))

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Shop Profile", color = JellyInk, fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), placeholder = { Text("Business name") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } }, Modifier.fillMaxWidth(), placeholder = { Text("Shop username") }, singleLine = true, isError = !usernameOk, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), placeholder = { Text("Category") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.fillMaxWidth(), placeholder = { Text("Phone") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(whatsapp, { whatsapp = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.fillMaxWidth(), placeholder = { Text("WhatsApp") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), placeholder = { Text("City") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(village, { village = it }, Modifier.fillMaxWidth(), placeholder = { Text("Village") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), placeholder = { Text("Mohalla") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), placeholder = { Text("Address") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(locationLink, { locationLink = it }, Modifier.fillMaxWidth(), placeholder = { Text("Google Maps Location Link") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(description, { description = it.take(2500) }, Modifier.fillMaxWidth(), placeholder = { Text("Description") }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(16.dp)) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        JellyButton(if (photo != null) "Photo ✓" else "Change Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { photoPicker.launch("image/*") }
                        JellyButton(if (cover != null) "Cover ✓" else "Change Cover", Modifier.weight(1f), icon = JellyIcons.Photo) { coverPicker.launch("image/*") }
                    }
                }
            }
        },
        confirmButton = {
            JellyButton("Save Shop Profile", primary = true, icon = JellyIcons.Check, enabled = name.isNotBlank() && usernameOk) {
                onSave(
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
            }
        },
        dismissButton = { JellyButton("Cancel", onClick = onDismiss) }
    )
}

@Composable
fun SettingsScreen(
    me: User,
    busy: Boolean,
    error: String?,
    blockedUsers: List<User>,
    onSave: (JSONObject, Uri?) -> Unit,
    onPrivacy: (JSONObject) -> Unit,
    onLocation: (Double, Double) -> Unit,
    onPassword: (String, String) -> Unit,
    onRefreshBlocked: () -> Unit,
    onUnblock: (Long) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(me.id, me.name) { mutableStateOf(me.name) }
    var username by remember(me.id, me.username) { mutableStateOf(me.username) }
    var email by remember(me.id, me.email) { mutableStateOf(me.email) }
    var phone by remember(me.id, me.phone) { mutableStateOf(me.phone) }
    var city by remember(me.id, me.city) { mutableStateOf(me.city) }
    var village by remember(me.id, me.village) { mutableStateOf(me.village) }
    var area by remember(me.id, me.area) { mutableStateOf(me.area) }
    var bio by remember(me.id, me.bio) { mutableStateOf(me.bio) }
    var showEmail by remember(me.id, me.showEmail) { mutableStateOf(me.showEmail) }
    var showPhone by remember(me.id, me.showPhone) { mutableStateOf(me.showPhone) }
    var showLocation by remember(me.id, me.showLocation) { mutableStateOf(me.showLocation) }
    var hideFollowers by remember(me.id, me.hideFollowers) { mutableStateOf(me.hideFollowers) }
    var privateProfile by remember(me.id, me.profileVisibility) { mutableStateOf(me.profileVisibility.equals("followers", true)) }
    var acceptMessages by remember(me.id, me.acceptMessages) { mutableStateOf(me.acceptMessages) }
    var avatarUri by remember(me.id) { mutableStateOf<Uri?>(null) }
    var passwordOpen by remember { mutableStateOf(false) }
    var blockedOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) avatarUri = uri
    }

    fun sendLastKnownLocation() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.asSequence()
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider ->
                runCatching {
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) manager.getLastKnownLocation(provider) else null
                }.getOrNull()
            }
            .maxByOrNull { it.time }
        if (location != null) onLocation(location.latitude, location.longitude)
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) sendLastKnownLocation()
    }

    val usernameOk = username.matches(Regex("[a-z0-9_]{3,30}"))
    val emailOk = email.isBlank() || email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
    val phoneOk = phone.isBlank() || phone.matches(Regex("\\+?[0-9]{7,15}"))
    val canSave = name.trim().length >= 2 && usernameOk && emailOk && phoneOk && !busy

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Settings & Privacy", "Manage your privacy and experience", JellyIcons.Gear) }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Profile")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (avatarUri != null) {
                            Box(Modifier.size(66.dp).clip(RoundedCornerShape(99.dp))) {
                                AsyncImage(avatarUri, "Selected profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        } else Avatar(me, 66.dp)
                        Spacer(Modifier.width(10.dp))
                        JellyButton("Choose Photo", icon = JellyIcons.Photo) { avatarPicker.launch("image/*") }
                    }
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(
                        username,
                        { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                        Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        isError = !usernameOk,
                        supportingText = { if (!usernameOk) Text("3–30 characters: a-z, 0-9 or underscore") },
                        shape = RoundedCornerShape(17.dp),
                        singleLine = true
                    )
                    OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Email") }, isError = !emailOk, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(
                        phone,
                        { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Phone") },
                        isError = !phoneOk,
                        shape = RoundedCornerShape(17.dp),
                        singleLine = true
                    )
                    OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text("City") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(village, { village = it }, Modifier.fillMaxWidth(), label = { Text("Village") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), label = { Text("Mohallah / Area") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(bio, { bio = it }, Modifier.fillMaxWidth(), label = { Text("Bio") }, shape = RoundedCornerShape(17.dp), minLines = 3, maxLines = 6)

                    JellyButton(
                        if (busy) "Saving…" else "Save Profile",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Check,
                        enabled = canSave
                    ) {
                        onSave(
                            JSONObject()
                                .put("name", name.trim())
                                .put("username", username.trim())
                                .put("email", email.trim())
                                .put("phone", phone.trim())
                                .put("city", city.trim())
                                .put("village", village.trim())
                                .put("area", area.trim())
                                .put("bio", bio.trim()),
                            avatarUri
                        )
                    }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SectionTitle("Privacy")
                    PrivacySwitch("Show my email on my profile", showEmail) { showEmail = it }
                    PrivacySwitch("Show my phone number on my profile", showPhone) { showPhone = it }
                    PrivacySwitch("Private profile (followers only)", privateProfile) { privateProfile = it }
                    PrivacySwitch("Hide followers and following counts", hideFollowers) { hideFollowers = it }
                    PrivacySwitch("Accept messages", acceptMessages) { acceptMessages = it }
                    PrivacySwitch("Show current location", showLocation) { checked ->
                        showLocation = checked
                        if (checked) {
                            val granted =
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (granted) sendLastKnownLocation()
                            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                    JellyButton("Save Privacy", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Shield) {
                        onPrivacy(
                            JSONObject()
                                .put("show_email", showEmail)
                                .put("show_phone", showPhone)
                                .put("show_location", showLocation)
                                .put("hide_followers", hideFollowers)
                                .put("accept_messages", acceptMessages)
                                .put("profile_visibility", if (privateProfile) "followers" else "public")
                        )
                    }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Password & Security")
                    JellyButton("Change Password", Modifier.fillMaxWidth(), icon = JellyIcons.Lock) { passwordOpen = true }
                    JellyButton("Blocked Users (${blockedUsers.size})", Modifier.fillMaxWidth(), icon = JellyIcons.People) {
                        blockedOpen = true
                        onRefreshBlocked()
                    }
                }
            }
        }

        error?.let { item { ErrorCard(it) } }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Account")
                    JellyButton("Logout", Modifier.fillMaxWidth(), icon = JellyIcons.Logout, onClick = onLogout)
                    JellyButton("Delete Account", Modifier.fillMaxWidth(), icon = JellyIcons.Delete) { deleteOpen = true }
                }
            }
        }
    }

    if (passwordOpen) {
        var current by remember { mutableStateOf("") }
        var next by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { passwordOpen = false },
            title = { Text("Change Password", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(current, { current = it }, Modifier.fillMaxWidth(), label = { Text("Current password") }, shape = RoundedCornerShape(17.dp))
                    OutlinedTextField(next, { next = it }, Modifier.fillMaxWidth(), label = { Text("New password") }, supportingText = { Text("At least 6 characters") }, shape = RoundedCornerShape(17.dp))
                }
            },
            confirmButton = {
                JellyButton("Change Password", primary = true, icon = JellyIcons.Lock, enabled = current.isNotBlank() && next.length >= 6) {
                    onPassword(current, next)
                    passwordOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { passwordOpen = false } }
        )
    }

    if (blockedOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { blockedOpen = false },
            title = { Text("Blocked Users", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (blockedUsers.isEmpty()) item { Text("No blocked users.", color = JellyMuted) }
                    items(blockedUsers, key = { "blocked-${it.id}" }) { user ->
                        JellyGlass(Modifier.fillMaxWidth(), padding = 8.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(user, 38.dp)
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    UserName(user, 11)
                                    Text("@${user.username}", color = JellyMuted, fontSize = 9.sp)
                                }
                                JellyButton("Unblock") { onUnblock(user.id) }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { blockedOpen = false } }
        )
    }

    if (deleteOpen) {
        var password by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Delete Account", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This permanently deletes your account and its data.", color = Color(0xFFB23A55), fontWeight = FontWeight.Bold)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, shape = RoundedCornerShape(17.dp))
                }
            },
            confirmButton = {
                JellyButton("Delete Account", primary = true, icon = JellyIcons.Delete, enabled = password.isNotBlank()) {
                    onDeleteAccount(password)
                    deleteOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteOpen = false } }
        )
    }
}

@Composable
private fun PrivacySwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
