package com.mychhachh.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    section: String,
    query: String,
    loading: Boolean,
    error: String?,
    onSection: (String) -> Unit,
    onQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onAction: (String, Long, JSONObject) -> Unit,
    onProfile: (Long) -> Unit
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Admin Center", "Live platform controls and activity", JellyIcons.Shield) }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AdminStat("Users", stats.optInt("total_users"), Modifier.weight(1f))
                        AdminStat("Online", stats.optInt("online_users"), Modifier.weight(1f))
                        AdminStat("Shops", stats.optInt("total_shops"), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AdminStat("Posts", stats.optInt("total_posts"), Modifier.weight(1f))
                        AdminStat("Votes", stats.optInt("total_votes"), Modifier.weight(1f))
                        AdminStat("Alerts", stats.optInt("admin_alerts"), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Search this section…") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                    val sections = listOf(
                        "users" to "Users",
                        "verification" to "Verification",
                        "reports" to "Reports",
                        "posts" to "Posts",
                        "shops" to "Shops",
                        "votes" to "Voting",
                        "activity" to "Activity"
                    )
                    sections.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEach { (key, label) ->
                                JellyPill(label, section == key, Modifier.weight(1f)) { onSection(key) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    JellyButton("Refresh", Modifier.fillMaxWidth(), icon = JellyIcons.Search, onClick = onRefresh)
                }
            }
        }

        if (loading && rows.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onRefresh) } }

        when (section) {
            "users" -> items(rows, key = { "admin-user-${it.optLong("id")}" }) { o ->
                val u = runCatching { o.toUser() }.getOrNull()
                if (u != null) {
                    JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(u, 48.dp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    UserName(u, 13)
                                    Text("@${u.username}", color = JellyMuted, fontSize = 9.5f.sp)
                                    val contact = listOf(u.email, u.phone).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (contact.isNotBlank()) Text(contact, color = JellyMuted, fontSize = 8.5f.sp)
                                    Text(
                                        if (o.optBoolean("online", false)) "Online" else "Offline",
                                        color = if (o.optBoolean("online", false)) JellyGreen else JellyMuted,
                                        fontSize = 8.5f.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                JellyButton("Profile") { onProfile(u.id) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                JellyButton(
                                    if (u.verified) "Remove Tick" else "Blue Tick",
                                    Modifier.weight(1f),
                                    icon = JellyIcons.Check
                                ) { onAction("user_setting", u.id, JSONObject().put("key", "verified")) }
                                JellyButton(
                                    if (o.optBoolean("blocked", false)) "Unblock" else "Block",
                                    Modifier.weight(1f),
                                    icon = JellyIcons.Shield
                                ) { onAction("toggle_user", u.id, JSONObject()) }
                                JellyButton("Warning", Modifier.weight(1f), icon = JellyIcons.Bell) {
                                    warningTarget = u.id
                                    warningText = ""
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(
                                    "allow_comments" to "Comments",
                                    "allow_likes" to "Likes",
                                    "allow_messages" to "Messages",
                                    "allow_posts" to "Posts"
                                ).forEach { (key, label) ->
                                    JellyButton(label, Modifier.weight(1f)) {
                                        onAction("user_setting", u.id, JSONObject().put("key", key))
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                JellyButton("Photo", Modifier.weight(1f)) {
                                    onAction("user_setting", u.id, JSONObject().put("key", "allow_photo_upload"))
                                }
                                JellyButton("Video", Modifier.weight(1f)) {
                                    onAction("user_setting", u.id, JSONObject().put("key", "allow_video_upload"))
                                }
                                JellyButton("Follows", Modifier.weight(1f)) {
                                    onAction("user_setting", u.id, JSONObject().put("key", "allow_follows"))
                                }
                                JellyButton(
                                    if (o.optBoolean("promoted", false)) "Stop Promote" else "Promote",
                                    Modifier.weight(1f),
                                    icon = JellyIcons.Star
                                ) {
                                    onAction("promote_user", u.id, JSONObject())
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                JellyButton("Publish as User", Modifier.weight(1f), icon = JellyIcons.Edit) {
                                    userPostTarget = u.id
                                    userPostText = ""
                                }
                                JellyButton("Delete Account", Modifier.weight(1f), icon = JellyIcons.Delete) {
                                    deleteUserTarget = u.id
                                }
                            }
                        }
                    }
                }
            }

            "verification" -> items(rows, key = { "verify-${it.optLong("id")}" }) { o ->
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
                            JellyButton("Reject", Modifier.weight(1f), icon = JellyIcons.Close) {
                                verificationTarget = o.optLong("id")
                                verificationDecision = "rejected"
                                verificationNote = o.optString("admin_note", "")
                            }
                        }
                    }
                }
            }

            "reports" -> items(rows, key = { "report-${it.optLong("id")}" }) { o ->
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (reporterId > 0) {
                                JellyButton("Reporter", Modifier.weight(1f), icon = JellyIcons.User) { onProfile(reporterId) }
                            }
                            if (targetType == "profile" && targetId > 0) {
                                JellyButton("Reported Profile", Modifier.weight(1f), icon = JellyIcons.Shield) { onProfile(targetId) }
                            }
                            JellyButton("Reply", Modifier.weight(1f), icon = JellyIcons.Reply) {
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
                                JellyButton("Delete", icon = JellyIcons.Delete) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            JellyButton(if (o.optInt("active", 1) == 0) "Enable" else "Disable") {
                                onAction("toggle_shop", o.optLong("id"), JSONObject())
                            }
                            JellyButton("Promote", icon = JellyIcons.Star) {
                                onAction("promote_shop", o.optLong("id"), JSONObject())
                            }
                            JellyButton("Delete", icon = JellyIcons.Delete) {
                                onAction("delete_shop", o.optLong("id"), JSONObject())
                            }
                        }
                    }
                }
            }

            "votes" -> items(rows, key = { "admin-vote-${it.optLong("id")}" }) { o ->
                JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Voting #${o.optLong("id")} · ${o.optString("status", "")}", color = JellyInk, fontWeight = FontWeight.Black)
                        val left = o.optInt("left_votes", o.optInt("votes1", 0))
                        val right = o.optInt("right_votes", o.optInt("votes2", 0))
                        Text("Left: $left   ·   Right: $right", color = JellyMuted, fontSize = 10.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            JellyButton("L +1", Modifier.weight(1f)) {
                                onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "left").put("delta", 1))
                            }
                            JellyButton("L -1", Modifier.weight(1f)) {
                                onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "left").put("delta", -1))
                            }
                            JellyButton("R +1", Modifier.weight(1f)) {
                                onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "right").put("delta", 1))
                            }
                            JellyButton("R -1", Modifier.weight(1f)) {
                                onAction("vote_adjust", o.optLong("id"), JSONObject().put("side", "right").put("delta", -1))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            JellyButton("End", Modifier.weight(1f)) {
                                onAction("vote_end", o.optLong("id"), JSONObject())
                            }
                            JellyButton("Cancel", Modifier.weight(1f)) {
                                onAction("vote_cancel", o.optLong("id"), JSONObject())
                            }
                            JellyButton("Delete", Modifier.weight(1f), icon = JellyIcons.Delete) {
                                onAction("vote_delete", o.optLong("id"), JSONObject())
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
        }

        if (!loading && rows.isEmpty() && error == null) {
            item { EmptyCard("Nothing to show in this section.", JellyIcons.Shield) }
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
                    placeholder = { Text("Write admin reply…") },
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
                JellyButton("Delete", primary = true, icon = JellyIcons.Delete) {
                    onAction("delete_user", id, JSONObject())
                    deleteUserTarget = null
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteUserTarget = null } }
        )
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
    var timeMode by remember(stateKey) { mutableStateOf(settings.optString("theme_time_mode", "auto").ifBlank { "auto" }) }
    var manualWeather by remember(stateKey) { mutableStateOf(settings.optString("theme_manual_weather", "clear").ifBlank { "clear" }) }

    var cardOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_card_opacity", 78).toFloat()) }
    var headerOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_header_opacity", 78).toFloat()) }
    var navOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_nav_opacity", 78).toFloat()) }
    var inputOpacity by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_input_opacity", 90).toFloat()) }
    var blur by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_blur", 28).toFloat()) }
    var cardRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_card_radius", 30).toFloat()) }
    var headerRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_header_radius", 28).toFloat()) }
    var navRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_nav_radius", 25).toFloat()) }
    var buttonRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_button_radius", 24).toFloat()) }
    var inputRadius by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_input_radius", 17).toFloat()) }
    var shadow by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_shadow", 4).toFloat()) }
    var fontScale by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_font_scale", 100).toFloat()) }
    var pageWidth by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_page_width", 980).toFloat()) }
    var cardPadding by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_card_padding", 13).toFloat()) }
    var sectionGap by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_section_gap", 9).toFloat()) }
    var navHeight by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_nav_height", 64).toFloat()) }
    var buttonHeight by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_button_height", 44).toFloat()) }
    var profileCover by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_profile_cover_height", 165).toFloat()) }
    var profileAvatar by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_profile_avatar_size", 96).toFloat()) }
    var shopCover by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_shop_cover_height", 185).toFloat()) }
    var shopAvatar by remember(stateKey) { mutableFloatStateOf(settings.optInt("theme_shop_avatar_size", 96).toFloat()) }

    var headerColor by remember(stateKey) { mutableStateOf(settings.optString("theme_header_color", "#ffffff")) }
    var headerText by remember(stateKey) { mutableStateOf(settings.optString("theme_header_text", "#0d1a34")) }
    var navColor by remember(stateKey) { mutableStateOf(settings.optString("theme_nav_color", "#ffffff")) }
    var iconColor by remember(stateKey) { mutableStateOf(settings.optString("theme_icon_color", "#302467")) }
    var activeColor by remember(stateKey) { mutableStateOf(settings.optString("theme_active_color", "#ff4faf")) }
    var textColor by remember(stateKey) { mutableStateOf(settings.optString("theme_text_color", "#302467")) }
    var mutedColor by remember(stateKey) { mutableStateOf(settings.optString("theme_muted_color", "#727b9e")) }
    var cardColor by remember(stateKey) { mutableStateOf(settings.optString("theme_card_color", "#fdfeff")) }
    var borderColor by remember(stateKey) { mutableStateOf(settings.optString("theme_border_color", "#dce6f6")) }
    var buttonColor by remember(stateKey) { mutableStateOf(settings.optString("theme_button_color", "#ffffff")) }
    var inputColor by remember(stateKey) { mutableStateOf(settings.optString("theme_input_color", "#ffffff")) }
    var accent by remember(stateKey) { mutableStateOf(settings.optString("theme_accent", "#ff4faf")) }
    var accent2 by remember(stateKey) { mutableStateOf(settings.optString("theme_accent2", "#9070f8")) }

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
        item { PageTitle("Theme", "Live website and native app appearance controls", JellyIcons.Palette) }

        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 13.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Branding")
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
                    SectionTitle("Theme Engine")
                    ThemeSwitch("Enable theme", themeEnabled) { themeEnabled = it }
                    ThemeSwitch("Jelly theme", jelly) { jelly = it }
                    ThemeSwitch("Live Chhachh weather", weatherLive) { weatherLive = it }
                    ThemeSwitch("Natural motion", motion) { motion = it }
                    ThemeSwitch("Rainbow My Chhachh", rainbow) { rainbow = it }
                    ThemeSwitch("Weather chip", weatherChip) { weatherChip = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JellyButton("Time: ${timeMode.uppercase()}", Modifier.weight(1f)) {
                            timeMode = when (timeMode) { "auto" -> "day"; "day" -> "night"; else -> "auto" }
                        }
                        JellyButton("Weather: ${manualWeather.replaceFirstChar { it.uppercase() }}", Modifier.weight(1f)) {
                            manualWeather = when (manualWeather) {
                                "clear" -> "clouds"
                                "clouds" -> "rain"
                                "rain" -> "fog"
                                "fog" -> "storm"
                                else -> "clear"
                            }
                        }
                    }
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
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SectionTitle("Order & Visibility")
                    OutlinedTextField(homeOrder, { homeOrder = it }, Modifier.fillMaxWidth(), label = { Text("Home order") }, supportingText = { Text("notice,tabs,composer") }, shape = RoundedCornerShape(17.dp))
                    OutlinedTextField(headerItems, { headerItems = it }, Modifier.fillMaxWidth(), label = { Text("Native header items order") }, supportingText = { Text("home,people,shop,map,messages") }, shape = RoundedCornerShape(17.dp))
                    OutlinedTextField(menuItems, { menuItems = it }, Modifier.fillMaxWidth(), label = { Text("Native menu items order") }, supportingText = { Text("votes,saved,settings,theme,admin,logout") }, shape = RoundedCornerShape(17.dp))
                }
            }
        }

        error?.let { item { ErrorCard(it) } }

        item {
            JellyButton("Save Theme", Modifier.fillMaxWidth(), primary = true, icon = JellyIcons.Palette, enabled = !busy) {
                onSaveTheme(
                    JSONObject()
                        .put("theme_enabled", if (themeEnabled) 1 else 0)
                        .put("theme_jelly_enabled", if (jelly) 1 else 0)
                        .put("theme_weather_live", if (weatherLive) 1 else 0)
                        .put("theme_motion", if (motion) 1 else 0)
                        .put("theme_brand_rainbow", if (rainbow) 1 else 0)
                        .put("theme_weather_chip", if (weatherChip) 1 else 0)
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

@Composable
private fun ThemeColorField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.take(7)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("$label #RRGGBB") },
        singleLine = true,
        shape = RoundedCornerShape(17.dp)
    )
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
