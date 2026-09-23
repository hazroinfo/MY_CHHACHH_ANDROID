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
import androidx.compose.foundation.clickable
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
import com.mychhachh.app.ui.theme.JellyGreen
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import org.json.JSONObject
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    data: JSONObject?,
    loading: Boolean,
    error: String?,
    meId: Long,
    onFollow: (Long) -> Unit,
    onMessage: (Long) -> Unit,
    onEdit: () -> Unit,
    onShop: (Long) -> Unit,
    onBlock: (Long) -> Unit,
    onReport: (Long, String) -> Unit,
    onLoadRelations: suspend (Long, String) -> List<User>,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = data?.optJSONObject("user")?.toUser()
    val posts = data?.optJSONArray("posts")?.posts().orEmpty()
    val shop = data?.optJSONObject("shop")?.toShop()
    var relationMode by remember { mutableStateOf<String?>(null) }
    var relationUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var relationLoading by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        val value = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }
    }

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
                            Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFE3F8FF), Color(0xFFF0E8FF))))
                        ) {
                            if (!u.cover.isNullOrBlank()) AsyncImage(u.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-44).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Avatar(u, 96.dp)
                            Spacer(Modifier.height(6.dp))
                            UserName(u, 20)
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

                            val detailParts = listOf(
                                u.hometown.takeIf { it.isNotBlank() }?.let { "Hometown: $it" },
                                u.work.takeIf { it.isNotBlank() }?.let { "Work: $it" },
                                u.school.takeIf { it.isNotBlank() }?.let { "School: $it" },
                                u.gender.takeIf { it.isNotBlank() }?.let { "Gender: $it" },
                                u.relationshipStatus.takeIf { it.isNotBlank() }?.let { it }
                            ).filterNotNull()
                            detailParts.forEach {
                                Text(it, color = JellyMuted, fontSize = 9.5f.sp, modifier = Modifier.padding(top = 3.dp))
                            }

                            if (u.email.isNotBlank()) {
                                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    JellyIcon(JellyIcons.Mail, size = 20.dp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(u.email, color = JellyMuted, fontSize = 9.5f.sp)
                                }
                            }
                            if (u.phone.isNotBlank()) {
                                JellyButton(u.phone, icon = JellyIcons.Phone) {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${u.phone}"))) }
                                }
                            }
                            if (u.locationLat != null && u.locationLng != null) {
                                JellyButton("Current Location", icon = JellyIcons.Map) {
                                    val uri = Uri.parse("geo:${u.locationLat},${u.locationLng}?q=${u.locationLat},${u.locationLng}")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                }
                            }

                            val socials = listOf(
                                "Facebook" to u.socialFacebook,
                                "Instagram" to u.socialInstagram,
                                "YouTube" to u.socialYoutube,
                                "Website" to u.socialWebsite
                            ).filter { it.second.isNotBlank() }
                            if (socials.isNotEmpty()) {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    socials.take(4).forEach { (label, url) ->
                                        JellyButton(label, Modifier.weight(1f)) { openUrl(url) }
                                    }
                                }
                            }

                            val followers = data.optLong("followers", -1)
                            val following = data.optLong("following_count", -1)
                            if (followers >= 0 || following >= 0) {
                                Row(
                                    Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (followers >= 0) {
                                        StatChip(followers.toString(), "Followers") {
                                            relationMode = "followers"
                                            scope.launch {
                                                relationLoading = true
                                                relationUsers = runCatching { onLoadRelations(u.id, "followers") }.getOrDefault(emptyList())
                                                relationLoading = false
                                            }
                                        }
                                    }
                                    if (following >= 0) {
                                        StatChip(following.toString(), "Following") {
                                            relationMode = "following"
                                            scope.launch {
                                                relationLoading = true
                                                relationUsers = runCatching { onLoadRelations(u.id, "following") }.getOrDefault(emptyList())
                                                relationLoading = false
                                            }
                                        }
                                    }
                                }
                            }

                            if (shop != null) {
                                Spacer(Modifier.height(8.dp))
                                JellyButton("View Shop", primary = true, icon = JellyIcons.Shop) { onShop(shop.id) }
                            }

                            Spacer(Modifier.height(10.dp))
                            if (u.id == meId) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton("Edit Profile", Modifier.weight(1f), primary = true, icon = JellyIcons.Edit, onClick = onEdit)
                                    JellyButton("Settings", Modifier.weight(1f), icon = JellyIcons.Gear, onClick = onEdit)
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton(
                                        if (data.optBoolean("following", false)) "Following" else "Follow",
                                        Modifier.weight(1f),
                                        primary = !data.optBoolean("following", false),
                                        icon = JellyIcons.Follow
                                    ) { onFollow(u.id) }
                                    JellyButton("Message", Modifier.weight(1f), icon = JellyIcons.Message) { onMessage(u.id) }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton(
                                        if (data.optBoolean("blocked", false)) "Unblock" else "Block",
                                        Modifier.weight(1f),
                                        icon = JellyIcons.Shield
                                    ) { onBlock(u.id) }
                                    JellyButton("Report", Modifier.weight(1f), icon = JellyIcons.Shield) { reportOpen = true }
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

            if (reportOpen) {
                item {
                    var reason by remember(u.id) { mutableStateOf("") }
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { reportOpen = false },
                        title = { Text("Report Profile", color = JellyInk, fontWeight = FontWeight.Black) },
                        text = {
                            OutlinedTextField(
                                reason,
                                { reason = it.take(3000) },
                                Modifier.fillMaxWidth(),
                                placeholder = { Text("Explain the problem…") },
                                minLines = 4,
                                maxLines = 8,
                                shape = RoundedCornerShape(18.dp)
                            )
                        },
                        confirmButton = {
                            JellyButton("Send Report", primary = true, icon = JellyIcons.Shield, enabled = reason.trim().length >= 3) {
                                onReport(u.id, reason.trim())
                                reportOpen = false
                            }
                        },
                        dismissButton = { JellyButton("Cancel") { reportOpen = false } }
                    )
                }
            }
        }
    }

    relationMode?.let { mode ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { relationMode = null },
            title = { Text(if (mode == "followers") "Followers" else "Following", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (relationLoading) item { LoadingBlock() }
                    if (!relationLoading && relationUsers.isEmpty()) item { Text("No users to show.", color = JellyMuted) }
                    items(relationUsers, key = { "relation-${it.id}" }) { person ->
                        JellyGlass(Modifier.fillMaxWidth(), padding = 8.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(person, 38.dp)
                                Spacer(Modifier.width(7.dp))
                                Column {
                                    UserName(person, 11)
                                    Text("@${person.username}", color = JellyMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { relationMode = null } }
        )
    }
}

@Composable
private fun StatChip(number: String, label: String, onClick: (() -> Unit)? = null) {
    JellyGlass(radius = 14.dp, padding = 8.dp, onClick = onClick) {
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
    verification: JSONObject?,
    supportTickets: List<JSONObject>,
    onSave: (JSONObject, Uri?) -> Unit,
    onPrivacy: (JSONObject) -> Unit,
    onLocation: (Double, Double) -> Unit,
    onPassword: (String, String) -> Unit,
    onSubmitVerification: (String, String, Uri, Uri?, Uri) -> Unit,
    onRefreshBlocked: () -> Unit,
    onUnblock: (Long) -> Unit,
    onSubmitSupport: (String, String, String) -> Unit,
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
    var hometown by remember(me.id, me.hometown) { mutableStateOf(me.hometown) }
    var gender by remember(me.id, me.gender) { mutableStateOf(me.gender) }
    var relationshipStatus by remember(me.id, me.relationshipStatus) { mutableStateOf(me.relationshipStatus) }
    var work by remember(me.id, me.work) { mutableStateOf(me.work) }
    var school by remember(me.id, me.school) { mutableStateOf(me.school) }
    var facebook by remember(me.id, me.socialFacebook) { mutableStateOf(me.socialFacebook) }
    var instagram by remember(me.id, me.socialInstagram) { mutableStateOf(me.socialInstagram) }
    var youtube by remember(me.id, me.socialYoutube) { mutableStateOf(me.socialYoutube) }
    var website by remember(me.id, me.socialWebsite) { mutableStateOf(me.socialWebsite) }
    var showEmail by remember(me.id, me.showEmail) { mutableStateOf(me.showEmail) }
    var showPhone by remember(me.id, me.showPhone) { mutableStateOf(me.showPhone) }
    var showLocation by remember(me.id, me.showLocation) { mutableStateOf(me.showLocation) }
    var hideFollowers by remember(me.id, me.hideFollowers) { mutableStateOf(me.hideFollowers) }
    var privateProfile by remember(me.id, me.profileVisibility) { mutableStateOf(me.profileVisibility.equals("followers", true)) }
    var acceptMessages by remember(me.id, me.acceptMessages) { mutableStateOf(me.acceptMessages) }
    var avatarUri by remember(me.id) { mutableStateOf<Uri?>(null) }
    var passwordOpen by remember { mutableStateOf(false) }
    var blockedOpen by remember { mutableStateOf(false) }
    var verifyPhone by remember(me.id, me.phone) { mutableStateOf(me.phone) }
    var verifyType by remember { mutableStateOf("id_card") }
    var verifyFront by remember { mutableStateOf<Uri?>(null) }
    var verifyBack by remember { mutableStateOf<Uri?>(null) }
    var verifySelfie by remember { mutableStateOf<Uri?>(null) }
    var deleteOpen by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) avatarUri = uri
    }
    val verifyFrontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) verifyFront = it }
    val verifyBackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) verifyBack = it }
    val verifySelfiePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) verifySelfie = it }

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
            SettingsGroupHeader(
                title = "Privacy",
                subtitle = "Control who can see your information",
                icon = JellyIcons.Shield,
                expanded = settingsSection == "privacy"
            ) { settingsSection = if (settingsSection == "privacy") null else "privacy" }
        }

        if (settingsSection == "privacy") item {
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
                    JellyButton("Change Password", Modifier.fillMaxWidth(), icon = JellyIcons.Lock) { passwordOpen = true }
                    JellyButton("Blocked Users (${blockedUsers.size})", Modifier.fillMaxWidth(), icon = JellyIcons.People) {
                        blockedOpen = true
                        onRefreshBlocked()
                    }
                    JellyButton("Save Changes", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Shield) {
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
            SettingsGroupHeader(
                title = "Notifications",
                subtitle = "Choose what updates you receive",
                icon = JellyIcons.Bell,
                expanded = settingsSection == "notifications"
            ) { settingsSection = if (settingsSection == "notifications") null else "notifications" }
        }

        if (settingsSection == "notifications") item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Notifications")
                    PrivacySwitch("Allow messages from people", acceptMessages) { acceptMessages = it }
                    JellyButton("Save Changes", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check) {
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
            SettingsGroupHeader(
                title = "Language",
                subtitle = "Choose your preferred language",
                icon = JellyIcons.Address,
                expanded = settingsSection == "language"
            ) { settingsSection = if (settingsSection == "language") null else "language" }
        }

        if (settingsSection == "language") item {
            val prefs = context.getSharedPreferences("my_chhachh_native", Context.MODE_PRIVATE)
            var language by remember { mutableStateOf(prefs.getString("language", "en") ?: "en") }
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("Language")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JellyPill("English", language == "en", Modifier.weight(1f)) {
                            language = "en"
                            prefs.edit().putString("language", "en").apply()
                        }
                        JellyPill("اردو", language == "ur", Modifier.weight(1f)) {
                            language = "ur"
                            prefs.edit().putString("language", "ur").apply()
                        }
                    }
                    Text("Language preference is saved on this device.", color = JellyMuted, fontSize = 9.5f.sp)
                }
            }
        }


        error?.let { item { ErrorCard(it) } }

        item {
            SettingsGroupHeader(
                title = "Verification",
                subtitle = "Secure your identity and media access",
                icon = JellyIcons.Shield,
                expanded = settingsSection == "verification"
            ) { settingsSection = if (settingsSection == "verification") null else "verification" }
        }

        if (settingsSection == "verification") item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Identity Verification")
                    val status = verification?.optString("status", "not_submitted") ?: "not_submitted"
                    Text(
                        when (status) {
                            "approved" -> "Verified ✓"
                            "pending" -> "Verification is pending admin review."
                            "rejected" -> "Verification was rejected. You can submit again."
                            else -> "Verify identity to unlock media features when required by admin policy."
                        },
                        color = if (status == "approved") JellyGreen else JellyMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5f.sp
                    )
                    verification?.optJSONObject("request")?.optString("admin_note", "")?.takeIf { it.isNotBlank() }?.let {
                        Text("Admin note: $it", color = Color(0xFFB23A55), fontSize = 9.5f.sp)
                    }

                    if (status != "approved") {
                        OutlinedTextField(
                            verifyPhone,
                            { verifyPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) },
                            Modifier.fillMaxWidth(),
                            label = { Text("Phone number") },
                            shape = RoundedCornerShape(17.dp),
                            singleLine = true
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            JellyPill("ID Card", verifyType == "id_card", Modifier.weight(1f)) { verifyType = "id_card" }
                            JellyPill("Passport", verifyType == "passport", Modifier.weight(1f)) { verifyType = "passport" }
                        }
                        JellyButton(
                            if (verifyFront != null) "Front / Passport ✓" else "Front / Passport",
                            Modifier.fillMaxWidth(),
                            icon = JellyIcons.Photo
                        ) { verifyFrontPicker.launch("*/*") }
                        if (verifyType == "id_card") {
                            JellyButton(
                                if (verifyBack != null) "ID Back ✓" else "ID Back",
                                Modifier.fillMaxWidth(),
                                icon = JellyIcons.Photo
                            ) { verifyBackPicker.launch("*/*") }
                        }
                        JellyButton(
                            if (verifySelfie != null) "Selfie ✓" else "Selfie",
                            Modifier.fillMaxWidth(),
                            icon = JellyIcons.User
                        ) { verifySelfiePicker.launch("image/*") }

                        val verifyReady = verifyPhone.matches(Regex("\\+?[0-9]{7,15}")) &&
                            verifyFront != null && verifySelfie != null &&
                            (verifyType == "passport" || verifyBack != null)
                        JellyButton(
                            if (busy) "Submitting…" else "Submit Verification",
                            Modifier.fillMaxWidth(),
                            primary = true,
                            icon = JellyIcons.Shield,
                            enabled = verifyReady && !busy
                        ) {
                            onSubmitVerification(
                                verifyPhone,
                                verifyType,
                                verifyFront!!,
                                if (verifyType == "id_card") verifyBack else null,
                                verifySelfie!!
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroupHeader(
                title = "Help & Support",
                subtitle = "Private support tickets with Chhachh Team",
                icon = JellyIcons.Comment,
                expanded = settingsSection == "help"
            ) { settingsSection = if (settingsSection == "help") null else "help" }
        }

        if (settingsSection == "help") item {
            var category by remember { mutableStateOf("help") }
            var subject by remember { mutableStateOf("") }
            var supportMessage by remember { mutableStateOf("") }
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("Help & Support")
                    Text(
                        "Send a problem, safety report or feedback directly to the Chhachh Team.",
                        color = JellyMuted,
                        fontSize = 10.sp
                    )
                    JellyButton(
                        when (category) {
                            "technical" -> "Technical problem"
                            "account" -> "Account problem"
                            "safety" -> "Safety / abuse"
                            "feedback" -> "Feedback"
                            "other" -> "Other"
                            else -> "General help"
                        },
                        Modifier.fillMaxWidth(),
                        icon = JellyIcons.Info
                    ) {
                        category = when (category) {
                            "help" -> "technical"
                            "technical" -> "account"
                            "account" -> "safety"
                            "safety" -> "feedback"
                            "feedback" -> "other"
                            else -> "help"
                        }
                    }
                    OutlinedTextField(
                        subject,
                        { subject = it.take(120) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Subject") },
                        shape = RoundedCornerShape(17.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        supportMessage,
                        { supportMessage = it.take(3000) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Message") },
                        shape = RoundedCornerShape(17.dp),
                        minLines = 4,
                        maxLines = 8
                    )
                    JellyButton(
                        if (busy) "Sending…" else "Send to Chhachh Team",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Comment,
                        enabled = !busy && subject.trim().isNotBlank() && supportMessage.trim().length >= 3
                    ) {
                        onSubmitSupport(category, subject.trim(), supportMessage.trim())
                        subject = ""
                        supportMessage = ""
                    }
                    if (supportTickets.isNotEmpty()) {
                        SectionTitle("My Support Requests")
                        supportTickets.take(12).forEach { ticket ->
                            JellyGlass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 9.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(ticket.optString("subject", "Help & Support"), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Text(ticket.optString("reason", ""), color = JellyInk, fontSize = 9.5f.sp, maxLines = 4)
                                    Text(ticket.optString("status", "open"), color = JellyMuted, fontSize = 8.5f.sp)
                                    ticket.optString("admin_reply", "").takeIf { it.isNotBlank() }?.let { reply ->
                                        Text("Chhachh Team reply", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                        Text(reply, color = JellyMuted, fontSize = 9.sp, maxLines = 5)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsGroupHeader(
                title = "Delete Account",
                subtitle = "Permanently remove your account",
                icon = JellyIcons.Logout,
                expanded = settingsSection == "delete",
                danger = true
            ) { settingsSection = if (settingsSection == "delete") null else "delete" }
        }

        if (settingsSection == "delete") item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Delete Account")
                    Text("Permanently remove your account.", color = JellyMuted, fontSize = 10.sp)
                    JellyButton("Delete Account", Modifier.fillMaxWidth(), icon = JellyIcons.Delete, danger = true) {
                        deleteOpen = true
                    }
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
                JellyButton("Delete Account", icon = JellyIcons.Delete, enabled = password.isNotBlank(), danger = true) {
                    onDeleteAccount(password)
                    deleteOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteOpen = false } }
        )
    }
}

@Composable
private fun SettingsGroupHeader(
    title: String,
    subtitle: String,
    icon: Int,
    expanded: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    JellyGlass(
        Modifier.fillMaxWidth(),
        radius = 22.dp,
        padding = 0.dp,
        onClick = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JellyIcon(icon, size = 34.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (danger) JellyDanger else JellyInk,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(subtitle, color = JellyMuted, fontSize = 10.sp, maxLines = 2)
            }
            Text(if (expanded) "⌄" else "›", color = JellyMuted, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SettingsMenuRow(
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JellyIcon(icon, size = 26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = JellyMuted, fontSize = 9.sp, maxLines = 1)
        }
        Text("›", color = JellyMuted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrivacySwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
