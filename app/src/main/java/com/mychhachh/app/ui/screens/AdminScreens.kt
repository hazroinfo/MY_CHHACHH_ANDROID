package com.mychhachh.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychhachh.app.data.User
import com.mychhachh.app.data.toUser
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private fun jsonObjects(a: JSONArray?): List<JSONObject> {
    if (a == null) return emptyList()
    return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
}

@Composable
fun AdminCenterScreen(
    state: JSONObject?,
    list: JSONObject?,
    me: User,
    traffic: JSONObject?,
    section: String,
    query: String,
    loading: Boolean,
    error: String?,
    onSection: (String) -> Unit,
    onQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onAction: (String, Long, JSONObject) -> Unit,
    onProfile: (Long) -> Unit,
    onTheme: () -> Unit,
    onAnnouncements: () -> Unit,
    onPublishAnnouncement: (String, String, Uri?, Uri?) -> Unit,
    onPublishAdminPost: (String, Uri?, Uri?) -> Unit
) {
    val stats = state?.optJSONObject("stats") ?: JSONObject()
    val rows = jsonObjects(list?.optJSONArray("items"))
    var replyTarget by remember { mutableStateOf<Long?>(null) }
    var replyText by remember { mutableStateOf("") }
    var warningTarget by remember { mutableStateOf<Long?>(null) }
    var warningText by remember { mutableStateOf("") }
    var userPostTarget by remember { mutableStateOf<Long?>(null) }
    var userPostText by remember { mutableStateOf("") }
    var verificationTarget by remember { mutableStateOf<Long?>(null) }
    var verificationDecision by remember { mutableStateOf("approved") }
    var verificationNote by remember { mutableStateOf("") }
    var deleteUserTarget by remember { mutableStateOf<Long?>(null) }
    var noticeType by remember { mutableStateOf("announcement") }
    var noticeText by remember { mutableStateOf("") }
    var noticePhoto by remember { mutableStateOf<Uri?>(null) }
    var noticeAudio by remember { mutableStateOf<Uri?>(null) }
    var adminPostText by remember { mutableStateOf("") }
    var adminPostPhoto by remember { mutableStateOf<Uri?>(null) }
    var adminPostVideo by remember { mutableStateOf<Uri?>(null) }
    var featurePackageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val noticePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) noticePhoto = it }
    val noticeAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) noticeAudio = it }
    val adminPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) adminPostPhoto = it }
    val adminVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) adminPostVideo = it }
    val featurePackagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            featurePackageText = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
        }
    }
    val settings = state?.optJSONObject("settings") ?: JSONObject()
    val notices = jsonObjects(state?.optJSONArray("notices"))
    val packages = jsonObjects(state?.optJSONArray("feature_packages"))
    val pagedSections = setOf("users", "verification", "posts", "shops", "votes", "reports", "deleted", "activity", "records")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            PageTitle("Admin Center", "Website administration only. Theme Builder is a separate menu page.", JellyIcons.Shield)
        }

        if (stats.optInt("admin_alerts", 0) > 0) {
            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(JellyIcons.Bell, size = 32.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${stats.optInt("admin_alerts")} items need admin attention",
                                color = JellyInk,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                "${stats.optInt("pending_verifications")} verification requests · ${stats.optInt("open_reports")} open reports",
                                color = JellyMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 8.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminNavGroup(
                        "People & content",
                        listOf(
                            AdminNavItem("overview", "Overview", JellyIcons.Eye),
                            AdminNavItem("users", "Users", JellyIcons.People),
                            AdminNavItem("verification", "Verification", JellyIcons.Shield, stats.optInt("pending_verifications")),
                            AdminNavItem("posts", "Posts", JellyIcons.Photo),
                            AdminNavItem("shops", "Shops", JellyIcons.Shop),
                            AdminNavItem("votes", "Voting", JellyIcons.Vote)
                        ),
                        section,
                        onSection
                    )
                    AdminNavGroup(
                        "Safety & records",
                        listOf(
                            AdminNavItem("reports", "Reports", JellyIcons.Shield, stats.optInt("open_reports")),
                            AdminNavItem("deleted", "Deleted", JellyIcons.Delete),
                            AdminNavItem("activity", "Activity", JellyIcons.Clock),
                            AdminNavItem("records", "Records", JellyIcons.User)
                        ),
                        section,
                        onSection
                    )
                    AdminNavGroup(
                        "Communication",
                        listOf(AdminNavItem("notices", "Announcements", JellyIcons.Announcement)),
                        section,
                        onSection
                    )
                    AdminNavGroup(
                        "System",
                        listOf(
                            AdminNavItem("features", "Features", JellyIcons.Gear),
                            AdminNavItem("installer", "Installer", JellyIcons.Plus),
                            AdminNavItem("ads", "Ad Manager", JellyIcons.Star),
                            AdminNavItem("traffic", "Traffic", JellyIcons.Eye),
                            AdminNavItem("social", "Login Setup", JellyIcons.People),
                            AdminNavItem("theme", "Theme Builder", JellyIcons.Palette)
                        ),
                        if (section == "theme") "theme" else section
                    ) { key ->
                        if (key == "theme") onTheme() else onSection(key)
                    }
                    AdminNavGroup(
                        "Account",
                        listOf(AdminNavItem("adminprofile", "Admin Profile", JellyIcons.User)),
                        section,
                        onSection
                    )
                }
            }
        }

        if (section == "overview") {
            item {
                JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Overview", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Live website administration", color = JellyMuted, fontSize = 9.5f.sp)
                            }
                            Text("Live data", color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            AdminStat("Users", stats.optInt("total_users"), Modifier.weight(1f))
                            AdminStat("Shops", stats.optInt("total_shops"), Modifier.weight(1f))
                            AdminStat("Open Reports", stats.optInt("open_reports"), Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            AdminStat("Pending Verifications", stats.optInt("pending_verifications"), Modifier.weight(1f))
                            AdminStat("Posts", stats.optInt("total_posts"), Modifier.weight(1f))
                            AdminStat("Voting", stats.optInt("total_votes"), Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        adminSectionTitle(section),
                        Modifier.weight(1f),
                        color = JellyInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    JellyButton("Refresh", icon = JellyIcons.Search, onClick = onRefresh)
                }
            }
            if (section in pagedSections) {
                item {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Search this section…") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        if (loading && (section !in pagedSections || rows.isEmpty())) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onRefresh) } }

        when (section) {
            "users" -> items(rows, key = { "admin-user-${it.optLong("id")}" }) { o ->
                val u = runCatching { o.toUser() }.getOrNull()
                if (u != null) {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(u, 46.dp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                UserName(u, 12)
                                Text("@${u.username}", color = JellyMuted, fontSize = 9.sp)
                                val contact = listOf(u.email, u.phone).filter { it.isNotBlank() }.joinToString(" · ")
                                if (contact.isNotBlank()) Text(contact, color = JellyMuted, fontSize = 8.5f.sp, maxLines = 1)
                                val loc = listOf(u.area, u.village, u.city).filter { it.isNotBlank() }.joinToString(" · ")
                                if (loc.isNotBlank()) Text(loc, color = JellyMuted, fontSize = 8.2f.sp, maxLines = 1)
                                Text(
                                    if (o.optBoolean("online", false)) "Online" else "Offline",
                                    color = if (o.optBoolean("online", false)) JellyGreen else JellyMuted,
                                    fontSize = 8.5f.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            JellyButton("Open Profile", primary = true) { onProfile(u.id) }
                        }
                    }
                }
            }

            "verification" -> {
                item {
                    var postPhoto by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_unverified_post_photo", 0) != 0) }
                    var postVideo by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_unverified_post_video", 0) != 0) }
                    var shopPhoto by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_unverified_shop_photo", 0) != 0) }
                    var shopVideo by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_unverified_shop_video", 0) != 0) }
                    var messagePhoto by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_unverified_message_photo", 0) != 0) }
                    var announcementPhoto by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_unverified_announcement_photo", 0) != 0) }
                    var autoTick by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_auto_blue_tick", 0) != 0) }
                    var photoAfter by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_enable_photo_after_approval", 1) != 0) }
                    var videoAfter by remember(settings.toString()) { mutableStateOf(settings.optInt("verification_enable_video_after_approval", 1) != 0) }

                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            SectionTitle("Verification Access Rules")
                            Text(
                                "Choose exactly what an unverified user may upload. These are live backend rules; per-user Admin controls still apply.",
                                color = JellyMuted,
                                fontSize = 9.5f.sp
                            )
                            VerificationRuleRow("Feed photos without verification", "Allow normal post photos before identity approval.", postPhoto) { postPhoto = it }
                            VerificationRuleRow("Feed videos without verification", "Allow normal post videos before identity approval.", postVideo) { postVideo = it }
                            VerificationRuleRow("Shop photos without verification", "Allow shop-post photos before identity approval.", shopPhoto) { shopPhoto = it }
                            VerificationRuleRow("Shop videos without verification", "Allow shop-post videos before identity approval.", shopVideo) { shopVideo = it }
                            VerificationRuleRow("Message photos without verification", "Allow photo attachments in messages before identity approval.", messagePhoto) { messagePhoto = it }
                            VerificationRuleRow("Announcement photos without verification", "Allow announcement photos before identity approval.", announcementPhoto) { announcementPhoto = it }
                            VerificationRuleRow("Give Blue Tick automatically after approval", "", autoTick) { autoTick = it }
                            VerificationRuleRow("Enable photo upload after approval", "", photoAfter) { photoAfter = it }
                            VerificationRuleRow("Enable video upload after approval", "", videoAfter) { videoAfter = it }
                            Text(
                                "Example: to require verification only for video, keep video-without-verification OFF and Enable video upload after approval ON.",
                                color = JellyMuted,
                                fontSize = 8.5f.sp
                            )
                            JellyButton("Save Verification Rules", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check) {
                                onAction(
                                    "verification_policy",
                                    0L,
                                    JSONObject()
                                        .put("verification_unverified_post_photo", postPhoto)
                                        .put("verification_unverified_post_video", postVideo)
                                        .put("verification_unverified_shop_photo", shopPhoto)
                                        .put("verification_unverified_shop_video", shopVideo)
                                        .put("verification_unverified_message_photo", messagePhoto)
                                        .put("verification_unverified_announcement_photo", announcementPhoto)
                                        .put("verification_auto_blue_tick", autoTick)
                                        .put("verification_enable_photo_after_approval", photoAfter)
                                        .put("verification_enable_video_after_approval", videoAfter)
                                )
                            }
                        }
                    }
                }
                items(rows, key = { "verify-${it.optLong("id")}" }) { o ->
                val u = o.optJSONObject("user")?.let { runCatching { it.toUser() }.getOrNull() }
                JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (u != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(u, 44.dp)
                                Spacer(Modifier.width(7.dp))
                                Column {
                                    UserName(u, 12)
                                    Text("@${u.username}", color = JellyMuted, fontSize = 9.sp)
                                }
                            }
                        }
                        Text("Status: ${o.optString("status", "pending")}", color = JellyMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            JellyButton("Approve", Modifier.weight(1f), primary = true, icon = JellyIcons.Check) {
                                verificationTarget = o.optLong("id")
                                verificationDecision = "approved"
                                verificationNote = o.optString("admin_note", "")
                            }
                            JellyButton("Reject", Modifier.weight(1f), icon = JellyIcons.Close, danger = true) {
                                verificationTarget = o.optLong("id")
                                verificationDecision = "rejected"
                                verificationNote = o.optString("admin_note", "")
                            }
                        }
                    }
                }
            }
            }

            "reports" -> {
                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Column {
                            SectionTitle("Reports & Support")
                            Text(
                                "User reports and Help & Support requests arrive here for review and reply.",
                                color = JellyMuted,
                                fontSize = 9.5f.sp
                            )
                        }
                    }
                }
                if (rows.isEmpty() && !loading) item { EmptyCard("No reports or support requests.", JellyIcons.Shield) }
                items(rows, key = { "report-${it.optLong("id")}" }) { o ->
                val reporter = o.optJSONObject("reporter")?.let { runCatching { it.toUser() }.getOrNull() }
                JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reporter?.let {
                            UserName(it, 12)
                            Text("@${it.username}", color = JellyMuted, fontSize = 9.sp)
                        }
                        val targetType = o.optString("target_type", "")
                        val reporterId = o.optLong("reporter_id", o.optLong("user_id", reporter?.id ?: 0L))
                        val targetId = o.optLong("target_user_id", o.optLong("target_id", 0L))
                        if (o.optString("category").isNotBlank()) {
                            Text("Type: ${o.optString("category")}", color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        if (o.optString("subject").isNotBlank()) {
                            Text(o.optString("subject"), color = JellyInk, fontSize = 10.5f.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(o.optString("reason", o.optString("text", "Report")), color = JellyInk, fontSize = 11.5f.sp)
                        Text("Status: ${o.optString("status", "open")}", color = JellyMuted, fontSize = 9.sp)
                        if (o.optString("admin_reply").isNotBlank()) {
                            Text("Previous reply: ${o.optString("admin_reply")}", color = JellyMuted, fontSize = 9.5f.sp, maxLines = 4)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (reporterId > 0) {
                                JellyButton("Open Reporter Profile", Modifier.weight(1f), icon = JellyIcons.User) { onProfile(reporterId) }
                            }
                            if (targetType == "profile" && targetId > 0) {
                                JellyButton("Open Reported Profile", Modifier.weight(1f), icon = JellyIcons.Shield) { onProfile(targetId) }
                            }
                        }
                        JellyButton("Reply", Modifier.fillMaxWidth(), icon = JellyIcons.Reply) {
                            replyTarget = o.optLong("id")
                            replyText = o.optString("admin_reply", "")
                        }
                    }
                }
            }
            }

            "posts" -> items(rows, key = { "admin-post-${it.optLong("id")}" }) { o ->
                JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        o.optJSONObject("user")?.let { uo ->
                            runCatching { uo.toUser() }.getOrNull()?.let { UserName(it, 11) }
                        }
                        Text(o.optString("text", "(media post)"), color = JellyInk, fontSize = 11.5f.sp, maxLines = 5)
                        val shopPost = o.optLong("shop_id", 0L) > 0L || o.optJSONObject("shop") != null || o.optBoolean("shop_post", false)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            JellyButton(if (o.optBoolean("promoted", false)) "Stop Promotion" else "Promote", icon = JellyIcons.Star) {
                                onAction(if (shopPost) "promote_shop_post" else "promote_post", o.optLong("id"), JSONObject())
                            }
                            if (!shopPost) {
                                JellyButton("Delete", icon = JellyIcons.Delete, danger = true) {
                                    onAction("delete_post", o.optLong("id"), JSONObject())
                                }
                            }
                        }
                    }
                }
            }

            "shops" -> items(rows, key = { "admin-shop-${it.optLong("id")}" }) { o ->
                JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(o.optString("name", "Shop"), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("@${o.optString("username", "")}", color = JellyMuted, fontSize = 9.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            JellyButton(if (o.optInt("active", 1) == 0) "Enable" else "Disable", Modifier.weight(1f)) {
                                onAction("toggle_shop", o.optLong("id"), JSONObject())
                            }
                            JellyButton("Promote", Modifier.weight(1f), icon = JellyIcons.Star) {
                                onAction("promote_shop", o.optLong("id"), JSONObject())
                            }
                        }
                        JellyButton("Delete Shop", Modifier.fillMaxWidth(), icon = JellyIcons.Delete, danger = true) {
                            onAction("delete_shop", o.optLong("id"), JSONObject())
                        }
                    }
                }
            }

            "votes" -> {
                item {
                    val waiting = rows.count { it.optString("status") == "waiting" }
                    val ready = rows.count { it.optString("status") == "ready" }
                    val live = rows.count { it.optString("status") == "active" }
                    val finished = rows.size - waiting - ready - live
                    val showLiveCounts = settings.optInt("voting_show_live_counts", 0) != 0
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    SectionTitle("Voting Arena Control")
                                    Text("Manage challenges, waiting rooms, live matches and completed results.", color = JellyMuted, fontSize = 9.5f.sp)
                                }
                                Text("${rows.size} total", color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Show vote counts during live voting", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.5f.sp)
                                    Text(
                                        if (showLiveCounts) "ON — visitors can see live totals before the timer ends."
                                        else "OFF — counts stay hidden until the final result.",
                                        color = JellyMuted,
                                        fontSize = 8.5f.sp
                                    )
                                }
                                JellyButton(if (showLiveCounts) "Turn OFF" else "Turn ON", primary = !showLiveCounts, danger = showLiveCounts) {
                                    onAction(
                                        "feature",
                                        0L,
                                        JSONObject().put("key", "voting_show_live_counts").put("value", if (showLiveCounts) 0 else 1)
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AdminStat("Waiting", waiting, Modifier.weight(1f))
                                AdminStat("Ready", ready, Modifier.weight(1f))
                                AdminStat("Live", live, Modifier.weight(1f))
                                AdminStat("Finished", finished, Modifier.weight(1f))
                            }
                        }
                    }
                }

                items(rows, key = { "admin-vote-${it.optLong("id")}" }) { o ->
                    var adjustAmount by remember(o.optLong("id")) { mutableStateOf("1") }
                    val amount = (adjustAmount.toIntOrNull() ?: 1).coerceIn(1, 100000)
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Voting #${o.optLong("id")} · ${o.optString("status", "")}", color = JellyInk, fontWeight = FontWeight.Black)
                            val left = o.optInt("left_votes", o.optInt("votes1", 0))
                            val right = o.optInt("right_votes", o.optInt("votes2", 0))
                            Text("Left: $left   ·   Right: $right", color = JellyMuted, fontSize = 10.sp)
                            Text("Admin vote adjustment", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.5f.sp)
                            Text("Add or reduce votes for either candidate. Every change is saved in the admin activity log.", color = JellyMuted, fontSize = 8.5f.sp)
                            OutlinedTextField(
                                adjustAmount,
                                { adjustAmount = it.filter(Char::isDigit).take(6) },
                                Modifier.fillMaxWidth(),
                                label = { Text("Amount") },
                                singleLine = true,
                                shape = RoundedCornerShape(15.dp)
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                JellyButton("Left Add", Modifier.weight(1f), icon = JellyIcons.Plus) {
                                    onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "left").put("delta", amount))
                                }
                                JellyButton("Left Reduce", Modifier.weight(1f), icon = JellyIcons.Delete) {
                                    onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "left").put("delta", -amount))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                JellyButton("Right Add", Modifier.weight(1f), icon = JellyIcons.Plus) {
                                    onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "right").put("delta", amount))
                                }
                                JellyButton("Right Reduce", Modifier.weight(1f), icon = JellyIcons.Delete) {
                                    onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "right").put("delta", -amount))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                JellyButton("End now", Modifier.weight(1f), icon = JellyIcons.Check) {
                                    onAction("vote_end", o.optLong("id"), JSONObject())
                                }
                                JellyButton("Cancel match", Modifier.weight(1f), icon = JellyIcons.Close) {
                                    onAction("vote_cancel", o.optLong("id"), JSONObject())
                                }
                                JellyButton("Delete record", Modifier.weight(1f), icon = JellyIcons.Delete, danger = true) {
                                    onAction("vote_delete", o.optLong("id"), JSONObject())
                                }
                            }
                        }
                    }
                }
            }

            "activity" -> items(rows, key = { "activity-${it.optLong("id")}" }) { o ->
                JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(o.optString("action", "activity"), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 11.5f.sp)
                        Text("User #${o.optLong("actor_id")} · ${o.optString("created_at", "")}", color = JellyMuted, fontSize = 9.sp)
                        if (o.optString("ip").isNotBlank()) Text("IP: ${o.optString("ip")}", color = JellyMuted, fontSize = 8.5f.sp)
                        val details = when {
                            o.optJSONObject("details") != null -> o.optJSONObject("details").toString()
                            o.optString("details").isNotBlank() -> o.optString("details")
                            else -> ""
                        }
                        if (details.isNotBlank()) Text(details.take(420), color = JellyMuted, fontSize = 8.sp, maxLines = 6)
                    }
                }
            }

            "deleted" -> items(rows, key = { "deleted-${it.optLong("id", it.optLong("original_user_id"))}" }) { o ->
                JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        val originalId = o.optLong("original_user_id", o.optLong("user_id"))
                        Text("Original User #$originalId", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text("Reason: ${o.optString("reason", "deleted")}", color = JellyMuted, fontSize = 9.5f.sp)
                        Text("Deleted: ${o.optString("deleted_at", o.optString("created_at", ""))}", color = JellyMuted, fontSize = 9.sp)
                        if (originalId > 0) {
                            JellyButton("Export Evidence PDF", icon = JellyIcons.Save) {
                                val url = Uri.parse("https://chhachh.pages.dev/evidence_pdf.php?user_id=$originalId")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                            }
                        }
                    }
                }
            }

            "records" -> {
                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                SectionTitle("User Records & PDF")
                                Text(
                                    "Search a user and open their profile. User controls and evidence PDF are on the profile.",
                                    color = JellyMuted,
                                    fontSize = 9.sp
                                )
                            }
                            JellyButton("Full Site PDF", icon = JellyIcons.Save) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://chhachh.pages.dev/full_export_pdf.php")
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                items(rows, key = { "record-${it.optLong("id")}" }) { o ->
                    val u = runCatching { o.toUser() }.getOrNull()
                    if (u != null) {
                        JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(u, 48.dp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    UserName(u, 12)
                                    Text("@${u.username}", color = JellyMuted, fontSize = 9.sp)
                                    val contact = listOf(u.email, u.phone).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (contact.isNotBlank()) Text(contact, color = JellyMuted, fontSize = 8.5f.sp)
                                    Text(
                                        if (o.optBoolean("online", false)) "Online" else "Offline",
                                        color = JellyMuted,
                                        fontSize = 8.5f.sp
                                    )
                                }
                                JellyButton("Open Profile & Controls", primary = true) { onProfile(u.id) }
                            }
                        }
                    }
                }
            }

            "notices" -> {
                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                SectionTitle("Announcements")
                                Text("Manage voice and community announcements.", color = JellyMuted, fontSize = 9.5f.sp)
                            }
                            JellyButton("Open Announcements", primary = true, icon = JellyIcons.Announcement, onClick = onAnnouncements)
                        }
                    }
                }
                if (notices.isEmpty()) item { EmptyCard("No announcements yet.", JellyIcons.Announcement) }
                items(notices, key = { "notice-${it.optLong("id")}" }) { o ->
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(o.optString("notice_type", "announcement").uppercase(), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            Text(
                                o.optString("text").ifBlank {
                                    when {
                                        o.optString("audio").isNotBlank() -> "Voice announcement"
                                        o.optString("photo").isNotBlank() -> "Photo announcement"
                                        else -> "Announcement"
                                    }
                                },
                                color = JellyInk,
                                fontSize = 10.5f.sp,
                                maxLines = 5
                            )
                            Text(
                                "${o.optString("created_at", "")} · ${if (o.optInt("active", 1) == 0) "Hidden" else "Visible"}",
                                color = JellyMuted,
                                fontSize = 8.5f.sp
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                JellyButton(if (o.optInt("active", 1) == 0) "Show" else "Hide", Modifier.weight(1f)) {
                                    onAction("toggle_admin_notice", o.optLong("id"), JSONObject())
                                }
                                JellyButton("Delete", Modifier.weight(1f), icon = JellyIcons.Delete, danger = true) {
                                    onAction("delete_admin_notice", o.optLong("id"), JSONObject())
                                }
                            }
                        }
                    }
                }
            }

            "features" -> {
                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Feature Controls")
                            Text("Live backend switches from the V95 website.", color = JellyMuted, fontSize = 9.5f.sp)
                            val keys = listOf(
                                "likes", "comments", "shares", "user_profiles", "shops", "user_posts",
                                "messaging", "follows", "videos", "comment_likes", "comment_replies", "mentions"
                            )
                            keys.forEach { key ->
                                FeatureToggleRow(key, settings.optInt(key, 1) != 0) { value ->
                                    onAction("feature", 0L, JSONObject().put("key", key).put("value", if (value) 1 else 0))
                                }
                            }
                            JellyButton("Open Complete Theme Builder", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Palette, onClick = onTheme)
                        }
                    }
                }
            }

            "installer" -> {
                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Feature Installer")
                            Text(
                                "Install future My Chhachh feature packages without replacing the core website.",
                                color = JellyMuted,
                                fontSize = 9.5f.sp
                            )
                            JellyButton("Choose Feature Package", Modifier.fillMaxWidth(), icon = JellyIcons.Plus) {
                                featurePackagePicker.launch("*/*")
                            }
                            OutlinedTextField(
                                featurePackageText,
                                { featurePackageText = it },
                                Modifier.fillMaxWidth(),
                                label = { Text("Feature package JSON") },
                                minLines = 5,
                                maxLines = 12,
                                shape = RoundedCornerShape(17.dp)
                            )
                            JellyButton(
                                "Check & Install Feature",
                                Modifier.fillMaxWidth(),
                                primary = true,
                                icon = JellyIcons.Plus,
                                enabled = featurePackageText.trim().isNotBlank()
                            ) {
                                onAction("feature_package_install", 0L, JSONObject().put("package_text", featurePackageText.trim()))
                            }
                        }
                    }
                }
                if (packages.isEmpty()) item { EmptyCard("No extra features installed yet.", JellyIcons.Gear) }
                items(packages, key = { "package-${it.optString("id")}" }) { p ->
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(p.optString("name", p.optString("id", "Feature")), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 11.5f.sp)
                            Text("v${p.optString("version", "1.0.0")} · ${if (p.optBoolean("enabled", true)) "ON" else "OFF"}", color = JellyMuted, fontSize = 9.sp)
                            if (p.optString("description").isNotBlank()) Text(p.optString("description"), color = JellyMuted, fontSize = 9.sp, maxLines = 4)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                JellyButton(if (p.optBoolean("enabled", true)) "Turn Off" else "Turn On", Modifier.weight(1f)) {
                                    onAction(
                                        "feature_package_toggle",
                                        0L,
                                        JSONObject().put("feature_id", p.optString("id")).put("enabled", if (p.optBoolean("enabled", true)) 0 else 1)
                                    )
                                }
                                if (p.optInt("history_count", 0) > 0) {
                                    JellyButton("Restore", Modifier.weight(1f)) {
                                        onAction("feature_package_rollback", 0L, JSONObject().put("feature_id", p.optString("id")))
                                    }
                                }
                                JellyButton("Remove", Modifier.weight(1f), danger = true) {
                                    onAction("feature_package_remove", 0L, JSONObject().put("feature_id", p.optString("id")))
                                }
                            }
                        }
                    }
                }
            }

            "ads" -> {
                item {
                    var enabled by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_enabled", 0) != 0) }
                    var home by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_home", 1) != 0) }
                    var shopList by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_shop_list", 1) != 0) }
                    var shopDetail by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_shop_detail", 1) != 0) }
                    var search by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_search", 1) != 0) }
                    var label by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_label", 1) != 0) }
                    var client by remember(settings.toString()) { mutableStateOf(settings.optString("google_ads_client", "")) }
                    var slot by remember(settings.toString()) { mutableStateOf(settings.optString("google_ads_slot", "")) }
                    var interval by remember(settings.toString()) { mutableStateOf(settings.optInt("google_ads_interval", 10).toString()) }
                    var style by remember(settings.toString()) { mutableStateOf(settings.optString("google_ads_style", "soft")) }

                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            SectionTitle("Ad Manager")
                            Text("Google ads stay inside selected card placements.", color = JellyMuted, fontSize = 9.5f.sp)
                            ThemeSwitch("Enable Google ads", enabled) { enabled = it }
                            ThemeSwitch("Home feed", home) { home = it }
                            ThemeSwitch("Shop listing", shopList) { shopList = it }
                            ThemeSwitch("Shop detail", shopDetail) { shopDetail = it }
                            ThemeSwitch("Search", search) { search = it }
                            ThemeSwitch("Sponsored label", label) { label = it }
                            OutlinedTextField(client, { client = it }, Modifier.fillMaxWidth(), label = { Text("AdSense client") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(slot, { slot = it }, Modifier.fillMaxWidth(), label = { Text("Ad slot") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(interval, { interval = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(), label = { Text("Posts between ads") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            JellyButton("Style: ${style.replaceFirstChar { it.uppercase() }}", Modifier.fillMaxWidth()) {
                                style = when (style) { "soft" -> "plain"; "plain" -> "minimal"; else -> "soft" }
                            }
                            JellyButton("Save Ad Settings", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check) {
                                onAction(
                                    "google_ads",
                                    0L,
                                    JSONObject()
                                        .put("enabled", enabled)
                                        .put("home", home)
                                        .put("shop_list", shopList)
                                        .put("shop_detail", shopDetail)
                                        .put("search", search)
                                        .put("label", label)
                                        .put("client", client.trim())
                                        .put("slot", slot.trim())
                                        .put("interval", interval.toIntOrNull()?.coerceIn(3, 50) ?: 10)
                                        .put("style", style)
                                )
                            }
                        }
                    }
                }
            }

            "traffic" -> {
                item {
                    val t = traffic?.optJSONObject("traffic") ?: traffic ?: JSONObject()
                    val today = t.optJSONObject("today") ?: JSONObject()
                    val yesterday = t.optJSONObject("yesterday") ?: JSONObject()
                    val live = t.optInt("live")
                    val liveMembers = t.optInt("live_members")
                    val liveGuests = t.optInt("live_guests")
                    val liveBase = maxOf(10, live, today.optInt("visitors"))
                    val meter = (live.toFloat() / liveBase.toFloat()).coerceIn(0.03f, 1f)
                    val days = jsonObjects(t.optJSONArray("days"))
                    val livePaths = jsonObjects(t.optJSONArray("live_paths"))
                    val maxViews = days.maxOfOrNull { it.optInt("views") }?.coerceAtLeast(1) ?: 1
                    val maxPath = livePaths.maxOfOrNull { it.optInt("visitors") }?.coerceAtLeast(1) ?: 1

                    JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    SectionTitle("Professional Traffic Meter")
                                    Text(
                                        "Real server-side traffic. Admin visits and known bots are excluded.",
                                        color = JellyMuted,
                                        fontSize = 9.5f.sp
                                    )
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TrafficLiveMeter(
                                    live = live,
                                    members = liveMembers,
                                    guests = liveGuests,
                                    progress = meter,
                                    modifier = Modifier.size(148.dp)
                                )
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TrafficTodayCard(
                                        "Views today",
                                        today.optInt("views").toString(),
                                        trafficChangeText(t.optDouble("change_views", 0.0))
                                    )
                                    TrafficTodayCard(
                                        "Unique visitors",
                                        today.optInt("visitors").toString(),
                                        trafficChangeText(t.optDouble("change_visitors", 0.0))
                                    )
                                    val views = today.optInt("views").coerceAtLeast(1)
                                    val memberPct = (today.optInt("logged_in_views").toFloat() / views.toFloat()).coerceIn(0f, 1f)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .background(Color(0x1F66738B), RoundedCornerShape(999.dp))
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth(memberPct)
                                                    .fillMaxHeight()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF5AD8FF), Color(0xFF9A7BFF), Color(0xFFF26BCF))
                                                        ),
                                                        RoundedCornerShape(999.dp)
                                                    )
                                            )
                                        }
                                        Text(
                                            "${today.optInt("logged_in_views")} member views · ${today.optInt("guest_views")} guest views",
                                            color = JellyMuted,
                                            fontSize = 8.5f.sp
                                        )
                                    }
                                }
                            }

                            val kpis = listOf(
                                Triple("Yesterday", yesterday.optInt("views").toString(), "${yesterday.optInt("visitors")} visitors"),
                                Triple("7-day views", t.optInt("last7_views").toString(), "${t.optInt("last7_visitors")} visitors"),
                                Triple("30-day views", t.optInt("last30_views").toString(), "${t.optInt("last30_visitors")} visitors"),
                                Triple("Daily average", t.optInt("avg7_views").toString(), "${t.optInt("avg7_visitors")} visitors"),
                                Triple(
                                    "Peak day",
                                    t.optJSONObject("peak_day")?.optInt("views", 0)?.toString() ?: "0",
                                    t.optJSONObject("peak_day")?.optString("day", "No data yet") ?: "No data yet"
                                ),
                                Triple("Tracker", "LIVE", "Server-side counted")
                            )
                            kpis.chunked(3).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    row.forEach { (label, value, note) ->
                                        TrafficKpi(label, value, note, Modifier.weight(1f))
                                    }
                                }
                            }

                            JellyGlass(Modifier.fillMaxWidth(), radius = 20.dp, padding = 11.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Last 30 days", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    Text("Views per day", color = JellyMuted, fontSize = 8.5f.sp)
                                    if (days.isEmpty()) {
                                        Text("Traffic history will appear after visits are recorded.", color = JellyMuted, fontSize = 9.sp)
                                    } else {
                                        Row(
                                            Modifier.fillMaxWidth().height(150.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            days.take(30).reversed().forEach { day ->
                                                val ratio = (day.optInt("views").toFloat() / maxViews.toFloat()).coerceIn(0.04f, 1f)
                                                Column(
                                                    Modifier.weight(1f).fillMaxHeight(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Bottom
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .widthIn(min = 4.dp, max = 13.dp)
                                                            .fillMaxWidth()
                                                            .fillMaxHeight(ratio)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    listOf(Color(0xFF63D9FF), Color(0xFF8F7CFF), Color(0xFFF16CCA))
                                                                ),
                                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                                                            )
                                                    )
                                                    Spacer(Modifier.height(3.dp))
                                                    Text(
                                                        day.optString("day").takeLast(2),
                                                        color = JellyMuted,
                                                        fontSize = 6.5f.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            JellyGlass(Modifier.fillMaxWidth(), radius = 20.dp, padding = 11.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Live pages", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    Text("Visitors active in the last 5 minutes", color = JellyMuted, fontSize = 8.5f.sp)
                                    if (livePaths.isEmpty()) {
                                        Text("No active visitors right now.", color = JellyMuted, fontSize = 9.sp)
                                    } else {
                                        livePaths.forEach { p ->
                                            val visitors = p.optInt("visitors")
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row {
                                                    Text(
                                                        p.optString("path", "/"),
                                                        Modifier.weight(1f),
                                                        color = JellyInk,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    Text(visitors.toString(), color = JellyInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                                }
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .background(Color(0x1F66738B), RoundedCornerShape(999.dp))
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .fillMaxWidth((visitors.toFloat() / maxPath.toFloat()).coerceIn(0.06f, 1f))
                                                            .fillMaxHeight()
                                                            .background(
                                                                Brush.horizontalGradient(listOf(Color(0xFF5BD7FF), Color(0xFFF16CCB))),
                                                                RoundedCornerShape(999.dp)
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                "Unique visitors are counted per day. Live means activity within the last 5 minutes.",
                                color = JellyMuted,
                                fontSize = 8.3f.sp
                            )
                        }
                    }
                }
            }

            "social" -> {
                item {
                    var googleClient by remember(settings.toString()) { mutableStateOf(settings.optString("google_client_id", "")) }
                    var facebookId by remember(settings.toString()) { mutableStateOf(settings.optString("facebook_app_id", "")) }
                    var facebookSecret by remember { mutableStateOf("") }
                    var clearSecret by remember { mutableStateOf(false) }
                    val passOn = settings.optInt("password_login_enabled", 1) != 0
                    val regOn = settings.optInt("registration_enabled", 1) != 0
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Login & Registration Setup")
                            FeatureToggleRow("Email/password login", passOn) { value ->
                                onAction("feature", 0L, JSONObject().put("key", "password_login_enabled").put("value", if (value) 1 else 0))
                            }
                            FeatureToggleRow("New registrations", regOn) { value ->
                                onAction("feature", 0L, JSONObject().put("key", "registration_enabled").put("value", if (value) 1 else 0))
                            }
                            OutlinedTextField(googleClient, { googleClient = it }, Modifier.fillMaxWidth(), label = { Text("Google Web Client ID") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(facebookId, { facebookId = it.filter(Char::isDigit).take(30) }, Modifier.fillMaxWidth(), label = { Text("Facebook App ID") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(facebookSecret, { facebookSecret = it }, Modifier.fillMaxWidth(), label = { Text(if (settings.optBoolean("facebook_app_secret_set", false)) "Facebook secret (leave blank to keep)" else "Facebook App Secret") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            ThemeSwitch("Clear saved Facebook secret", clearSecret) { clearSecret = it }
                            JellyButton("Save Login Setup", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check) {
                                onAction(
                                    "social_auth_settings",
                                    0L,
                                    JSONObject()
                                        .put("google_client_id", googleClient.trim())
                                        .put("facebook_app_id", facebookId.trim())
                                        .put("facebook_app_secret", facebookSecret)
                                        .put("clear_facebook_secret", clearSecret)
                                )
                            }
                        }
                    }
                }
            }

            "adminprofile" -> {
                item {
                    var adminName by remember(me.id, me.name) { mutableStateOf(me.name) }
                    var adminUsername by remember(me.id, me.username) { mutableStateOf(me.username) }
                    var adminPhone by remember(me.id, me.phone) { mutableStateOf(me.phone) }
                    var adminEmail by remember(me.id, me.email) { mutableStateOf(me.email) }
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Admin Profile")
                            OutlinedTextField(adminName, { adminName = it }, Modifier.fillMaxWidth(), label = { Text("Admin name") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(adminUsername, { adminUsername = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(30) }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(adminPhone, { adminPhone = it.filter { c -> c.isDigit() || c == '+' }.take(16) }, Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            OutlinedTextField(adminEmail, { adminEmail = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, shape = RoundedCornerShape(17.dp))
                            JellyButton("Save Admin Profile", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check) {
                                onAction(
                                    "admin_profile",
                                    0L,
                                    JSONObject()
                                        .put("name", adminName.trim())
                                        .put("username", adminUsername.trim())
                                        .put("phone", adminPhone.trim())
                                        .put("email", adminEmail.trim())
                                )
                            }
                        }
                    }
                }

                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Admin Announcement")
                            Text("Users can like and comment on it in Announcements.", color = JellyMuted, fontSize = 9.5f.sp)
                            JellyButton("Type: ${noticeType.replaceFirstChar { it.uppercase() }}", Modifier.fillMaxWidth()) {
                                noticeType = when (noticeType) {
                                    "announcement" -> "emergency"
                                    "emergency" -> "ad"
                                    "ad" -> "info"
                                    else -> "announcement"
                                }
                            }
                            OutlinedTextField(noticeText, { noticeText = it.take(5000) }, Modifier.fillMaxWidth(), label = { Text("Announcement text") }, minLines = 3, maxLines = 7, shape = RoundedCornerShape(17.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                JellyButton(if (noticePhoto != null) "Photo ✓" else "Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { noticePhotoPicker.launch("image/*") }
                                JellyButton(if (noticeAudio != null) "Voice ✓" else "Voice", Modifier.weight(1f), icon = JellyIcons.Announcement) { noticeAudioPicker.launch("audio/*") }
                            }
                            JellyButton(
                                "Publish Announcement",
                                Modifier.fillMaxWidth(),
                                primary = true,
                                icon = JellyIcons.Send,
                                enabled = noticeText.isNotBlank() || noticePhoto != null || noticeAudio != null
                            ) {
                                onPublishAnnouncement(noticeType, noticeText.trim(), noticePhoto, noticeAudio)
                                noticeText = ""; noticePhoto = null; noticeAudio = null
                            }
                        }
                    }
                }

                item {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 11.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Normal Admin Post")
                            OutlinedTextField(adminPostText, { adminPostText = it.take(5000) }, Modifier.fillMaxWidth(), label = { Text("Feed post text") }, minLines = 3, maxLines = 7, shape = RoundedCornerShape(17.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                JellyButton(if (adminPostPhoto != null) "Photo ✓" else "Photo", Modifier.weight(1f), icon = JellyIcons.Photo) { adminPhotoPicker.launch("image/*") }
                                JellyButton(if (adminPostVideo != null) "Video ✓" else "Video", Modifier.weight(1f), icon = JellyIcons.Video) { adminVideoPicker.launch("video/*") }
                            }
                            JellyButton(
                                "Publish Admin Post",
                                Modifier.fillMaxWidth(),
                                primary = true,
                                icon = JellyIcons.Send,
                                enabled = adminPostText.isNotBlank() || adminPostPhoto != null || adminPostVideo != null
                            ) {
                                onPublishAdminPost(adminPostText.trim(), adminPostPhoto, adminPostVideo)
                                adminPostText = ""; adminPostPhoto = null; adminPostVideo = null
                            }
                        }
                    }
                }
            }
        }

        if (!loading && section in pagedSections && rows.isEmpty() && error == null) {
            item { EmptyCard("No records found.", JellyIcons.Shield) }
        }
    }

    replyTarget?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("Reply to Report", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    replyText,
                    { replyText = it.take(3000) },
                    Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    placeholder = { Text("Write reply to user...") },
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Send Reply", primary = true, icon = JellyIcons.Send, enabled = replyText.isNotBlank()) {
                    onAction("report_reply", id, JSONObject().put("reply", replyText.trim()))
                    replyTarget = null
                }
            },
            dismissButton = { JellyButton("Cancel") { replyTarget = null } }
        )
    }

    warningTarget?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { warningTarget = null },
            title = { Text("Send Warning", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    warningText,
                    { warningText = it.take(2000) },
                    Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 7,
                    placeholder = { Text("Warning message…") },
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Send Warning", primary = true, icon = JellyIcons.Bell, enabled = warningText.isNotBlank()) {
                    onAction("warning", id, JSONObject().put("message", warningText.trim()))
                    warningTarget = null
                }
            },
            dismissButton = { JellyButton("Cancel") { warningTarget = null } }
        )
    }


    userPostTarget?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { userPostTarget = null },
            title = { Text("Publish as User", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    userPostText,
                    { userPostText = it.take(5000) },
                    Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 9,
                    placeholder = { Text("Write the post to publish as this user…") },
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton("Publish", primary = true, icon = JellyIcons.Send, enabled = userPostText.isNotBlank()) {
                    onAction("admin_user_post", id, JSONObject().put("text", userPostText.trim()))
                    userPostTarget = null
                }
            },
            dismissButton = { JellyButton("Cancel") { userPostTarget = null } }
        )
    }

    verificationTarget?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { verificationTarget = null },
            title = {
                Text(
                    if (verificationDecision == "approved") "Approve Verification" else "Reject Verification",
                    color = JellyInk,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                OutlinedTextField(
                    verificationNote,
                    { verificationNote = it.take(2000) },
                    Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 7,
                    placeholder = { Text("Admin note (optional)…") },
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                JellyButton(
                    if (verificationDecision == "approved") "Approve" else "Reject",
                    primary = verificationDecision == "approved",
                    icon = if (verificationDecision == "approved") JellyIcons.Check else JellyIcons.Close
                ) {
                    onAction(
                        "verification_review",
                        id,
                        JSONObject()
                            .put("decision", verificationDecision)
                            .put("admin_note", verificationNote.trim())
                    )
                    verificationTarget = null
                }
            },
            dismissButton = { JellyButton("Cancel") { verificationTarget = null } }
        )
    }

    deleteUserTarget?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteUserTarget = null },
            title = { Text("Delete User Account", color = JellyInk, fontWeight = FontWeight.Black) },
            text = { Text("Delete this user account? Preserved admin evidence remains on the server.", color = JellyInk) },
            confirmButton = {
                JellyButton("Delete", icon = JellyIcons.Delete, danger = true) {
                    onAction("delete_user", id, JSONObject())
                    deleteUserTarget = null
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteUserTarget = null } }
        )
    }
}

private data class AdminNavItem(
    val key: String,
    val label: String,
    val icon: Int,
    val badge: Int = 0
)

private fun adminSectionTitle(section: String): String = when (section) {
    "overview" -> "Overview"
    "users" -> "Users"
    "verification" -> "Verification"
    "posts" -> "Posts"
    "shops" -> "Shops"
    "votes" -> "Voting"
    "reports" -> "Reports & Support"
    "deleted" -> "Deleted Accounts"
    "activity" -> "Activity"
    "records" -> "User Records"
    "notices" -> "Announcements"
    "features" -> "Features"
    "installer" -> "Feature Installer"
    "ads" -> "Ad Manager"
    "traffic" -> "Traffic"
    "social" -> "Login Setup"
    "adminprofile" -> "Admin Profile"
    else -> "Admin Center"
}

@Composable
private fun AdminNavGroup(
    title: String,
    items: List<AdminNavItem>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 3.dp))
        items.chunked(3).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { item ->
                    val active = selected == item.key
                    JellyGlass(
                        Modifier.weight(1f).heightIn(min = 76.dp),
                        radius = 16.dp,
                        padding = 5.dp,
                        onClick = { onSelect(item.key) },
                        surfaceColor = if (active) Color.White else LiveJellyTheme.cardColor,
                        surfaceOpacity = if (active) .72f else .34f
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                JellyIcon(item.icon, size = 34.dp)
                                Text(
                                    item.label,
                                    color = if (active) LiveJellyTheme.activeColor else JellyInk,
                                    fontSize = 8.5f.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 2
                                )
                            }
                            if (item.badge > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .background(Color(0xFFFF4D88), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        if (item.badge > 99) "99+" else item.badge.toString(),
                                        color = Color.White,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VerificationRuleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JellyIcon(JellyIcons.Shield, size = 25.dp)
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 10.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = JellyMuted, fontSize = 8.2f.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun FeatureToggleRow(label: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.replace('_', ' ').replaceFirstChar { it.uppercase() },
            Modifier.weight(1f),
            color = JellyInk,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5f.sp
        )
        JellyButton(if (enabled) "Turn OFF" else "Turn ON", primary = !enabled, danger = enabled) {
            onChange(!enabled)
        }
    }
}

@Composable
private fun AdminMenuRow(
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit
) {
    JellyGlass(
        Modifier.fillMaxWidth(),
        radius = 14.dp,
        padding = 10.dp,
        onClick = onClick,
        surfaceColor = LiveJellyTheme.cardColor,
        surfaceOpacity = .98f
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JellyIcon(icon, size = 27.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = JellyMuted, fontSize = 9.sp, maxLines = 1)
            }
            Text("›", color = JellyMuted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminStat(label: String, value: Int, modifier: Modifier = Modifier) {
    JellyGlass(modifier, radius = 18.dp, padding = 9.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(label, color = JellyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NativeThemeScreen(
    settings: JSONObject,
    busy: Boolean,
    error: String?,
    onSaveTheme: (JSONObject) -> Unit,
    onSaveBrand: (JSONObject, Uri?) -> Unit
) {
    val stateKey = settings.toString()
    var siteName by remember(stateKey) { mutableStateOf(settings.optString("site_name", "My Chhachh")) }
    var tagline by remember(stateKey) { mutableStateOf(settings.optString("site_tagline", "Connect with people for information.")) }
    var iconEnabled by remember(stateKey) { mutableStateOf(settings.optInt("site_icon_enabled", 1) != 0) }
    var taglineEnabled by remember(stateKey) { mutableStateOf(settings.optInt("site_tagline_enabled", 1) != 0) }
    var iconUri by remember { mutableStateOf<Uri?>(null) }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) iconUri = it }

    var themeEnabled by remember(stateKey) { mutableStateOf(settings.optInt("theme_enabled", 1) != 0) }
    var jelly by remember(stateKey) { mutableStateOf(settings.optInt("theme_jelly_enabled", 1) != 0) }
    var weatherLive by remember(stateKey) { mutableStateOf(settings.optInt("theme_weather_live", 1) != 0) }
    var motion by remember(stateKey) { mutableStateOf(settings.optInt("theme_motion", 1) != 0) }
    var rainbow by remember(stateKey) { mutableStateOf(settings.optInt("theme_brand_rainbow", 1) != 0) }
    var weatherChip by remember(stateKey) { mutableStateOf(settings.optInt("theme_weather_chip", 0) != 0) }
    var themeIconEnabled by remember(stateKey) { mutableStateOf(settings.optInt("theme_theme_icon_enabled", 1) != 0) }
    var jellyDepth by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_jelly_depth", 92).toFloat()) }
    var jellyShine by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_jelly_shine", 96).toFloat()) }
    var jellyBorder by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_jelly_border", 92).toFloat()) }
    var jellySaturation by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_jelly_saturation", 140).toFloat()) }
    var timeMode by remember(stateKey) { mutableStateOf(settings.optString("theme_time_mode", "auto").ifBlank { "auto" }) }
    var manualWeather by remember(stateKey) { mutableStateOf(settings.optString("theme_manual_weather", "clear").ifBlank { "clear" }) }

    var cardOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_card_opacity", 97).toFloat()) }
    var headerOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_header_opacity", 97).toFloat()) }
    var navOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_nav_opacity", 96).toFloat()) }
    var inputOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_input_opacity", 96).toFloat()) }
    var blur by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_blur", 18).toFloat()) }
    var cardRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_card_radius", 22).toFloat()) }
    var headerRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_header_radius", 28).toFloat()) }
    var navRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_nav_radius", 17).toFloat()) }
    var buttonRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_button_radius", 14).toFloat()) }
    var inputRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_input_radius", 16).toFloat()) }
    var shadow by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_shadow", 12).toFloat()) }
    var fontScale by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_font_scale", 100).toFloat()) }
    var pageWidth by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_page_width", 940).toFloat()) }
    var cardPadding by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_card_padding", 18).toFloat()) }
    var sectionGap by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_section_gap", 14).toFloat()) }
    var navHeight by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_nav_height", 64).toFloat()) }
    var buttonHeight by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_button_height", 44).toFloat()) }
    var profileCover by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_profile_cover_height", 175).toFloat()) }
    var profileAvatar by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_profile_avatar_size", 92).toFloat()) }
    var shopCover by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_shop_cover_height", 185).toFloat()) }
    var shopAvatar by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_shop_avatar_size", 92).toFloat()) }

    var headerColor by remember(stateKey) { mutableStateOf(settings.optString("theme_header_color", "#ffffff")) }
    var headerText by remember(stateKey) { mutableStateOf(settings.optString("theme_header_text", "#0d1a34")) }
    var navColor by remember(stateKey) { mutableStateOf(settings.optString("theme_nav_color", "#ffffff")) }
    var iconColor by remember(stateKey) { mutableStateOf(settings.optString("theme_icon_color", "#10203d")) }
    var activeColor by remember(stateKey) { mutableStateOf(settings.optString("theme_active_color", "#1683ff")) }
    var textColor by remember(stateKey) { mutableStateOf(settings.optString("theme_text_color", "#0d1a34")) }
    var mutedColor by remember(stateKey) { mutableStateOf(settings.optString("theme_muted_color", "#66738b")) }
    var cardColor by remember(stateKey) { mutableStateOf(settings.optString("theme_card_color", "#ffffff")) }
    var borderColor by remember(stateKey) { mutableStateOf(settings.optString("theme_border_color", "#e8edf2")) }
    var buttonColor by remember(stateKey) { mutableStateOf(settings.optString("theme_button_color", "#1683ff")) }
    var inputColor by remember(stateKey) { mutableStateOf(settings.optString("theme_input_color", "#f4f7fa")) }
    var accent by remember(stateKey) { mutableStateOf(settings.optString("theme_accent", "#1683ff")) }
    var accent2 by remember(stateKey) { mutableStateOf(settings.optString("theme_accent2", "#7c3aed")) }

    var bgClear by remember(stateKey) { mutableStateOf(settings.optString("theme_bg_clear", "")) }
    var bgClouds by remember(stateKey) { mutableStateOf(settings.optString("theme_bg_clouds", "")) }
    var bgRain by remember(stateKey) { mutableStateOf(settings.optString("theme_bg_rain", "")) }
    var bgFog by remember(stateKey) { mutableStateOf(settings.optString("theme_bg_fog", "")) }
    var bgStorm by remember(stateKey) { mutableStateOf(settings.optString("theme_bg_storm", "")) }
    var bgNight by remember(stateKey) { mutableStateOf(settings.optString("theme_bg_night", "")) }
    var homeOrder by remember(stateKey) { mutableStateOf(settings.optString("theme_home_order", "notice,tabs,composer")) }
    var headerItems by remember(stateKey) { mutableStateOf(settings.optString("theme_header_items", "home,people,shop,map,messages")) }
    var menuItems by remember(stateKey) { mutableStateOf(settings.optString("theme_menu_items", "votes,saved,settings,theme,admin,logout")) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Theme Builder", "Theme and branding controls only. Admin controls stay in Admin Center.", JellyIcons.Palette) }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Complete Theme Control")
                    Text("Logo, every jelly icon, all main frames, sizes, spacing, colors, header/menu order and live background.", color = JellyMuted, fontSize = 9.5f.sp)
                    OutlinedTextField(siteName, { siteName = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Website / App name") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    OutlinedTextField(tagline, { tagline = it.take(120) }, Modifier.fillMaxWidth(), label = { Text("Tagline") }, shape = RoundedCornerShape(17.dp), singleLine = true)
                    ThemeSwitch("Show logo", iconEnabled) { iconEnabled = it }
                    ThemeSwitch("Show tagline", taglineEnabled) { taglineEnabled = it }
                    JellyButton(if (iconUri != null) "New Logo Selected ✓" else "Choose Logo", Modifier.fillMaxWidth(), icon = JellyIcons.Photo) {
                        iconPicker.launch("image/*")
                    }
                    JellyButton("Save Branding", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Check, enabled = !busy && siteName.isNotBlank()) {
                        onSaveBrand(
                            JSONObject()
                                .put("site_name", siteName.trim())
                                .put("site_tagline", tagline.trim())
                                .put("site_icon_enabled", if (iconEnabled) 1 else 0)
                                .put("site_tagline_enabled", if (taglineEnabled) 1 else 0)
                                .put("site_icon_fit", "contain")
                                .put("site_icon_header_blend", true)
                                .put("site_icon_auto_transparent", true)
                                .put("site_icon_size", 40),
                            iconUri
                        )
                    }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("Jelly depth & live background")
                    ThemeSlider("Jelly depth", jellyDepth, 0f..100f) { jellyDepth = it }
                    ThemeSlider("Jelly shine", jellyShine, 0f..100f) { jellyShine = it }
                    ThemeSlider("Jelly border", jellyBorder, 0f..100f) { jellyBorder = it }
                    ThemeSlider("Jelly saturation", jellySaturation, 80f..180f) { jellySaturation = it }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JellyButton(
                            "Time mode: " + when (timeMode) {
                                "day" -> "Day"
                                "night" -> "Night"
                                else -> "Automatic"
                            },
                            Modifier.weight(1f)
                        ) {
                            timeMode = when (timeMode) { "auto" -> "day"; "day" -> "night"; else -> "auto" }
                        }
                        JellyButton(
                            "Manual weather: " + manualWeather.replaceFirstChar { it.uppercase() },
                            Modifier.weight(1f)
                        ) {
                            manualWeather = when (manualWeather) {
                                "clear" -> "clouds"
                                "clouds" -> "rain"
                                "rain" -> "fog"
                                "fog" -> "storm"
                                "storm" -> "snow"
                                else -> "clear"
                            }
                        }
                    }

                    ThemeSwitch("Live weather background", weatherLive) { weatherLive = it }
                    ThemeSwitch("Background motion", motion) { motion = it }
                    ThemeSwitch("Rainbow website name", rainbow) { rainbow = it }
                    ThemeSwitch("Jelly theme", jelly) { jelly = it }
                    ThemeSwitch("Weather chip", weatherChip) { weatherChip = it }
                    ThemeSwitch("Theme Builder in menu", themeIconEnabled) { themeIconEnabled = it }
                    ThemeSwitch("Enable theme", themeEnabled) { themeEnabled = it }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("Opacity, Shape & Layout")
                    ThemeSlider("Card opacity", cardOpacity, 45f..100f) { cardOpacity = it }
                    ThemeSlider("Header opacity", headerOpacity, 45f..100f) { headerOpacity = it }
                    ThemeSlider("Navigation opacity", navOpacity, 45f..100f) { navOpacity = it }
                    ThemeSlider("Input opacity", inputOpacity, 45f..100f) { inputOpacity = it }
                    ThemeSlider("Glass blur", blur, 0f..40f) { blur = it }
                    ThemeSlider("Card radius", cardRadius, 8f..36f) { cardRadius = it }
                    ThemeSlider("Header radius", headerRadius, 0f..42f) { headerRadius = it }
                    ThemeSlider("Navigation radius", navRadius, 6f..32f) { navRadius = it }
                    ThemeSlider("Button radius", buttonRadius, 6f..32f) { buttonRadius = it }
                    ThemeSlider("Input radius", inputRadius, 6f..40f) { inputRadius = it }
                    ThemeSlider("Shadow", shadow, 0f..30f) { shadow = it }
                    ThemeSlider("Font scale", fontScale, 85f..120f) { fontScale = it }
                    ThemeSlider("Content width", pageWidth, 760f..1200f) { pageWidth = it }
                    ThemeSlider("Card padding", cardPadding, 10f..32f) { cardPadding = it }
                    ThemeSlider("Section spacing", sectionGap, 6f..30f) { sectionGap = it }
                    ThemeSlider("Navigation height", navHeight, 48f..78f) { navHeight = it }
                    ThemeSlider("Button height", buttonHeight, 34f..60f) { buttonHeight = it }
                    ThemeSlider("Profile cover", profileCover, 120f..220f) { profileCover = it }
                    ThemeSlider("Profile avatar", profileAvatar, 70f..120f) { profileAvatar = it }
                    ThemeSlider("Shop cover", shopCover, 100f..320f) { shopCover = it }
                    ThemeSlider("Shop avatar", shopAvatar, 60f..170f) { shopAvatar = it }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("Colors")
                    ThemeColorField("Header", headerColor) { headerColor = it }
                    ThemeColorField("Header text", headerText) { headerText = it }
                    ThemeColorField("Navigation", navColor) { navColor = it }
                    ThemeColorField("Icon", iconColor) { iconColor = it }
                    ThemeColorField("Active icon", activeColor) { activeColor = it }
                    ThemeColorField("Main text", textColor) { textColor = it }
                    ThemeColorField("Muted text", mutedColor) { mutedColor = it }
                    ThemeColorField("Card", cardColor) { cardColor = it }
                    ThemeColorField("Borders", borderColor) { borderColor = it }
                    ThemeColorField("Buttons", buttonColor) { buttonColor = it }
                    ThemeColorField("Inputs", inputColor) { inputColor = it }
                    ThemeColorField("Accent 1", accent) { accent = it }
                    ThemeColorField("Accent 2", accent2) { accent2 = it }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("Weather Background URLs")
                    Text("Leave blank to use the built-in natural backgrounds.", color = JellyMuted, fontSize = 9.5f.sp)
                    ThemeTextField("Clear day", bgClear) { bgClear = it }
                    ThemeTextField("Cloudy", bgClouds) { bgClouds = it }
                    ThemeTextField("Rain", bgRain) { bgRain = it }
                    ThemeTextField("Fog", bgFog) { bgFog = it }
                    ThemeTextField("Storm", bgStorm) { bgStorm = it }
                    ThemeTextField("Night", bgNight) { bgNight = it }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Order & Visibility")
                    Text("Show or hide items and move them up or down.", color = JellyMuted, fontSize = 9.sp)

                    ThemeOrderEditor(
                        title = "Home order",
                        value = homeOrder,
                        allowed = listOf("notice", "tabs", "composer"),
                        labels = mapOf("notice" to "Notice", "tabs" to "Feed tabs", "composer" to "Post composer"),
                        onValue = { homeOrder = it }
                    )
                    ThemeOrderEditor(
                        title = "Header navigation",
                        value = headerItems,
                        allowed = listOf("home", "people", "shop", "map", "messages"),
                        labels = mapOf("home" to "Home", "people" to "People", "shop" to "Shop", "map" to "Map", "messages" to "Messages"),
                        onValue = { headerItems = it }
                    )
                    ThemeOrderEditor(
                        title = "Side menu",
                        value = menuItems,
                        allowed = listOf("home", "people", "shop", "votes", "saved", "map", "messages", "announcements", "notifications", "profile", "settings", "theme", "admin", "logout"),
                        labels = mapOf(
                            "home" to "Home", "people" to "People", "shop" to "Shop", "votes" to "Voting",
                            "saved" to "Saved", "map" to "Map", "messages" to "Messages", "announcements" to "Announcements",
                            "notifications" to "Notifications", "profile" to "Profile", "settings" to "Settings",
                            "theme" to "Theme Builder", "admin" to "Admin Center", "logout" to "Logout"
                        ),
                        onValue = { menuItems = it }
                    )
                }
            }
        }

        error?.let { item { ErrorCard(it) } }

        item {
            JellyButton("Save & Apply Complete Theme", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Palette, enabled = !busy) {
                onSaveTheme(
                    JSONObject()
                        .put("theme_enabled", if (themeEnabled) 1 else 0)
                        .put("theme_jelly_enabled", if (jelly) 1 else 0)
                        .put("theme_weather_live", if (weatherLive) 1 else 0)
                        .put("theme_motion", if (motion) 1 else 0)
                        .put("theme_brand_rainbow", if (rainbow) 1 else 0)
                        .put("theme_weather_chip", if (weatherChip) 1 else 0)
                        .put("theme_theme_icon_enabled", if (themeIconEnabled) 1 else 0)
                        .put("theme_jelly_depth", jellyDepth.roundToInt())
                        .put("theme_jelly_shine", jellyShine.roundToInt())
                        .put("theme_jelly_border", jellyBorder.roundToInt())
                        .put("theme_jelly_saturation", jellySaturation.roundToInt())
                        .put("theme_time_mode", timeMode)
                        .put("theme_manual_weather", manualWeather)
                        .put("theme_card_opacity", cardOpacity.roundToInt())
                        .put("theme_header_opacity", headerOpacity.roundToInt())
                        .put("theme_nav_opacity", navOpacity.roundToInt())
                        .put("theme_input_opacity", inputOpacity.roundToInt())
                        .put("theme_blur", blur.roundToInt())
                        .put("theme_card_radius", cardRadius.roundToInt())
                        .put("theme_header_radius", headerRadius.roundToInt())
                        .put("theme_nav_radius", navRadius.roundToInt())
                        .put("theme_button_radius", buttonRadius.roundToInt())
                        .put("theme_input_radius", inputRadius.roundToInt())
                        .put("theme_shadow", shadow.roundToInt())
                        .put("theme_font_scale", fontScale.roundToInt())
                        .put("theme_page_width", pageWidth.roundToInt())
                        .put("theme_card_padding", cardPadding.roundToInt())
                        .put("theme_section_gap", sectionGap.roundToInt())
                        .put("theme_nav_height", navHeight.roundToInt())
                        .put("theme_button_height", buttonHeight.roundToInt())
                        .put("theme_profile_cover_height", profileCover.roundToInt())
                        .put("theme_profile_avatar_size", profileAvatar.roundToInt())
                        .put("theme_shop_cover_height", shopCover.roundToInt())
                        .put("theme_shop_avatar_size", shopAvatar.roundToInt())
                        .put("theme_header_color", headerColor)
                        .put("theme_header_text", headerText)
                        .put("theme_nav_color", navColor)
                        .put("theme_icon_color", iconColor)
                        .put("theme_active_color", activeColor)
                        .put("theme_text_color", textColor)
                        .put("theme_muted_color", mutedColor)
                        .put("theme_card_color", cardColor)
                        .put("theme_border_color", borderColor)
                        .put("theme_button_color", buttonColor)
                        .put("theme_input_color", inputColor)
                        .put("theme_accent", accent)
                        .put("theme_accent2", accent2)
                        .put("theme_bg_clear", bgClear.trim())
                        .put("theme_bg_clouds", bgClouds.trim())
                        .put("theme_bg_rain", bgRain.trim())
                        .put("theme_bg_fog", bgFog.trim())
                        .put("theme_bg_storm", bgStorm.trim())
                        .put("theme_bg_night", bgNight.trim())
                        .put("theme_home_order", homeOrder.trim())
                        .put("theme_header_items", headerItems.trim())
                        .put("theme_menu_items", menuItems.trim())
                )
            }
        }
    }
}

private fun trafficChangeText(value: Double): String = when {
    value > 0.0 -> "+${if (value % 1.0 == 0.0) value.toInt() else value}% vs yesterday"
    value < 0.0 -> "${if (value % 1.0 == 0.0) value.toInt() else value}% vs yesterday"
    else -> "No change"
}

@Composable
private fun TrafficLiveMeter(
    live: Int,
    members: Int,
    guests: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            drawArc(
                color = Color(0x2466738B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF5BD7FF), Color(0xFF9A7BFF), Color(0xFFF16CCB), Color(0xFF5BD7FF))
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(stroke)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LIVE NOW", color = JellyMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(live.toString(), color = JellyInk, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("$members members · $guests guests", color = JellyMuted, fontSize = 7.5f.sp)
        }
    }
}

@Composable
private fun TrafficTodayCard(label: String, value: String, change: String) {
    JellyGlass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 9.dp) {
        Column {
            Text(label, color = JellyMuted, fontSize = 8.5f.sp, fontWeight = FontWeight.Bold)
            Text(value, color = JellyInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(change, color = JellyMuted, fontSize = 7.5f.sp)
        }
    }
}

@Composable
private fun TrafficKpi(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    JellyGlass(modifier, radius = 17.dp, padding = 9.dp) {
        Column {
            Text(label.uppercase(), color = JellyMuted, fontSize = 7.2f.sp, fontWeight = FontWeight.Black)
            Text(value, color = JellyInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(note, color = JellyMuted, fontSize = 7.4f.sp, maxLines = 2)
        }
    }
}

private fun parseThemeColor(value: String): Color {
    val hex = value.trim().removePrefix("#")
    return runCatching {
        val rgb = hex.toLong(16)
        when (hex.length) {
            6 -> Color(0xFF000000L or rgb)
            8 -> Color(rgb)
            else -> Color.White
        }
    }.getOrDefault(Color.White)
}

@Composable
private fun ThemeColorField(label: String, value: String, onValue: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JellyGlass(
            Modifier.size(50.dp),
            radius = 15.dp,
            padding = 5.dp
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(parseThemeColor(value), RoundedCornerShape(11.dp))
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = {
                val cleaned = it.filter { ch -> ch == '#' || ch.isDigit() || ch.lowercaseChar() in 'a'..'f' }.take(7)
                onValue(cleaned)
            },
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            supportingText = { Text("#RRGGBB") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp)
        )
    }
}

@Composable
private fun ThemeOrderEditor(
    title: String,
    value: String,
    allowed: List<String>,
    labels: Map<String, String>,
    onValue: (String) -> Unit
) {
    val active = value.split(',').map { it.trim() }.filter { it in allowed }.distinct()
    val all = active + allowed.filter { it !in active }

    JellyGlass(Modifier.fillMaxWidth(), radius = 18.dp, padding = 9.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text("Show / hide and move", color = JellyMuted, fontSize = 8.sp)

            all.forEach { key ->
                val enabled = key in active
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked ->
                            val next = active.toMutableList()
                            if (checked && key !in next) next.add(key)
                            if (!checked) next.remove(key)
                            onValue(next.joinToString(","))
                        }
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        labels[key] ?: key,
                        Modifier.weight(1f),
                        color = JellyInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    if (enabled) {
                        val index = active.indexOf(key)
                        JellyButton("↑", enabled = index > 0) {
                            if (index > 0) {
                                val next = active.toMutableList()
                                val moved = next.removeAt(index)
                                next.add(index - 1, moved)
                                onValue(next.joinToString(","))
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        JellyButton("↓", enabled = index in 0 until active.lastIndex) {
                            if (index in 0 until active.lastIndex) {
                                val next = active.toMutableList()
                                val moved = next.removeAt(index)
                                next.add(index + 1, moved)
                                onValue(next.joinToString(","))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeTextField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
private fun ThemeSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ThemeSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column {
        Text("$label: ${value.roundToInt()}", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 10.5f.sp)
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}
