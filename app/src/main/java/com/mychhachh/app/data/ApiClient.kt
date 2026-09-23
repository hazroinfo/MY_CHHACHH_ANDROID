package com.mychhachh.app.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val status: Int = 0, val payload: JSONObject? = null) : IOException(message)

class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("my_chhachh_cookies_v3", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val edit = prefs.edit()
        for (c in cookies) {
            val o = JSONObject()
                .put("name", c.name).put("value", c.value).put("expiresAt", c.expiresAt)
                .put("domain", c.domain).put("path", c.path).put("secure", c.secure)
                .put("httpOnly", c.httpOnly).put("hostOnly", c.hostOnly)
            edit.putString("${c.domain}|${c.path}|${c.name}", o.toString())
        }
        edit.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val expired = mutableListOf<String>()
        val out = mutableListOf<Cookie>()
        prefs.all.forEach { (key, raw) ->
            val o = runCatching { JSONObject(raw as String) }.getOrNull() ?: return@forEach
            val exp = o.optLong("expiresAt", 0L)
            if (exp <= now) { expired += key; return@forEach }
            val domain = o.optString("domain")
            val b = Cookie.Builder().name(o.optString("name")).value(o.optString("value")).path(o.optString("path", "/")).expiresAt(exp)
            if (o.optBoolean("hostOnly", true)) b.hostOnlyDomain(domain) else b.domain(domain)
            if (o.optBoolean("secure", false)) b.secure()
            if (o.optBoolean("httpOnly", false)) b.httpOnly()
            runCatching { b.build() }.getOrNull()?.takeIf { it.matches(url) }?.let(out::add)
        }
        if (expired.isNotEmpty()) prefs.edit().also { e -> expired.forEach(e::remove) }.apply()
        return out
    }

    fun clear() = prefs.edit().clear().apply()
}

