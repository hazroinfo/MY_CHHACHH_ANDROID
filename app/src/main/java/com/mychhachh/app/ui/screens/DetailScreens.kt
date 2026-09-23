package com.mychhachh.app.ui.screens

import android.content.Intent
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
    onFollow: (Long) -> Unit,
    onProfile: (Long) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit
) {
    val context = LocalContext.current
    val shop = data?.optJSONObject("shop")?.toShop() ?: data?.takeIf { it.has("name") }?.toShop()
    val posts = data?.optJSONArray("posts")?.posts().orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (loading && shop == null) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        shop?.let { s ->
            item {
                JellyGlass(Modifier.fillMaxWidth()) {
                    Column {
                        Box(
                            Modifier.fillMaxWidth().height(112.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFE3F8FF), Color(0xFFFFEAF5))))
                        ) {
                            s.cover?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        }
                        Column(Modifier.padding(horizontal = 14.dp).offset(y = (-30).dp)) {
                            Box(
                                Modifier.size(74.dp).clip(RoundedCornerShape(20.dp)).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!s.photo.isNullOrBlank()) AsyncImage(s.photo, s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else JellyIcon(JellyIcons.Shop, size = 52.dp)
                            }
                            Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 19.sp)
                            if (s.username.isNotBlank()) Text("@${s.username}", color = JellyMuted, fontSize = 10.5f.sp)
                            if (s.category.isNotBlank()) Text(s.category, color = JellyMuted, fontSize = 10.5f.sp)
                            if (s.description.isNotBlank()) Text(s.description, color = JellyInk, fontSize = 12.5f.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
                            val loc = listOf(s.area, s.village, s.city).filter { it.isNotBlank() }.joinToString(" • ")
                            if (loc.isNotBlank()) {
                                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                    JellyIcon(JellyIcons.Pin, size = 21.dp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(loc, color = JellyMuted, fontSize = 10.sp)
                                }
                            }
                            if (s.followers > 0) Text("${s.followers} followers", color = JellyMuted, fontSize = 9.5f.sp, modifier = Modifier.padding(top = 5.dp))
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                JellyButton(if (s.followed) "Following" else "Follow", primary = !s.followed, icon = JellyIcons.Follow) { onFollow(s.id) }
                                if (s.phone.isNotBlank()) JellyButton("Call", icon = JellyIcons.Phone) {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${s.phone}"))) }
                                }
                            }
                            if (s.whatsapp.isNotBlank() || s.locationUrl.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (s.whatsapp.isNotBlank()) JellyButton("WhatsApp", icon = JellyIcons.Whatsapp) {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${s.whatsapp.filter { it.isDigit() }}"))) }
                                    }
                                    if (s.locationUrl.isNotBlank()) JellyButton("Location", icon = JellyIcons.Map) {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.locationUrl))) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Shop posts") }
            if (posts.isEmpty()) item { EmptyCard("No shop posts yet.", JellyIcons.Shop) }
            items(posts, key = { "shop-post-${it.id}" }) { p ->
                PostCard(p, true, {}, { onProfile(p.user.id) }, onLike, onComment, onShare, onSave)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    me: User,
    busy: Boolean,
    error: String?,
    onSave: (JSONObject, Uri?) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember(me.id, me.name) { mutableStateOf(me.name) }
    var username by remember(me.id, me.username) { mutableStateOf(me.username) }
    var city by remember(me.id, me.city) { mutableStateOf(me.city) }
    var village by remember(me.id, me.village) { mutableStateOf(me.village) }
    var area by remember(me.id, me.area) { mutableStateOf(me.area) }
    var bio by remember(me.id, me.bio) { mutableStateOf(me.bio) }
    var privateProfile by remember(me.id, me.profileVisibility) { mutableStateOf(me.profileVisibility.equals("private", true)) }
    var avatarUri by remember(me.id) { mutableStateOf<Uri?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) avatarUri = uri
    }
    val usernameOk = username.matches(Regex("[a-z0-9._]{3,30}"))
    val canSave = name.trim().length >= 2 && usernameOk && !busy

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Profile photo")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (avatarUri != null) {
                            Box(Modifier.size(66.dp).clip(RoundedCornerShape(99.dp))) {
                                AsyncImage(avatarUri, "Selected profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        } else Avatar(me, 66.dp)
                        Spacer(Modifier.width(10.dp))
                        JellyButton("Choose photo", icon = JellyIcons.Photo) { avatarPicker.launch("image/*") }
                    }
                }
            }
        }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Edit profile")
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(
                        username,
                        { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' } },
                        Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        supportingText = { if (!usernameOk) Text("3–30 characters: a-z, 0-9, dot or underscore") },
                        isError = !usernameOk,
                        shape = RoundedCornerShape(17.dp),
                        singleLine = true
                    )
                    OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text("City") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(village, { village = it }, Modifier.fillMaxWidth(), label = { Text("Village") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), label = { Text("Mohallah / Area") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(bio, { bio = it }, Modifier.fillMaxWidth(), label = { Text("Bio") }, shape = RoundedCornerShape(17.dp), minLines = 3, maxLines = 6)

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Private profile", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Only approved followers can see follower-only content.", color = JellyMuted, fontSize = 9.5f.sp)
                        }
                        Switch(checked = privateProfile, onCheckedChange = { privateProfile = it })
                    }

                    error?.let { Text(it, color = Color(0xFFB23A55), fontWeight = FontWeight.Bold, fontSize = 10.5f.sp) }
                    JellyButton(
                        if (busy) "Saving…" else "Save changes",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Check,
                        enabled = canSave
                    ) {
                        onSave(
                            JSONObject()
                                .put("name", name.trim())
                                .put("username", username.trim())
                                .put("city", city.trim())
                                .put("village", village.trim())
                                .put("area", area.trim())
                                .put("bio", bio.trim())
                                .put("profile_visibility", if (privateProfile) "private" else "public"),
                            avatarUri
                        )
                    }
                }
            }
        }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Account")
                    Text("Your login session is stored securely on this device.", color = JellyMuted, fontSize = 10.5f.sp)
                    JellyButton("Logout", icon = JellyIcons.Logout, onClick = onLogout)
                }
            }
        }
    }
}
