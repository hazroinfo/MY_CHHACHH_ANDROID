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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyDanger
import com.mychhachh.app.ui.theme.JellyGreen
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import com.mychhachh.app.ui.theme.LiveJellyTheme
import org.json.JSONObject
import kotlinx.coroutines.launch

@Composable
private fun v95FramePadding() = if (LocalConfiguration.current.screenWidthDp <= 700) {
    minOf(LiveJellyTheme.framePadding, 8f).dp
} else {
    LiveJellyTheme.framePadding.dp
}

@Composable
fun ProfileScreen(
    data: JSONObject?,
    loading: Boolean,
    error: String?,
    meId: Long,
    onFollow: (Long) -> Unit,
    onMessage: (Long) -> Unit,
    onSettings: () -> Unit,
    onUpdateProfile: (JSONObject, Uri?, Uri?) -> Unit,
    onShop: (Long) -> Unit,
    onBlock: (Long) -> Unit,
    onReport: (Long, String) -> Unit,
    isAdmin: Boolean,
    onAdminAction: (String, Long, JSONObject) -> Unit,
    onLoadRelations: suspend (Long, String) -> List<User>,
    onRemoveRelation: suspend (Long, String) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRaw = data?.optJSONObject("user")
    val user = userRaw?.toUser()
    val posts = data?.optJSONArray("posts")?.posts().orEmpty()
    val shop = data?.optJSONObject("shop")?.toShop()
    val profileScreenWidthDp = LocalConfiguration.current.screenWidthDp
    val profileAvatarDp = LiveJellyTheme.profileAvatarSize
    val profileNameSize = if (profileScreenWidthDp <= 430) {
        (profileScreenWidthDp * .052f).coerceIn(18f, 23f).toInt()
    } else {
        (profileScreenWidthDp * .05f).coerceIn(19f, 28f).toInt()
    }
    var relationMode by remember { mutableStateOf<String?>(null) }
    var relationUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var relationLoading by remember { mutableStateOf(false) }
    var editProfileOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var adminWarningOpen by remember { mutableStateOf(false) }
    var adminWarningText by remember { mutableStateOf("") }
    var adminPostOpen by remember { mutableStateOf(false) }
    var adminPostText by remember { mutableStateOf("") }
    var adminDeleteOpen by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        val value = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(v95FramePadding(), 8.dp, v95FramePadding(), 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (loading && user == null) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        user?.let { u ->
            item {
                JellyGlass(
                    Modifier.fillMaxWidth(),
                    padding = LiveJellyTheme.cardPadding.dp,
                    gradientColors = listOf(
                        Color.White.copy(alpha = .73f),
                        Color(0xFFE3F5FF).copy(alpha = .50f)
                    ),
                    showShine = false
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(LiveJellyTheme.profileCoverHeight.dp)
                                .shadow(7.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0x596BD3FF),
                                            Color(0x4DA580FF),
                                            Color(0x45FF78C2)
                                        )
                                    )
                                )
                        ) {
                            if (!u.cover.isNullOrBlank()) {
                                AsyncImage(
                                    u.cover,
                                    null,
                                    Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(.54f)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0x33112347))
                                        )
                                    )
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                Modifier
                                    .width(profileAvatarDp.dp)
                                    .offset(y = (-LiveJellyTheme.profileOverlap).dp)
                            ) {
                                Avatar(u, profileAvatarDp.dp)
                            }
                            Spacer(Modifier.width(if (profileScreenWidthDp <= 430) 9.dp else 12.dp))
                            Column(
                                Modifier.weight(1f).padding(top = if (profileScreenWidthDp <= 430) 9.dp else 11.dp)
                            ) {
                                UserName(u, profileNameSize)
                                if (u.username.isNotBlank()) {
                                    Text(
                                        "@${u.username}",
                                        color = JellyMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }
                                Text(
                                    if (data.optBoolean("online", false)) "Online now"
                                    else data.optString("last_seen", "").takeIf { it.isNotBlank() }?.let { "Last seen: ${shortTime(it)}" }
                                        ?: "Last seen: Not available",
                                    color = JellyMuted,
                                    fontSize = 9.5f.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }

                        val followers = data.optLong("followers", -1)
                        val following = data.optLong("following_count", -1)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(1.dp)
                                .background(JellyMuted.copy(alpha = .12f))
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileCount(
                                if (followers >= 0) followers.toString() else "—",
                                "Followers",
                                Modifier.weight(1f)
                            ) {
                                if (followers >= 0) {
                                    relationMode = "followers"
                                    scope.launch {
                                        relationLoading = true
                                        relationUsers = runCatching { onLoadRelations(u.id, "followers") }.getOrDefault(emptyList())
                                        relationLoading = false
                                    }
                                }
                            }
                            ProfileCount(
                                if (following >= 0) following.toString() else "—",
                                "Following",
                                Modifier.weight(1f)
                            ) {
                                if (following >= 0) {
                                    relationMode = "following"
                                    scope.launch {
                                        relationLoading = true
                                        relationUsers = runCatching { onLoadRelations(u.id, "following") }.getOrDefault(emptyList())
                                        relationLoading = false
                                    }
                                }
                            }
                            ProfileCount(posts.size.toString(), "Posts", Modifier.weight(1f))
                        }

                        if (u.bio.isNotBlank()) {
                            Text(
                                u.bio,
                                color = JellyInk,
                                fontSize = 12.5f.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        val socials = listOf(
                            "Facebook" to u.socialFacebook,
                            "Instagram" to u.socialInstagram,
                            "YouTube" to u.socialYoutube,
                            "Website" to u.socialWebsite
                        ).filter { it.second.isNotBlank() }
                        if (socials.isNotEmpty()) {
                            socials.chunked(2).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    row.forEach { (label, url) ->
                                        JellyButton(label, Modifier.weight(1f)) { openUrl(url) }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        val aboutItems = listOf(
                            Triple("Current city", u.city, JellyIcons.City),
                            Triple("Village", u.village, JellyIcons.Village),
                            Triple("Mohalla / Area", u.area, JellyIcons.Mohalla),
                            Triple("From", u.hometown, JellyIcons.Hometown),
                            Triple(
                                "Gender",
                                when (u.gender) {
                                    "male" -> "Male"
                                    "female" -> "Female"
                                    "prefer_not_say" -> ""
                                    else -> u.gender.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                },
                                JellyIcons.Gender
                            ),
                            Triple(
                                "Relationship",
                                when (u.relationshipStatus) {
                                    "single" -> "Single"
                                    "married" -> "Married"
                                    "engaged" -> "Engaged"
                                    "prefer_not_say" -> ""
                                    else -> u.relationshipStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }
                                },
                                JellyIcons.Heart
                            ),
                            Triple("Work", u.work, JellyIcons.Work),
                            Triple("School / college", u.school, JellyIcons.School),
                            Triple("Email", u.email, JellyIcons.Mail),
                            Triple("Phone", u.phone, JellyIcons.Phone)
                        ).filter { it.second.isNotBlank() }

                        if (aboutItems.isNotEmpty()) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("About", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text(
                                    "${aboutItems.size} details · balanced profile information",
                                    color = JellyMuted,
                                    fontSize = 9.sp
                                )
                                aboutItems.chunked(2).forEach { row ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        row.forEach { (label, value, icon) ->
                                            ProfileAboutCard(
                                                label = label,
                                                value = value,
                                                icon = icon,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileAboutCard(
                                "Verification",
                                if (u.verified) "Complete" else "Not verified",
                                JellyIcons.Shield,
                                Modifier.weight(1f)
                            )
                            if (u.id != meId) {
                                JellyGlass(
                                    Modifier.weight(1f),
                                    radius = 18.dp,
                                    padding = 7.dp,
                                    onClick = { reportOpen = true }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        JellyIcon(JellyIcons.Shield, size = 25.dp)
                                        Spacer(Modifier.width(7.dp))
                                        Column {
                                            Text("Safety", color = JellyMuted, fontSize = 9.sp)
                                            Text("Report profile", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.5f.sp)
                                        }
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }

                        if (u.id == meId) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                JellyButton(
                                    "Edit Profile",
                                    Modifier.fillMaxWidth(),
                                    primary = true,
                                    icon = JellyIcons.Edit
                                ) { editProfileOpen = true }
                                JellyButton(
                                    "Account & Privacy Settings",
                                    Modifier.fillMaxWidth(),
                                    icon = JellyIcons.Gear,
                                    onClick = onSettings
                                )
                            }
                        } else if (isAdmin) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                JellyButton("Message", Modifier.weight(1f), icon = JellyIcons.Message) { onMessage(u.id) }
                                JellyButton("Share", Modifier.weight(1f), icon = JellyIcons.Share) {
                                    val intent = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, "https://chhachh.pages.dev/profile.php?id=${u.id}")
                                    runCatching { context.startActivity(Intent.createChooser(intent, "Share profile")) }
                                }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                JellyButton(
                                    if (data.optBoolean("following", false)) "Following" else "Follow",
                                    Modifier.weight(1f),
                                    primary = !data.optBoolean("following", false),
                                    icon = JellyIcons.Follow
                                ) { onFollow(u.id) }
                                JellyButton("Message", Modifier.weight(1f), icon = JellyIcons.Message) { onMessage(u.id) }
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                JellyButton("Share", Modifier.weight(1f), icon = JellyIcons.Share) {
                                    val intent = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, "https://chhachh.pages.dev/profile.php?id=${u.id}")
                                    runCatching { context.startActivity(Intent.createChooser(intent, "Share profile")) }
                                }
                                JellyButton(
                                    if (data.optBoolean("blocked", false)) "Unblock" else "Block",
                                    Modifier.weight(1f),
                                    icon = JellyIcons.Shield,
                                    danger = true
                                ) { onBlock(u.id) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            if (shop != null) {
                item {
                    JellyButton(
                        "Shop · ${shop.name}",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Shop
                    ) { onShop(shop.id) }
                }
            }

            if (isAdmin && u.id != meId) {
                item {
                    AdminProfileUserControls(
                        raw = userRaw ?: JSONObject(),
                        user = u,
                        onToggle = { key -> onAdminAction("user_setting", u.id, JSONObject().put("key", key)) },
                        onBlock = { onAdminAction("toggle_user", u.id, JSONObject()) },
                        onPromote = { onAdminAction("promote_user", u.id, JSONObject()) },
                        onWarning = { adminWarningText = ""; adminWarningOpen = true },
                        onPublish = { adminPostText = ""; adminPostOpen = true },
                        onDelete = { adminDeleteOpen = true }
                    )
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
                                placeholder = { Text("Write report reason...") },
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

    if (editProfileOpen && user != null) {
        ProfileEditDialog(
            user = user,
            onDismiss = { editProfileOpen = false },
            onSave = { fields, avatarUri, coverUri ->
                onUpdateProfile(fields, avatarUri, coverUri)
                editProfileOpen = false
            }
        )
    }

    if (adminWarningOpen && user != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { adminWarningOpen = false },
            title = { Text("Send Warning", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    adminWarningText,
                    { adminWarningText = it.take(2000) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Write an admin warning…") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Send Warning", primary = true, icon = JellyIcons.Bell, enabled = adminWarningText.isNotBlank()) {
                    onAdminAction("warning", user.id, JSONObject().put("message", adminWarningText.trim()))
                    adminWarningOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { adminWarningOpen = false } }
        )
    }

    if (adminPostOpen && user != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { adminPostOpen = false },
            title = { Text("Publish as this user", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    adminPostText,
                    { adminPostText = it.take(5000) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Write a post…") },
                    minLines = 4,
                    maxLines = 9,
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Publish User Post", primary = true, icon = JellyIcons.Send, enabled = adminPostText.isNotBlank()) {
                    onAdminAction("admin_user_post", user.id, JSONObject().put("text", adminPostText.trim()))
                    adminPostOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { adminPostOpen = false } }
        )
    }

    if (adminDeleteOpen && user != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { adminDeleteOpen = false },
            title = { Text("Delete User Account", color = JellyInk, fontWeight = FontWeight.Black) },
            text = { Text("Delete this user account? Preserved evidence remains on the server.", color = JellyInk) },
            confirmButton = {
                JellyButton("Delete User", icon = JellyIcons.Delete, danger = true) {
                    onAdminAction("delete_user", user.id, JSONObject())
                    adminDeleteOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { adminDeleteOpen = false } }
        )
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
                                Column(Modifier.weight(1f)) {
                                    UserName(person, 11)
                                    Text("@${person.username}", color = JellyMuted, fontSize = 9.sp)
                                }
                                if (user?.id == meId) {
                                    JellyButton(
                                        if (mode == "followers") "Remove" else "Unfollow",
                                        danger = mode == "followers",
                                        icon = if (mode == "followers") JellyIcons.Close else JellyIcons.People
                                    ) {
                                        scope.launch {
                                            relationLoading = true
                                            runCatching { onRemoveRelation(person.id, mode) }
                                            relationUsers = runCatching { onLoadRelations(meId, mode) }.getOrDefault(emptyList())
                                            relationLoading = false
                                        }
                                    }
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
private fun ProfileEditDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (JSONObject, Uri?, Uri?) -> Unit
) {
    var name by remember(user.id) { mutableStateOf(user.name) }
    var username by remember(user.id) { mutableStateOf(user.username) }
    var phone by remember(user.id) { mutableStateOf(user.phone) }
    var email by remember(user.id) { mutableStateOf(user.email) }
    var bio by remember(user.id) { mutableStateOf(user.bio) }
    var gender by remember(user.id) { mutableStateOf(user.gender) }
    var relationship by remember(user.id) { mutableStateOf(user.relationshipStatus) }
    var work by remember(user.id) { mutableStateOf(user.work) }
    var school by remember(user.id) { mutableStateOf(user.school) }
    var city by remember(user.id) { mutableStateOf(user.city) }
    var hometown by remember(user.id) { mutableStateOf(user.hometown) }
    var village by remember(user.id) { mutableStateOf(user.village) }
    var area by remember(user.id) { mutableStateOf(user.area) }
    var facebook by remember(user.id) { mutableStateOf(user.socialFacebook) }
    var instagram by remember(user.id) { mutableStateOf(user.socialInstagram) }
    var youtube by remember(user.id) { mutableStateOf(user.socialYoutube) }
    var website by remember(user.id) { mutableStateOf(user.socialWebsite) }
    var avatarUri by remember(user.id) { mutableStateOf<Uri?>(null) }
    var coverUri by remember(user.id) { mutableStateOf<Uri?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) avatarUri = it }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) coverUri = it }

    val usernameOk = username.matches(Regex("[a-z0-9_]{3,30}"))
    val emailOk = email.isBlank() || email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
    val phoneOk = phone.isBlank() || phone.matches(Regex("\\+?[0-9]{7,15}"))

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile", color = JellyInk, fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionTitle("Basic details") }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        JellyButton(
                            if (avatarUri != null) "Profile Photo ✓" else "Change Profile Photo",
                            Modifier.weight(1f),
                            icon = JellyIcons.Photo
                        ) { avatarPicker.launch("image/*") }
                        JellyButton(
                            if (coverUri != null) "Cover Photo ✓" else "Change Cover Photo",
                            Modifier.weight(1f),
                            icon = JellyIcons.Photo
                        ) { coverPicker.launch("image/*") }
                    }
                }
                item { OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(30) }, Modifier.fillMaxWidth(), label = { Text("Username (a-z, 0-9, _)") }, singleLine = true, isError = username.isNotBlank() && !usernameOk, shape = RoundedCornerShape(16.dp)) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, Modifier.weight(1f), label = { Text("Phone number") }, singleLine = true, isError = phone.isNotBlank() && !phoneOk, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(email, { email = it.take(120) }, Modifier.weight(1f), label = { Text("Email") }, singleLine = true, isError = email.isNotBlank() && !emailOk, shape = RoundedCornerShape(16.dp))
                    }
                }
                item { OutlinedTextField(bio, { bio = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("Short bio") }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(16.dp)) }

                item { SectionTitle("Personal details · Optional") }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        JellyButton(
                            "Gender: " + when (gender) {
                                "male" -> "Male"; "female" -> "Female"; "prefer_not_say" -> "Prefer not to say"; else -> "Not set"
                            },
                            Modifier.weight(1f)
                        ) {
                            gender = when (gender) { "" -> "male"; "male" -> "female"; "female" -> "prefer_not_say"; else -> "" }
                        }
                        JellyButton(
                            "Relationship: " + when (relationship) {
                                "single" -> "Single"; "married" -> "Married"; "engaged" -> "Engaged"; "prefer_not_say" -> "Prefer not to say"; else -> "Not set"
                            },
                            Modifier.weight(1f)
                        ) {
                            relationship = when (relationship) { "" -> "single"; "single" -> "married"; "married" -> "engaged"; "engaged" -> "prefer_not_say"; else -> "" }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(work, { work = it.take(120) }, Modifier.weight(1f), label = { Text("Work / profession") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(school, { school = it.take(120) }, Modifier.weight(1f), label = { Text("School / college") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(city, { city = it.take(100) }, Modifier.weight(1f), label = { Text("Current city") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(hometown, { hometown = it.take(100) }, Modifier.weight(1f), label = { Text("From / hometown") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(village, { village = it.take(100) }, Modifier.weight(1f), label = { Text("Village") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(area, { area = it.take(100) }, Modifier.weight(1f), label = { Text("Mohalla / area") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    }
                }

                item { SectionTitle("Social links") }
                item { OutlinedTextField(facebook, { facebook = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Facebook link") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(instagram, { instagram = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Instagram link") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(youtube, { youtube = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("YouTube link") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
                item { OutlinedTextField(website, { website = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Website") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
            }
        },
        confirmButton = {
            JellyButton(
                "Save Profile",
                primary = true,
                icon = JellyIcons.Check,
                enabled = name.trim().length >= 2 && usernameOk && phoneOk && emailOk
            ) {
                onSave(
                    JSONObject()
                        .put("name", name.trim())
                        .put("username", username.trim())
                        .put("phone", phone.trim())
                        .put("email", email.trim())
                        .put("bio", bio.trim())
                        .put("gender", gender)
                        .put("relationship_status", relationship)
                        .put("work", work.trim())
                        .put("school", school.trim())
                        .put("city", city.trim())
                        .put("hometown", hometown.trim())
                        .put("village", village.trim())
                        .put("area", area.trim())
                        .put("social_facebook", facebook.trim())
                        .put("social_instagram", instagram.trim())
                        .put("social_youtube", youtube.trim())
                        .put("social_website", website.trim()),
                    avatarUri,
                    coverUri
                )
            }
        },
        dismissButton = { JellyButton("Cancel", onClick = onDismiss) }
    )
}

@Composable
private fun AdminProfileUserControls(
    raw: JSONObject,
    user: User,
    onToggle: (String) -> Unit,
    onBlock: () -> Unit,
    onPromote: () -> Unit,
    onWarning: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit
) {
    fun enabled(key: String): Boolean = if (raw.has(key)) raw.optInt(key, 1) != 0 && raw.optBoolean(key, true) else true
    JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Admin User Controls", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("All controls for this user are kept on this profile.", color = JellyMuted, fontSize = 9.5f.sp)
                }
                Text("User #${user.id}", color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            val controls = listOf(
                Triple("verified", "Blue Tick", JellyIcons.Shield),
                Triple("allow_photo_upload", "Photo Upload", JellyIcons.Photo),
                Triple("allow_video_upload", "Video Upload", JellyIcons.Video),
                Triple("allow_likes", "Likes", JellyIcons.Heart),
                Triple("allow_comments", "Comments", JellyIcons.Comment),
                Triple("allow_follows", "Followers", JellyIcons.People),
                Triple("allow_posts", "Posts", JellyIcons.Edit),
                Triple("allow_messages", "Messages", JellyIcons.Message)
            )
            controls.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { (key, label, icon) ->
                        val isOn = if (key == "verified") user.verified else enabled(key)
                        JellyButton(
                            "$label · ${if (isOn) "ON" else "OFF"}",
                            Modifier.weight(1f),
                            primary = isOn,
                            icon = icon
                        ) { onToggle(key) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JellyButton(
                    if (raw.optBoolean("blocked", false)) "Unblock Account" else "Block Account",
                    Modifier.weight(1f),
                    danger = !raw.optBoolean("blocked", false),
                    icon = JellyIcons.Shield,
                    onClick = onBlock
                )
                JellyButton(
                    if (raw.optBoolean("promoted", false)) "Stop Promotion" else "Promote User",
                    Modifier.weight(1f),
                    icon = JellyIcons.Star,
                    onClick = onPromote
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JellyButton("Send Warning", Modifier.weight(1f), icon = JellyIcons.Bell, onClick = onWarning)
                JellyButton("Publish as User", Modifier.weight(1f), icon = JellyIcons.Edit, onClick = onPublish)
            }
            val context = LocalContext.current
            JellyButton("Download Complete PDF", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Save) {
                val uri = Uri.parse("https://chhachh.pages.dev/evidence_pdf.php?user_id=${user.id}")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }
            JellyButton("Delete User Account", Modifier.fillMaxWidth(), icon = JellyIcons.Delete, danger = true, onClick = onDelete)
        }
    }
}

@Composable
private fun ProfileInfoTile(
    label: String,
    value: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    JellyGlass(modifier, radius = 16.dp, padding = 9.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            JellyIcon(icon, size = 21.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = JellyMuted, fontSize = 8.5f.sp, fontWeight = FontWeight.Bold)
                Text(
                    value,
                    color = JellyInk,
                    fontSize = 10.5f.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileAboutCard(
    label: String,
    value: String,
    icon: Int,
    modifier: Modifier = Modifier
) {
    JellyGlass(modifier, radius = 18.dp, padding = 7.dp) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.Top
        ) {
            JellyIcon(icon, size = 25.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = JellyMuted, fontSize = 9.sp)
                Text(
                    value,
                    color = JellyInk,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.5f.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    JellyGlass(modifier, radius = 14.dp, padding = 8.dp, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(label, color = JellyMuted, fontSize = 8.5f.sp)
        }
    }
}

@Composable
private fun ProfileCount(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.clickable { onClick?.invoke() } else Modifier)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(number, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Text(label, color = JellyMuted, fontSize = 8.5f.sp)
    }
}

@Composable
private fun ShopInfoCard(
    label: String,
    value: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    JellyGlass(
        modifier.then(if (onClick != null) Modifier.clickable { onClick?.invoke() } else Modifier),
        radius = 17.dp,
        padding = 9.dp
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.Top
        ) {
            JellyIcon(icon, size = 24.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = JellyMuted, fontSize = 8.5f.sp)
                Text(
                    value,
                    color = JellyInk,
                    fontSize = 10.5f.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
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
    onLoadFollowers: suspend (Long) -> List<User>,
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
    var followersOpen by remember { mutableStateOf(false) }
    var shopFollowers by remember { mutableStateOf<List<User>>(emptyList()) }
    var shopFollowersLoading by remember { mutableStateOf(false) }
    val shopScope = rememberCoroutineScope()
    val shopScreenWidthDp = LocalConfiguration.current.screenWidthDp
    val shopAvatarDp = if (shopScreenWidthDp <= 430) {
        minOf(LiveJellyTheme.shopAvatarSize, 92f)
    } else {
        LiveJellyTheme.shopAvatarSize
    }
    val shopNameSize = if (shopScreenWidthDp <= 430) {
        (shopScreenWidthDp * .052f).coerceIn(18f, 23f)
    } else {
        (shopScreenWidthDp * .05f).coerceIn(20f, 27f)
    }

    fun shareShop(id: Long, name: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://chhachh.pages.dev/shops.php?shop=$id")
        runCatching { context.startActivity(Intent.createChooser(intent, "Share $name")) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(v95FramePadding(), 8.dp, v95FramePadding(), 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (loading && shop == null) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }

        shop?.let { s ->
            val own = s.userId == meId
            item {
                JellyGlass(
                    Modifier.fillMaxWidth(),
                    padding = LiveJellyTheme.cardPadding.dp,
                    gradientColors = listOf(
                        Color.White.copy(alpha = .73f),
                        Color(0xFFE3F5FF).copy(alpha = .50f)
                    ),
                    showShine = false
                ) {
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(LiveJellyTheme.shopCoverHeight.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = LiveJellyTheme.cardRadius.dp,
                                        topEnd = LiveJellyTheme.cardRadius.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd = 18.dp
                                    )
                                )
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF65CBFF).copy(alpha = .42f),
                                            Color(0xFF9375FF).copy(alpha = .36f),
                                            Color(0xFFFF69BE).copy(alpha = .30f)
                                        )
                                    )
                                )
                        ) {
                            s.cover?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0x4D0C1C3A))
                                        )
                                    )
                            )
                            Row(
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(if (shopScreenWidthDp <= 700) 9.dp else 12.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = .70f))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                JellyIcon(JellyIcons.Shop, size = 20.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    s.category.ifBlank { "Local business" },
                                    color = JellyInk,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    Modifier
                                        .size(shopAvatarDp.dp)
                                        .offset(y = (-LiveJellyTheme.shopOverlap).dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color.White)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!s.photo.isNullOrBlank()) {
                                        AsyncImage(
                                            s.photo,
                                            s.name,
                                            Modifier.fillMaxSize().clip(RoundedCornerShape(999.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        JellyIcon(JellyIcons.Shop, size = 54.dp)
                                    }
                                }
                                Spacer(Modifier.width(if (shopScreenWidthDp <= 430) 9.dp else 12.dp))
                                Column(
                                    Modifier.weight(1f).padding(top = if (shopScreenWidthDp <= 430) 9.dp else 11.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = shopNameSize.sp)
                                    if (s.username.isNotBlank()) Text("@${s.username}", color = JellyMuted, fontSize = 10.5f.sp)
                                    Text(s.category.ifBlank { "Shop" }, color = JellyMuted, fontSize = 10.sp)
                                    val shortPlace = listOf(s.village, s.city).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (shortPlace.isNotBlank()) Text(shortPlace, color = JellyMuted, fontSize = 9.5f.sp)
                                }
                            }

                            JellyGlass(Modifier.fillMaxWidth(), radius = 20.dp, padding = 9.dp) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ProfileCount(posts.size.toString(), "Products", Modifier.weight(1f))
                                    ProfileCount(followers.toString(), "Followers", Modifier.weight(1f)) {
                                        if (own) {
                                            followersOpen = true
                                            shopScope.launch {
                                                shopFollowersLoading = true
                                                shopFollowers = runCatching { onLoadFollowers(s.id) }.getOrDefault(emptyList())
                                                shopFollowersLoading = false
                                            }
                                        }
                                    }
                                    ProfileCount(views.toString(), "Views", Modifier.weight(1f))
                                }
                            }

                            val businessRows = listOf(
                                Triple("Business name", s.name, JellyIcons.Shop),
                                Triple("Username", s.username.takeIf { it.isBlank() } ?: "@${s.username}", JellyIcons.User),
                                Triple("Category", s.category, JellyIcons.Category),
                                Triple("Phone", s.phone, JellyIcons.Phone),
                                Triple("WhatsApp", s.whatsapp, JellyIcons.Whatsapp),
                                Triple("City", s.city, JellyIcons.City),
                                Triple("Village", s.village, JellyIcons.Village),
                                Triple("Mohalla / Area", s.area, JellyIcons.Mohalla),
                                Triple("Address", s.location, JellyIcons.Address)
                            ).filter { it.second.isNotBlank() }
                            if (businessRows.isNotEmpty() || s.description.isNotBlank()) {
                                JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 10.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text("Business Information", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                Text(
                                                    "${businessRows.size} details · contact, location and shop information",
                                                    color = JellyMuted,
                                                    fontSize = 8.5f.sp
                                                )
                                            }
                                            JellyIcon(JellyIcons.Info, size = 24.dp)
                                        }
                                        businessRows.chunked(2).forEach { row ->
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                row.forEach { (label, value, icon) ->
                                                    ShopInfoCard(label, value, icon, Modifier.weight(1f))
                                                }
                                                if (row.size == 1) Spacer(Modifier.weight(1f))
                                            }
                                        }
                                        if (s.locationUrl.isNotBlank()) {
                                            ShopInfoCard(
                                                "Map location",
                                                "Open in Google Maps",
                                                JellyIcons.Map,
                                                Modifier.fillMaxWidth()
                                            ) {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.locationUrl)))
                                                }
                                            }
                                        }
                                        if (s.description.isNotBlank()) {
                                            Text("About this shop", color = JellyMuted, fontSize = 8.5f.sp, fontWeight = FontWeight.Bold)
                                            Text(s.description, color = JellyInk, fontSize = 11.sp, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }

                            if (own) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    JellyButton("Edit Shop", Modifier.weight(1f), primary = true, icon = JellyIcons.Edit) { editOpen = true }
                                    JellyButton("Share Shop", Modifier.weight(1f), icon = JellyIcons.Share) { shareShop(s.id, s.name) }
                                }
                                JellyButton("Delete Shop", Modifier.fillMaxWidth(), icon = JellyIcons.Delete, danger = true) { deleteOpen = true }
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
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    if (s.whatsapp.isNotBlank()) {
                                        JellyButton("WhatsApp", Modifier.weight(1f), icon = JellyIcons.Whatsapp) {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://wa.me/${s.whatsapp.filter { it.isDigit() }}")
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    JellyButton("Share", Modifier.weight(1f), icon = JellyIcons.Share) { shareShop(s.id, s.name) }
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

        if (followersOpen) {
            item {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { followersOpen = false },
                    title = { Text("Shop Followers", color = JellyInk, fontWeight = FontWeight.Black) },
                    text = {
                        LazyColumn(
                            Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (shopFollowersLoading) item { LoadingBlock() }
                            if (!shopFollowersLoading && shopFollowers.isEmpty()) {
                                item { Text("No followers yet.", color = JellyMuted, fontSize = 10.5f.sp) }
                            }
                            items(shopFollowers, key = { "shop-follower-${it.id}" }) { follower ->
                                JellyGlass(
                                    Modifier.fillMaxWidth(),
                                    radius = 15.dp,
                                    padding = 8.dp,
                                    onClick = { onProfile(follower.id) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Avatar(follower, 38.dp)
                                        Spacer(Modifier.width(7.dp))
                                        Column {
                                            UserName(follower, 11)
                                            if (follower.username.isNotBlank()) {
                                                Text("@${follower.username}", color = JellyMuted, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { JellyButton("Close") { followersOpen = false } }
                )
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
                placeholder = { Text("Write shop post...") },
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
        contentPadding = PaddingValues(v95FramePadding(), 8.dp, v95FramePadding(), 18.dp),
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
                    PrivacySwitch("Hide my followers and following counts", hideFollowers) { hideFollowers = it }
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
            val language = AppLanguage.current
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("Language")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JellyPill("English", language == "en", Modifier.weight(1f)) {
                            AppLanguage.set("en")
                            prefs.edit().putString("language", "en").apply()
                        }
                        JellyPill("اردو", language == "ur", Modifier.weight(1f)) {
                            AppLanguage.set("ur")
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
                    SectionTitle("Identity verification")
                    val status = verification?.optString("status", "not_submitted") ?: "not_submitted"
                    Text(
                        when (status) {
                            "approved" -> "Verified ✓"
                            "pending" -> "Verification is pending admin review."
                            "rejected" -> "Verification was rejected. You can submit again."
                            else -> "Keep your account trusted and unlock approved photo and video uploads."
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
                    SectionTitle("Contact My Chhachh Support")
                    Text(
                        "Send a problem, safety report or feedback directly to the Chhachh Team. Team replies stay in your support history.",
                        color = JellyMuted,
                        fontSize = 10.sp
                    )
                    Text("Request type", color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                        placeholder = { Text("Short subject") },
                        shape = RoundedCornerShape(17.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        supportMessage,
                        { supportMessage = it.take(3000) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Message") },
                        placeholder = { Text("Explain what happened and what help you need...") },
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
                    SectionTitle("My Support Requests")
                    if (supportTickets.isEmpty()) {
                        Text("No support requests yet.", color = JellyMuted, fontSize = 9.5f.sp)
                    }
                    if (supportTickets.isNotEmpty()) {
                        supportTickets.take(12).forEach { ticket ->
                            JellyGlass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 9.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(ticket.optString("subject", "Help & Support"), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Text(ticket.optString("reason", ""), color = JellyInk, fontSize = 9.5f.sp, maxLines = 4)
                                    Text(ticket.optString("status", "open"), color = JellyMuted, fontSize = 8.5f.sp)
                                    if (ticket.optString("admin_reply", "").isBlank()) {
                                        Text("Waiting for Chhachh Team reply.", color = JellyMuted, fontSize = 8.5f.sp)
                                    }
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

        item {
            JellyButton(
                if (busy) "Saving…" else "Save Changes",
                Modifier.fillMaxWidth(),
                primary = true,
                icon = JellyIcons.Check,
                enabled = !busy
            ) {
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
                    Text("This permanently deletes your account. This action cannot be undone.", color = Color(0xFFB23A55), fontWeight = FontWeight.Bold)
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