class ApiClient(private val context: Context) {
    companion object { const val BASE = "https://chhachh.pages.dev" }
    private val cookies = PersistentCookieJar(context.applicationContext)
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val url = if (path.startsWith("http")) path else BASE + path
        val req = Request.Builder().url(url).header("Accept", "application/json")
        val rb = (body ?: JSONObject()).toString().toRequestBody(jsonType)
        when (method) {
            "GET" -> req.get()
            "POST" -> req.post(rb)
            "PATCH" -> req.patch(rb)
            "DELETE" -> req.delete(rb)
        }
        client.newCall(req.build()).execute().use { res ->
            val text = res.body?.string().orEmpty()
            val data = if (text.isBlank()) JSONObject().put("ok", res.isSuccessful) else runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", text.take(500)) }
            if (!res.isSuccessful) throw ApiException(data.optString("error", "Request failed (${res.code})"), res.code, data)
            return data
        }
    }

    fun get(path: String) = request("GET", path)
    fun post(path: String, body: JSONObject = JSONObject()) = request("POST", path, body)
    fun patch(path: String, body: JSONObject = JSONObject()) = request("PATCH", path, body)
    fun delete(path: String, body: JSONObject = JSONObject()) = request("DELETE", path, body)
    fun clearSession() = cookies.clear()

    fun bootstrap(): Bootstrap {
        val d = get("/api/bootstrap")
        return Bootstrap(
            user = d.optJSONObject("user")?.toUser(),
            unread = d.optInt("unread", 0), announcementUnread = d.optInt("announcement_unread", 0),
            features = d.optJSONObject("features") ?: JSONObject()
        )
    }

    fun login(identity: String, password: String): User {
        val d = post("/api/login", JSONObject().put("identity", identity).put("password", password))
        return d.optJSONObject("user")?.toUser() ?: throw ApiException("Login response did not contain the user.")
    }

    fun register(name: String, username: String, email: String, password: String): JSONObject = post(
        "/api/register", JSONObject().put("name", name).put("username", username).put("email", email).put("password", password).put("terms_accepted", true)
    )

    fun verifyEmail(userId: Long, code: String): User {
        val d = post("/api/verify-email", JSONObject().put("user_id", userId).put("code", code))
        return d.optJSONObject("user")?.toUser() ?: throw ApiException("Verification response did not contain the user.")
    }

    fun resendCode(userId: Long) { post("/api/resend-code", JSONObject().put("user_id", userId)) }

    fun forgotSend(email: String): String {
        val d = post("/api/forgot/send", JSONObject().put("email", email.trim().lowercase()))
        return d.optString("reset_key").takeIf { it.isNotBlank() } ?: throw ApiException("Reset response did not contain a reset key.")
    }

    fun forgotReset(resetKey: String, code: String, password: String) {
        post("/api/forgot/reset", JSONObject().put("reset_key", resetKey).put("code", code.trim()).put("password", password))
    }

    fun logout() { runCatching { post("/api/logout") }; clearSession() }

    fun feed(mode: String, before: Long = 0L, limit: Int = 18): Pair<List<Post>, Long> {
        val suffix = if (before > 0) "&before=$before" else ""
        val d = get("/api/feed?mode=${mode}&limit=$limit$suffix")
        return (d.optJSONArray("items") ?: JSONArray()).posts() to d.optLong("next_before", 0)
    }

    fun users(q: String = ""): Pair<List<User>, Long> {
        val d = get("/api/users?limit=30&q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        return (d.optJSONArray("items") ?: JSONArray()).users() to d.optLong("next_before", 0)
    }

    fun user(id: Long): JSONObject = get("/api/users/$id")
    fun followUser(id: Long): JSONObject = post("/api/users/$id/follow")
    fun relationUsers(id: Long, mode: String): List<User> =
        (get("/api/users/$id/${if (mode == "following") "following" else "followers"}").optJSONArray("items") ?: JSONArray()).users()
    fun reportProfile(id: Long, reason: String): JSONObject =
        post("/api/report", JSONObject().put("target_type", "profile").put("target_id", id).put("reason", reason))

    fun shops(q: String = ""): List<Shop> {
        val d = get("/api/shops?q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        return (d.optJSONArray("items") ?: JSONArray()).shops()
    }
    fun shop(id: Long): JSONObject = get("/api/shops/$id")
    fun toggleShopFollow(id: Long): JSONObject = post("/api/shops/$id/follow")
    fun createShop(fields: JSONObject): JSONObject = post("/api/shops", fields)
    fun updateShop(id: Long, fields: JSONObject): JSONObject = patch("/api/shops/$id", fields)
    fun deleteShop(id: Long): JSONObject = delete("/api/shops/$id")
    fun createShopPost(id: Long, text: String, privacy: String, photo: String = "", video: String = ""): JSONObject =
        post("/api/shops/$id/posts", JSONObject().put("text", text).put("privacy", privacy).put("photo", photo).put("video", video))

    fun conversations(): List<Conversation> {
        val d = get("/api/messages")
        val a = d.optJSONArray("conversations") ?: JSONArray()
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o ->
            val u = o.optJSONObject("user")?.toUser() ?: return@let null
            val last = o.optJSONObject("last") ?: JSONObject()
            Conversation(u, last.optString("text", if (last.optString("photo").isNotBlank()) "Photo" else ""), last.optString("created_at", ""), !last.optBoolean("read", true) && last.optLong("target_id") > 0)
        }}
    }

    fun messageGroups(): List<MessageGroup> {
        val d = get("/api/message-groups")
        val a = d.optJSONArray("items") ?: JSONArray()
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val last = o.optJSONObject("last") ?: JSONObject()
            val members = o.optJSONArray("members")
            MessageGroup(
                id = o.optLong("id"),
                name = o.optString("name", "Group"),
                memberCount = members?.length() ?: o.optInt("member_count", 0),
                preview = last.optString("text", last.optString("message", "Group conversation")),
                createdAt = last.optString("created_at", o.optString("created_at", ""))
            )
        }
    }

    fun createMessageGroup(name: String, usernames: String): Long {
        val d = post("/api/message-groups", JSONObject().put("name", name).put("usernames", usernames))
        return d.optJSONObject("group")?.optLong("id")
            ?: d.optLong("group_id", 0L)
            ?: 0L
    }

    fun groupChat(groupId: Long): Pair<String, List<Message>> {
        val d = get("/api/group-messages?group=$groupId")
        val groupName = d.optJSONObject("group")?.optString("name", "Group") ?: "Group"
        val a = d.optJSONArray("items") ?: JSONArray()
        val items = (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            Message(
                id = o.optLong("id"),
                senderId = o.optLong("user_id", o.optLong("sender_id")),
                receiverId = 0L,
                text = o.optString("text", o.optString("message", "")),
                photo = mediaUrl(o.optString("photo", "")),
                audio = mediaUrl(o.optString("audio", "")),
                createdAt = o.optString("created_at", ""),
                locationLat = o.optString("location_lat", "").toDoubleOrNull(),
                locationLng = o.optString("location_lng", "").toDoubleOrNull()
            )
        }
        return groupName to items
    }

    fun sendGroupMessage(groupId: Long, text: String, photo: String = "", audio: String = "", locationLat: Double? = null, locationLng: Double? = null): Message {
        val body = JSONObject().put("group_id", groupId).put("message", text).put("text", text).put("photo", photo).put("audio", audio)
        if (locationLat != null && locationLng != null) {
            body.put("location_lat", locationLat).put("location_lng", locationLng)
        }
        val d = post("/api/group-messages", body)
        val o = d.optJSONObject("message") ?: d.optJSONObject("item") ?: JSONObject()
        return Message(
            id = o.optLong("id"),
            senderId = o.optLong("user_id", o.optLong("sender_id")),
            receiverId = 0L,
            text = o.optString("text", o.optString("message", text)),
            photo = mediaUrl(o.optString("photo", photo)),
            audio = mediaUrl(o.optString("audio", audio)),
            createdAt = o.optString("created_at", ""),
            locationLat = o.optString("location_lat", "").toDoubleOrNull() ?: locationLat,
            locationLng = o.optString("location_lng", "").toDoubleOrNull() ?: locationLng
        )
    }

    fun chat(withId: Long): Pair<User?, List<Message>> {
        val d = get("/api/messages?with=$withId")
        val u = d.optJSONObject("with")?.toUser()
        val a = d.optJSONArray("items") ?: JSONArray()
        val items = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o -> Message(
            id=o.optLong("id"), senderId=o.optLong("user_id", o.optLong("sender_id")), receiverId=o.optLong("target_id", o.optLong("receiver_id")),
            text=o.optString("text", o.optString("message", "")),
            photo=mediaUrl(o.optString("photo", "")),
            audio=mediaUrl(o.optString("audio", "")),
            createdAt=o.optString("created_at", ""),
            locationLat=o.optString("location_lat", "").toDoubleOrNull(),
            locationLng=o.optString("location_lng", "").toDoubleOrNull()
        )}}
        return u to items
    }

    fun sendMessage(to: Long, text: String, photo: String = "", audio: String = "", locationLat: Double? = null, locationLng: Double? = null): Message {
        val body = JSONObject().put("receiver_id", to).put("message", text).put("text", text).put("photo", photo).put("audio", audio)
        if (locationLat != null && locationLng != null) {
            body.put("location_lat", locationLat).put("location_lng", locationLng)
        }
        val d = post("/api/messages", body)
        val o = d.optJSONObject("message") ?: d.optJSONObject("item") ?: JSONObject()
        return Message(
            id=o.optLong("id"),
            senderId=o.optLong("user_id", o.optLong("sender_id")),
            receiverId=o.optLong("target_id", o.optLong("receiver_id", to)),
            text=o.optString("text", o.optString("message", text)),
            photo=mediaUrl(o.optString("photo", photo)),
            audio=mediaUrl(o.optString("audio", audio)),
            createdAt=o.optString("created_at", ""),
            locationLat=o.optString("location_lat", "").toDoubleOrNull() ?: locationLat,
            locationLng=o.optString("location_lng", "").toDoubleOrNull() ?: locationLng
        )
    }

    fun notifications(): Pair<List<Notice>, Int> {
        val d = get("/api/notifications")
        val a = d.optJSONArray("items") ?: JSONArray()
        val items = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o -> Notice(o.optLong("id"), o.optJSONObject("actor")?.toUser(), o.optString("text", o.optString("message", "Notification")), o.optString("type", "info"), o.optString("created_at", ""), o.optBoolean("read", false)) } }
        return items to d.optInt("unread", 0)
    }
    fun markNotificationsRead() { post("/api/notifications/read") }

    fun announcements(): Pair<List<Announcement>, Int> {
        val d = get("/api/announcements?limit=80")
        val a = d.optJSONArray("items") ?: JSONArray()
        val items = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o -> Announcement(
            o.optLong("id"), (o.optJSONObject("author") ?: o.optJSONObject("user"))?.toUser(), o.optString("text", ""), mediaUrl(o.optString("photo", "")), mediaUrl(o.optString("audio", "")), o.optString("notice_type", "announcement"), o.optInt("likes", 0), o.optInt("comments", 0), o.optBoolean("liked", false), o.optString("created_at", "")
        )}}
        return items to d.optInt("unread", 0)
    }
    fun markAnnouncementsRead() { post("/api/announcements/read") }
    fun toggleAnnouncementLike(id: Long): JSONObject = post("/api/announcements/$id/like")
    fun announcementComments(id: Long): List<Comment> =
        (get("/api/announcements/$id/comments").optJSONArray("items") ?: JSONArray()).comments()
    fun addAnnouncementComment(id: Long, text: String): JSONObject =
        post("/api/announcements/$id/comments", JSONObject().put("text", text))
    fun createAnnouncement(text: String, photo: String = "", audio: String = "", noticeType: String = "announcement"): JSONObject =
        post("/api/announcements", JSONObject().put("text", text).put("photo", photo).put("audio", audio).put("notice_type", noticeType))

    fun votes(): List<Vote> {
        val d = get("/api/votes?limit=40")
        val a = d.optJSONArray("items") ?: JSONArray()
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o ->
            val u1 = (o.optJSONObject("left") ?: o.optJSONObject("user1") ?: o.optJSONObject("challenger") ?: o.optJSONObject("left_user"))?.toUser()
            val u2 = (o.optJSONObject("right") ?: o.optJSONObject("user2") ?: o.optJSONObject("opponent") ?: o.optJSONObject("right_user"))?.toUser()
            Vote(
                id = o.optLong("id"),
                title = o.optString("title", o.optString("question", "Voting")),
                status = o.optString("status", "active"),
                user1 = u1,
                user2 = u2,
                votes1 = if (o.isNull("left_votes")) 0 else o.optInt("left_votes", o.optInt("votes1", 0)),
                votes2 = if (o.isNull("right_votes")) 0 else o.optInt("right_votes", o.optInt("votes2", 0)),
                createdAt = o.optString("created_at", ""),
                leftUserId = o.optLong("left_user_id", u1?.id ?: 0L),
                rightUserId = o.optLong("right_user_id", u2?.id ?: 0L),
                leftText = o.optString("left_text", ""),
                rightText = o.optString("right_text", ""),
                myChoice = o.optLong("my_choice", 0L),
                winnerUserId = o.optLong("winner_user_id", 0L),
                resultRevealed = o.optBoolean("result_revealed", o.optString("status") != "active"),
                durationHours = o.optInt("duration_hours", 24),
                startsAt = o.optString("starts_at", ""),
                endsAt = o.optString("ends_at", "")
            )
        }}
    }

    fun createVote(opponentUsername: String, durationHours: Int, leftText: String): JSONObject =
        post("/api/votes", JSONObject().put("opponent_username", opponentUsername.removePrefix("@")).put("duration_hours", durationHours).put("left_text", leftText))
    fun castVote(id: Long, choiceUserId: Long): JSONObject =
        post("/api/votes/$id/cast", JSONObject().put("choice_user_id", choiceUserId))
    fun respondVote(id: Long, decision: String): JSONObject =
        post("/api/votes/$id/respond", JSONObject().put("decision", decision))
    fun startVote(id: Long): JSONObject = post("/api/votes/$id/start")
    fun cancelVote(id: Long): JSONObject = post("/api/votes/$id/cancel")
    fun leaveVote(id: Long): JSONObject = post("/api/votes/$id/leave")
    fun shareVote(id: Long, privacy: String = "followers"): JSONObject =
        post("/api/votes/$id/share", JSONObject().put("privacy", privacy))
    fun updateVoteStatement(id: Long, text: String): JSONObject =
        post("/api/votes/$id/statement", JSONObject().put("text", text))
    fun voteComments(id: Long): JSONArray = get("/api/votes/$id/comments").optJSONArray("items") ?: JSONArray()
    fun addVoteComment(id: Long, text: String): JSONObject =
        post("/api/votes/$id/comments", JSONObject().put("text", text))

    fun saved(): List<Post> = (get("/api/saved").optJSONArray("items") ?: JSONArray()).posts()

    fun search(q: String): SearchBundle {
        val d = get("/api/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        return SearchBundle((d.optJSONArray("users") ?: JSONArray()).users(), (d.optJSONArray("shops") ?: JSONArray()).shops(), (d.optJSONArray("posts") ?: JSONArray()).posts())
    }

    fun likePost(id: Long): JSONObject = post("/api/posts/$id/like")
    fun likePost(postItem: Post): JSONObject =
        if (postItem.shopId > 0) post("/api/shop-posts/${postItem.id}/like") else likePost(postItem.id)

    fun sharePost(id: Long): JSONObject = post("/api/posts/$id/share")
    fun sharePost(postItem: Post): JSONObject =
        if (postItem.shopId > 0) post("/api/shop-posts/${postItem.id}/share") else sharePost(postItem.id)

    fun savePost(id: Long): JSONObject = post("/api/posts/$id/save")
    fun updatePost(id: Long, text: String, privacy: String): JSONObject =
        patch("/api/posts/$id", JSONObject().put("text", text).put("privacy", privacy))
    fun deletePost(id: Long): JSONObject = delete("/api/posts/$id")
    fun createPost(text: String, privacy: String = "public", checkin: String = "", feeling: String = "", photo: String = "", video: String = "", checkinLat: Double? = null, checkinLng: Double? = null): JSONObject {
        val body = JSONObject().put("text", text).put("privacy", privacy).put("checkin", checkin).put("feeling", feeling).put("photo", photo).put("video", video)
        if (checkinLat != null && checkinLng != null) body.put("checkin_lat", checkinLat).put("checkin_lng", checkinLng)
        return post("/api/posts", body)
    }

    fun postDetail(postId: Long): JSONObject = get("/api/posts/$postId")
    fun postComments(postId: Long): JSONArray = get("/api/posts/$postId/comments").optJSONArray("items") ?: JSONArray()
    fun postComments(postItem: Post): JSONArray =
        get(if (postItem.shopId > 0) "/api/shop-posts/${postItem.id}/comments" else "/api/posts/${postItem.id}/comments")
            .optJSONArray("items") ?: JSONArray()

    fun addComment(postId: Long, text: String): JSONObject = post("/api/posts/$postId/comments", JSONObject().put("text", text))
    fun addComment(postItem: Post, text: String, parentId: Long = 0L): JSONObject {
        val body = JSONObject().put("text", text)
        if (parentId > 0) body.put("parent_id", parentId)
        val path = if (postItem.shopId > 0) "/api/shop-posts/${postItem.id}/comments" else "/api/posts/${postItem.id}/comments"
        return post(path, body)
    }

    fun likeComment(commentId: Long): JSONObject = post("/api/comments/$commentId/like")

    fun profileMe(): User = get("/api/me").optJSONObject("user")?.toUser() ?: throw ApiException("Profile unavailable")
    fun updateProfile(fields: JSONObject): User = patch("/api/profile", fields).optJSONObject("user")?.toUser() ?: profileMe()
    fun updatePrivacy(fields: JSONObject): User = patch("/api/privacy", fields).optJSONObject("user")?.toUser() ?: profileMe()
    fun changePassword(currentPassword: String, newPassword: String): JSONObject =
        post("/api/password", JSONObject().put("current_password", currentPassword).put("new_password", newPassword))
    fun verificationStatus(): JSONObject = get("/api/verification")
    fun submitVerification(phone: String, documentType: String, front: String, back: String, selfie: String): JSONObject =
        post(
            "/api/verification",
            JSONObject()
                .put("phone", phone)
                .put("document_type", documentType)
                .put("front", front)
                .put("back", back)
                .put("selfie", selfie)
        )
    fun updateLocation(latitude: Double, longitude: Double): JSONObject =
        post("/api/location", JSONObject().put("latitude", latitude).put("longitude", longitude))
    fun deleteAccount(password: String): JSONObject =
        post("/api/delete-account", JSONObject().put("password", password))
    fun blockedUsers(): List<User> =
        (get("/api/blocked-users").optJSONArray("items") ?: JSONArray()).users()
    fun toggleBlockUser(id: Long): JSONObject = post("/api/users/$id/block")

    fun adminState(): JSONObject = get("/api/admin/state")
    fun adminList(section: String, page: Int = 1, query: String = "", status: String = ""): JSONObject {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val s = java.net.URLEncoder.encode(status, "UTF-8")
        return get("/api/admin/list?section=$section&page=$page&per_page=50&q=$q&status=$s")
    }
    fun adminAction(action: String, id: Long = 0L, fields: JSONObject = JSONObject()): JSONObject {
        val body = JSONObject(fields.toString()).put("action", action)
        if (id > 0) body.put("id", id)
        return post("/api/admin/action", body)
    }
    fun saveTheme(fields: JSONObject): JSONObject = adminAction("theme_settings", fields = fields)
    fun saveBranding(fields: JSONObject): JSONObject = adminAction("site_branding", fields = fields)

    fun weather(): JSONObject = get("/api/weather/current")

    fun geocode(q: String): JSONObject = get("/api/map/geocode?q=${java.net.URLEncoder.encode(q, "UTF-8")}")
    fun geocodePlaces(q: String): List<CheckinPlace> {
        val d = geocode(q)
        val a = d.optJSONArray("items")
        val out = mutableListOf<CheckinPlace>()
        if (a != null) {
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val lat = o.optString("lat", "").toDoubleOrNull() ?: o.optDouble("lat", Double.NaN)
                val lng = o.optString("lng", o.optString("lon", "")).toDoubleOrNull()
                    ?: o.optDouble("lng", o.optDouble("lon", Double.NaN))
                if (!lat.isNaN() && !lng.isNaN()) {
                    out += CheckinPlace(o.optString("display_name", o.optString("name", "Place")), lat, lng)
                }
            }
        }
        return out.take(8)
    }
    fun reverse(lat: Double, lng: Double): JSONObject = get("/api/map/reverse?lat=$lat&lng=$lng")

    fun uploadUri(uri: Uri, kind: String): String {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: when {
            kind.contains("video") -> "video/mp4"
            kind.contains("audio") -> "audio/mpeg"
            else -> "image/jpeg"
        }
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: when {
            mime.startsWith("video/") -> "mp4"
            mime.startsWith("audio/") -> "mp3"
            else -> "jpg"
        }
        val temp = File.createTempFile("mychhachh-upload-", ".$ext", context.cacheDir)
        try {
            resolver.openInputStream(uri)?.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
                ?: throw ApiException("Selected file could not be opened.")
            return upload(temp, mime, kind)
        } finally {
            runCatching { temp.delete() }
        }
    }

    fun upload(file: File, mime: String, kind: String): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("kind", kind)
            .addFormDataPart("file", file.name, file.asRequestBody(mime.toMediaType()))
            .build()
        val req = Request.Builder().url("$BASE/api/upload").post(body).header("Accept", "application/json").build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty(); val d = runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", text) }
            if (!res.isSuccessful) throw ApiException(d.optString("error", "Upload failed"), res.code, d)
            return d.optString("url", "")
        }
    }
}
