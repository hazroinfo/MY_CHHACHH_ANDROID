package com.mychhachh.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun V95AdminCenter(c: V95Controller) {
    if (c.user?.isAdmin != true) {
        V95Empty(c.t("Administrator access required", "ایڈمن رسائی درکار ہے"))
        return
    }

    LaunchedEffect(Unit) {
        if (c.adminState == null) c.loadAdmin()
    }

    val section = c.adminSection
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            V95PageHeading(
                c.t("Admin Center", "ایڈمن سینٹر"),
                c.t("Live site management", "لائیو سائٹ مینجمنٹ")
            )
        }

        item {
            V95GlassCard(radius = 22.dp, padding = 10.dp) {
                val sections = listOf(
                    "overview" to c.t("Overview", "خلاصہ"),
                    "users" to c.t("Users", "یوزرز"),
                    "verification" to c.t("Verification", "ویریفکیشن"),
                    "posts" to c.t("Posts", "پوسٹس"),
                    "shops" to c.t("Shops", "دکانیں"),
                    "votes" to c.t("Votes", "ووٹنگ"),
                    "reports" to c.t("Reports", "رپورٹس"),
                    "activity" to c.t("Logs", "لاگز"),
                    "announcements" to c.t("Announcements", "اعلانات")
                )
                sections.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { (key, label) ->
                            V95Button(label, Modifier.weight(1f), primary = section == key) {
                                if (key == "overview") c.loadAdmin()
                                else if (key == "announcements") {
                                    c.adminSection = key
                                    c.adminSectionData = c.adminState
                                } else c.openAdminSection(key)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        when (section) {
            "overview" -> {
                item { V95AdminOverview(c) }
                item { V95AdminFeatureToggles(c) }
            }
            "announcements" -> {
                val notices = c.adminState?.optJSONArray("notices") ?: JSONArray()
                if (notices.length() == 0) item { V95Empty(c.t("No admin announcements", "کوئی ایڈمن اعلان نہیں")) }
                items((0 until notices.length()).toList()) { i ->
                    V95AdminNoticeItem(c, notices.optJSONObject(i) ?: JSONObject())
                }
            }
            else -> {
                val data = c.adminSectionData
                val arr = data?.optJSONArray("items") ?: JSONArray()
                if (arr.length() == 0 && !c.busy) item { V95Empty(c.t("No records", "کوئی ریکارڈ نہیں")) }
                items((0 until arr.length()).toList()) { i ->
                    val item = arr.optJSONObject(i) ?: JSONObject()
                    V95AdminRecord(c, section, item)
                }
            }
        }
    }
}

@Composable
private fun V95AdminOverview(c: V95Controller) {
    val stats = c.adminState?.optJSONObject("stats") ?: JSONObject()
    val rows = listOf(
        c.t("Users", "یوزرز") to stats.optInt("total_users", 0),
        c.t("Online", "آن لائن") to stats.optInt("online_users", 0),
        c.t("Shops", "دکانیں") to stats.optInt("total_shops", 0),
        c.t("Posts", "پوسٹس") to stats.optInt("total_posts", 0),
        c.t("Votes", "ووٹنگ") to stats.optInt("total_votes", 0),
        c.t("Pending verification", "زیرِ التوا ویریفکیشن") to stats.optInt("pending_verifications", 0),
        c.t("Open reports", "اوپن رپورٹس") to stats.optInt("open_reports", 0),
        c.t("Alerts", "الرٹس") to stats.optInt("admin_alerts", 0)
    )
    V95GlassCard {
        Text(c.t("Overview", "خلاصہ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
        rows.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { (label, value) -> V95Metric(label, value.toString(), Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V95AdminFeatureToggles(c: V95Controller) {
    val settings = c.adminState?.optJSONObject("settings") ?: c.features
    val keys = listOf(
        "likes" to c.t("Likes", "لائکس"),
        "comments" to c.t("Comments", "کمنٹس"),
        "shares" to c.t("Shares", "شیئر"),
        "messaging" to c.t("Messages", "پیغامات"),
        "videos" to c.t("Videos", "ویڈیوز"),
        "voting" to c.t("Voting", "ووٹنگ"),
        "shops" to c.t("Shops", "دکانیں"),
        "follows" to c.t("Follows", "فالو"),
        "registration_enabled" to c.t("Registration", "رجسٹریشن"),
        "password_login_enabled" to c.t("Password login", "پاس ورڈ لاگ اِن")
    )

    V95GlassCard {
        Text(c.t("Feature controls", "فیچر کنٹرول"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
        keys.forEach { (key, label) ->
            val on = settings.optInt(key, c.features.optInt(key, 1)) != 0
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = .32f)).padding(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, Modifier.weight(1f), color = V95Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                V95Button(if (on) "ON" else "OFF", primary = on) {
                    c.runAdminAction(
                        "feature",
                        fields = JSONObject().put("key", key).put("value", if (on) 0 else 1)
                    )
                }
            }
        }
    }
}

@Composable
private fun V95AdminRecord(c: V95Controller, section: String, item: JSONObject) {
    val id = item.optLong("id")
    V95GlassCard(radius = 20.dp, padding = 10.dp) {
        when (section) {
            "users" -> V95AdminUser(c, item, id)
            "verification" -> V95AdminVerification(c, item, id)
            "posts" -> V95AdminPost(c, item, id)
            "shops" -> V95AdminShop(c, item, id)
            "votes" -> V95AdminVote(c, item, id)
            "reports" -> V95AdminReport(c, item, id)
            "activity" -> V95AdminActivity(c, item)
            else -> V95AdminGeneric(item)
        }
    }
}

@Composable
private fun V95AdminUser(c: V95Controller, item: JSONObject, id: Long) {
    Text(item.optString("name", "User"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
    Text("@${item.optString("username", "")} · ${item.optString("email", item.optString("phone", ""))}", color = V95Muted, fontSize = 10.sp)
    Text(if (item.optBoolean("online", false)) c.t("Online", "آن لائن") else c.t("Offline", "آف لائن"), color = if (item.optBoolean("online", false)) V95Green else V95Muted, fontSize = 10.sp)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        V95Button(c.t("Block", "بلاک"), Modifier.weight(1f), danger = true) { c.runAdminAction("toggle_user", id) }
        V95Button(c.t("Blue tick", "بلیو ٹک"), Modifier.weight(1f)) { c.runAdminAction("user_setting", id, JSONObject().put("key", "verified")) }
        V95Button(c.t("Promote", "پروموٹ"), Modifier.weight(1f)) { c.runAdminAction("promote_user", id) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        V95Button(c.t("Posts", "پوسٹس"), Modifier.weight(1f)) { c.runAdminAction("user_setting", id, JSONObject().put("key", "allow_posts")) }
        V95Button(c.t("Photo", "فوٹو"), Modifier.weight(1f)) { c.runAdminAction("user_setting", id, JSONObject().put("key", "allow_photo_upload")) }
        V95Button(c.t("Video", "ویڈیو"), Modifier.weight(1f)) { c.runAdminAction("user_setting", id, JSONObject().put("key", "allow_video_upload")) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        V95Button(c.t("Comments", "کمنٹس"), Modifier.weight(1f)) { c.runAdminAction("user_setting", id, JSONObject().put("key", "allow_comments")) }
        V95Button(c.t("Messages", "پیغامات"), Modifier.weight(1f)) { c.runAdminAction("user_setting", id, JSONObject().put("key", "allow_messages")) }
        V95Button(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) { c.runAdminAction("delete_user", id) }
    }
}

@Composable
private fun V95AdminVerification(c: V95Controller, item: JSONObject, id: Long) {
    val user = item.optJSONObject("user")
    Text(user?.optString("name", "User") ?: "User", color = V95Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
    Text(item.optString("status", "pending"), color = V95Muted, fontSize = 10.sp)
    Text(item.optString("phone", ""), color = V95Muted, fontSize = 10.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        V95Button(c.t("Approve", "منظور"), Modifier.weight(1f), primary = true) {
            c.runAdminAction("verification_review", id, JSONObject().put("decision", "approved").put("admin_note", "Approved in Android Admin Center"))
        }
        V95Button(c.t("Reject", "مسترد"), Modifier.weight(1f), danger = true) {
            c.runAdminAction("verification_review", id, JSONObject().put("decision", "rejected").put("admin_note", "Rejected in Android Admin Center"))
        }
    }
}

@Composable
private fun V95AdminPost(c: V95Controller, item: JSONObject, id: Long) {
    val user = item.optJSONObject("user")
    Text(user?.optString("name", c.t("Post", "پوسٹ")) ?: c.t("Post", "پوسٹ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
    Text(item.optString("text", "").take(320), color = V95Ink, fontSize = 11.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        V95Button(c.t("Promote", "پروموٹ"), Modifier.weight(1f)) { c.runAdminAction("promote_post", id) }
        V95Button(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) { c.runAdminAction("delete_post", id) }
    }
}

@Composable
private fun V95AdminShop(c: V95Controller, item: JSONObject, id: Long) {
    Text(item.optString("name", c.t("Shop", "دکان")), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
    Text(item.optString("category", ""), color = V95Muted, fontSize = 10.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        V95Button(c.t("Promote", "پروموٹ"), Modifier.weight(1f)) { c.runAdminAction("promote_shop", id) }
        V95Button(c.t("On/Off", "آن/آف"), Modifier.weight(1f)) { c.runAdminAction("toggle_shop", id) }
        V95Button(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) { c.runAdminAction("delete_shop", id) }
    }
}

@Composable
private fun V95AdminVote(c: V95Controller, item: JSONObject, id: Long) {
    Text(item.optString("title", c.t("Voting", "ووٹنگ")), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
    Text(item.optString("status", ""), color = V95Muted, fontSize = 10.sp)
    val left = item.optInt("left_votes", item.optInt("votes1", 0))
    val right = item.optInt("right_votes", item.optInt("votes2", 0))
    Text("${c.t("Left", "بائیں")}: $left   ·   ${c.t("Right", "دائیں")}: $right", color = V95Purple, fontWeight = FontWeight.Black, fontSize = 12.sp)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        V95Button("L +1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "left").put("delta", 1)) }
        V95Button("L -1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "left").put("delta", -1)) }
        V95Button("R +1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "right").put("delta", 1)) }
        V95Button("R -1", Modifier.weight(1f)) { c.runAdminAction("vote_adjust", id, JSONObject().put("side", "right").put("delta", -1)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        V95Button(c.t("End", "ختم"), Modifier.weight(1f), primary = true) { c.runAdminAction("vote_end", id) }
        V95Button(c.t("Cancel", "منسوخ"), Modifier.weight(1f)) { c.runAdminAction("vote_cancel", id) }
        V95Button(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) { c.runAdminAction("vote_delete", id) }
    }
}

@Composable
private fun V95AdminReport(c: V95Controller, item: JSONObject, id: Long) {
    var reply by remember(id) { mutableStateOf(item.optString("admin_reply", "")) }
    val reporter = item.optJSONObject("reporter")
    Text(reporter?.optString("name", c.t("Report", "رپورٹ")) ?: c.t("Report", "رپورٹ"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
    Text(item.optString("reason", item.optString("message", "")).take(420), color = V95Ink, fontSize = 11.sp)
    Text(item.optString("status", "open"), color = V95Muted, fontSize = 10.sp)
    V95Field(reply, c.t("Reply to user", "یوزر کو جواب")) { reply = it }
    V95Button(c.t("Send reply", "جواب بھیجیں"), Modifier.fillMaxWidth(), primary = true, enabled = reply.isNotBlank()) {
        c.runAdminAction("report_reply", id, JSONObject().put("reply", reply))
    }
}

@Composable
private fun V95AdminActivity(c: V95Controller, item: JSONObject) {
    Text(item.optString("action", "activity"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
    Text("ID ${item.optLong("actor_id")} · ${item.optString("created_at", "")}", color = V95Muted, fontSize = 9.sp)
    if (item.optString("ip", "").isNotBlank()) Text(item.optString("ip"), color = V95Muted, fontSize = 9.sp)
    val snapshot = item.optJSONObject("actor_snapshot")
    if (snapshot != null) Text(snapshot.optString("name", "") + " @" + snapshot.optString("username", ""), color = V95Ink, fontSize = 10.sp)
}

@Composable
private fun V95AdminNoticeItem(c: V95Controller, item: JSONObject) {
    val id = item.optLong("id")
    Text(item.optString("notice_type", "announcement").uppercase(), color = V95Purple, fontWeight = FontWeight.Black, fontSize = 10.sp)
    Text(item.optString("text", "").take(360), color = V95Ink, fontSize = 11.sp)
    Text(item.optString("created_at", ""), color = V95Muted, fontSize = 9.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        V95Button(if (item.optInt("active", 1) == 0) c.t("Show", "دکھائیں") else c.t("Hide", "چھپائیں"), Modifier.weight(1f)) {
            c.runAdminAction("toggle_admin_notice", id)
        }
        V95Button(c.t("Delete", "حذف"), Modifier.weight(1f), danger = true) {
            c.runAdminAction("delete_admin_notice", id)
        }
    }
}

@Composable
private fun V95AdminGeneric(item: JSONObject) {
    val keys = item.keys()
    var shown = 0
    while (keys.hasNext() && shown < 8) {
        val key = keys.next()
        val value = item.opt(key)
        if (value !is JSONObject && value !is JSONArray) {
            Text("${key.replace('_', ' ')}: ${value?.toString() ?: "null"}", color = V95Ink, fontSize = 10.sp)
            shown++
        }
    }
}
