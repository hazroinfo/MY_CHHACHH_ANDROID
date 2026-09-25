package com.mychhachh.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import java.io.File


internal enum class V95Route {
    HOME, PEOPLE, SHOPS, MAP, MESSAGES, VOTES, ANNOUNCEMENTS, NOTIFICATIONS,
    PROFILE, SHOP_DETAIL, CHAT, GROUP_CHAT, SEARCH, WEATHER, SETTINGS, ADMIN, AUTH, POST_DETAIL,
    SAVED, RELATIONS, THEME, ANNOUNCEMENT_DETAIL, VOTE_DETAIL
}

internal class V95Controller(context: Context) {
    private val app = context.applicationContext
    val api = ApiClient(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var route by mutableStateOf(V95Route.HOME)
    var user by mutableStateOf<User?>(null)
    var unread by mutableIntStateOf(0)
    var announcementUnread by mutableIntStateOf(0)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var authInfo by mutableStateOf<String?>(null)
    var pendingVerifyUserId by mutableLongStateOf(0L)
    var passwordResetKey by mutableStateOf("")
    var menuOpen by mutableStateOf(false)
    var language by mutableStateOf(app.getSharedPreferences("my_chhachh_v95", Context.MODE_PRIVATE).getString("lang", "en") ?: "en")
    var features by mutableStateOf(JSONObject())

    var feedMode by mutableStateOf("for_you")
    var mapPickForPost by mutableStateOf(false)
    var mapPickForMessage by mutableStateOf(false)
    var mapPickForGroupMessage by mutableStateOf(false)
    var composerCheckinName by mutableStateOf("")
    var composerCheckinLat by mutableStateOf<Double?>(null)
    var composerCheckinLng by mutableStateOf<Double?>(null)
    var feed by mutableStateOf<List<Post>>(emptyList())
    var people by mutableStateOf<List<User>>(emptyList())
    var shops by mutableStateOf<List<Shop>>(emptyList())
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
    var votes by mutableStateOf<List<Vote>>(emptyList())
    var selectedVote by mutableStateOf<Vote?>(null)
    var voteCommentsList by mutableStateOf<List<Comment>>(emptyList())
    var announcements by mutableStateOf<List<Announcement>>(emptyList())
    var selectedAnnouncement by mutableStateOf<Announcement?>(null)
    var announcementCommentsList by mutableStateOf<List<Comment>>(emptyList())
    var notices by mutableStateOf<List<Notice>>(emptyList())
    var saved by mutableStateOf<List<Post>>(emptyList())
    var weather by mutableStateOf<JSONObject?>(null)
    var searchBundle by mutableStateOf<SearchBundle?>(null)
    var searchText by mutableStateOf("")
    var selectedUser by mutableStateOf<User?>(null)
    var profileDetails by mutableStateOf<JSONObject?>(null)
    var profilePosts by mutableStateOf<List<Post>>(emptyList())
    var relationUsers by mutableStateOf<List<User>>(emptyList())
    var relationTitle by mutableStateOf("")
    var selectedShop by mutableStateOf<Shop?>(null)
    var shopDetails by mutableStateOf<JSONObject?>(null)
    var selectedShopPosts by mutableStateOf<List<Post>>(emptyList())
    var selectedChatUser by mutableStateOf<User?>(null)
    var chatMessages by mutableStateOf<List<Message>>(emptyList())
    var messageGroups by mutableStateOf<List<MessageGroup>>(emptyList())
    var selectedGroup by mutableStateOf<MessageGroup?>(null)
    var selectedGroupName by mutableStateOf("")
    var groupMessages by mutableStateOf<List<Message>>(emptyList())
    var selectedPost by mutableStateOf<Post?>(null)
    var postCommentsList by mutableStateOf<List<Comment>>(emptyList())
    var adminState by mutableStateOf<JSONObject?>(null)
    var adminSection by mutableStateOf("overview")
    var adminSectionData by mutableStateOf<JSONObject?>(null)
    var blockedUsersList by mutableStateOf<List<User>>(emptyList())
    var verificationState by mutableStateOf<JSONObject?>(null)
    var supportTickets by mutableStateOf<List<JSONObject>>(emptyList())

    fun dispose() { scope.cancel() }

    fun t(en: String, ur: String) = if (language == "ur") ur else en

    fun changeLanguage(value: String) {
        language = if (value == "ur") "ur" else "en"
        app.getSharedPreferences("my_chhachh_v95", Context.MODE_PRIVATE).edit().putString("lang", language).apply()
    }

    internal fun work(showBusy: Boolean = true, block: suspend () -> Unit) {
        scope.launch {
            if (showBusy) busy = true
            error = null
            try { block() } catch (e: Throwable) { error = e.message ?: "Request failed" }
            finally { if (showBusy) busy = false }
        }
    }

    fun bootstrap() = work {
        val b = withContext(Dispatchers.IO) { api.bootstrap() }
        user = b.user
        unread = b.unread
        announcementUnread = b.announcementUnread
        features = b.features
        loadFeed(false)
    }

    fun loadFeed(showBusy: Boolean = true) = work(showBusy) {
        val mode = feedMode
        feed = withContext(Dispatchers.IO) { api.feed(mode).first }
    }

    fun changeFeed(mode: String) {
        feedMode = mode
        loadFeed()
    }

    fun login(identity: String, password: String) = work {
        user = withContext(Dispatchers.IO) { api.login(identity, password) }
        route = V95Route.HOME
        loadFeed(false)
    }

    fun register(name: String, username: String, email: String, password: String) = work {
        val r = withContext(Dispatchers.IO) { api.register(name, username, email, password) }
        val pending = r.optLong("pending_user_id", 0L)
        if (r.optBoolean("verify_required", false) && pending > 0L) {
            pendingVerifyUserId = pending
            authInfo = t("Verification code sent to your email.", "ویریفکیشن کوڈ آپ کی ای میل پر بھیج دیا گیا ہے۔")
            route = V95Route.AUTH
        } else {
            user = r.optJSONObject("user")?.toUser() ?: withContext(Dispatchers.IO) { api.bootstrap().user }
            pendingVerifyUserId = 0L
            authInfo = r.optString("warning", "").takeIf { it.isNotBlank() }
            route = V95Route.HOME
            loadFeed(false)
        }
    }

    fun verifyPendingEmail(code: String) = work {
        val id = pendingVerifyUserId
        if (id <= 0L) return@work
        user = withContext(Dispatchers.IO) { api.verifyEmail(id, code) }
        pendingVerifyUserId = 0L
        authInfo = null
        route = V95Route.HOME
        loadFeed(false)
    }

    fun resendPendingEmail() = work(false) {
        val id = pendingVerifyUserId
        if (id <= 0L) return@work
        withContext(Dispatchers.IO) { api.resendCode(id) }
        authInfo = t("A new verification code was sent.", "نیا ویریفکیشن کوڈ بھیج دیا گیا ہے۔")
    }

    fun startPasswordReset(email: String) = work {
        passwordResetKey = withContext(Dispatchers.IO) { api.forgotSend(email) }
        authInfo = t("Password reset code sent to your email.", "پاس ورڈ ری سیٹ کوڈ آپ کی ای میل پر بھیج دیا گیا ہے۔")
    }

    fun finishPasswordReset(code: String, password: String, done: () -> Unit) = work {
        val key = passwordResetKey
        if (key.isBlank()) return@work
        withContext(Dispatchers.IO) { api.forgotReset(key, code, password) }
        passwordResetKey = ""
        authInfo = t("Password changed. You can login now.", "پاس ورڈ تبدیل ہوگیا۔ اب لاگ اِن کریں۔")
        done()
    }

    fun logout() = work {
        withContext(Dispatchers.IO) { api.logout() }
        user = null
        route = V95Route.HOME
        loadFeed(false)
    }

    fun createPost(
        text: String,
        photo: String,
        video: String,
        privacy: String = "public",
        feeling: String = "",
        onDone: () -> Unit
    ) = work {
        val checkin = composerCheckinName
        val lat = composerCheckinLat
        val lng = composerCheckinLng
        withContext(Dispatchers.IO) {
            api.createPost(
                text = text,
                privacy = privacy,
                checkin = checkin,
                feeling = feeling,
                photo = photo,
                video = video,
                checkinLat = lat,
                checkinLng = lng
            )
        }
        composerCheckinName = ""
        composerCheckinLat = null
        composerCheckinLng = null
        onDone()
        loadFeed(false)
    }

    fun startPostCheckin() {
        mapPickForPost = true
        route = V95Route.MAP
    }

    fun setPostCheckin(place: CheckinPlace) {
        composerCheckinName = place.name
        composerCheckinLat = place.lat
        composerCheckinLng = place.lng
        mapPickForPost = false
        route = V95Route.HOME
    }

    fun cancelPostCheckin() {
        mapPickForPost = false
        route = V95Route.HOME
    }

    fun startMessageLocation() {
        if (selectedChatUser == null) return
        mapPickForMessage = true
        mapPickForGroupMessage = false
        route = V95Route.MAP
    }

    fun startGroupMessageLocation() {
        if (selectedGroup == null) return
        mapPickForGroupMessage = true
        mapPickForMessage = false
        route = V95Route.MAP
    }

    fun setMessageLocation(place: CheckinPlace) = work {
        val to = selectedChatUser ?: return@work
        withContext(Dispatchers.IO) {
            api.sendMessage(
                to = to.id,
                text = place.name,
                locationLat = place.lat,
                locationLng = place.lng
            )
        }
        mapPickForMessage = false
        val result = withContext(Dispatchers.IO) { api.chat(to.id) }
        chatMessages = result.second
        route = V95Route.CHAT
    }

    fun setGroupMessageLocation(place: CheckinPlace) = work {
        val group = selectedGroup ?: return@work
        withContext(Dispatchers.IO) {
            api.sendGroupMessage(
                groupId = group.id,
                text = place.name,
                locationLat = place.lat,
                locationLng = place.lng
            )
        }
        mapPickForGroupMessage = false
        val result = withContext(Dispatchers.IO) { api.groupChat(group.id) }
        selectedGroupName = result.first
        groupMessages = result.second
        route = V95Route.GROUP_CHAT
    }

    fun openCoordinates(lat: Double, lng: Double) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(intent) }
            .onFailure { error = t("Map app could not open.", "میپ ایپ نہیں کھل سکی۔") }
    }

    fun upload(uri: Uri, kind: String, done: (String) -> Unit) = work {
        val url = withContext(Dispatchers.IO) { api.uploadUri(uri, kind) }
        done(url)
    }

    fun likePost(post: Post) = work(false) {
        withContext(Dispatchers.IO) { api.likePost(post) }
        loadFeed(false)
    }

    fun savePost(post: Post) = work(false) {
        withContext(Dispatchers.IO) { api.savePost(post.id) }
        loadFeed(false)
    }

    fun sharePost(post: Post) = work(false) {
        withContext(Dispatchers.IO) { api.sharePost(post) }
        val shareUrl = "https://chhachh.pages.dev/post.php?id=${post.id}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareUrl)
        }
        val chooser = Intent.createChooser(shareIntent, t("Share post", "پوسٹ شیئر کریں")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(chooser)
    }

    fun editPost(post: Post, text: String, privacy: String, done: () -> Unit = {}) = work {
        withContext(Dispatchers.IO) { api.updatePost(post.id, text.trim(), privacy) }
        if (selectedPost?.id == post.id) {
            selectedPost = selectedPost?.copy(text = text.trim(), privacy = privacy)
        }
        loadFeed(false)
        done()
    }


    fun openPost(post: Post) = work {
        selectedPost = post
        postCommentsList = withContext(Dispatchers.IO) { api.postComments(post).comments() }
        route = V95Route.POST_DETAIL
    }

    fun reloadPostComments(showBusy: Boolean = false) = work(showBusy) {
        val post = selectedPost ?: return@work
        postCommentsList = withContext(Dispatchers.IO) { api.postComments(post).comments() }
    }

    fun addPostComment(text: String, done: () -> Unit) = work(false) {
        val post = selectedPost ?: return@work
        withContext(Dispatchers.IO) { api.addComment(post, text) }
        postCommentsList = withContext(Dispatchers.IO) { api.postComments(post).comments() }
        done()
    }

    fun deletePost(post: Post) = work {
        withContext(Dispatchers.IO) { api.deletePost(post.id) }
        feed = feed.filterNot { it.id == post.id }
        if (selectedPost?.id == post.id) selectedPost = null
        route = V95Route.HOME
    }

    fun reportPost(post: Post) = work(false) {
        withContext(Dispatchers.IO) { api.reportPost(post.id, "Reported from Android app") }
    }

    fun loadPeople() = work {
        people = withContext(Dispatchers.IO) { api.users().first }
    }

    fun follow(person: User) = work(false) {
        withContext(Dispatchers.IO) { api.followUser(person.id) }
        people = people.map { if (it.id == person.id) it.copy(followed = !it.followed) else it }
    }

    fun openProfile(person: User) = work {
        selectedUser = person
        val details = withContext(Dispatchers.IO) { runCatching { api.user(person.id) }.getOrNull() }
        profileDetails = details
        profilePosts = (details?.optJSONArray("posts") ?: JSONArray()).posts()
        route = V95Route.PROFILE
    }

    fun loadProfile(person: User, showBusy: Boolean = false) = work(showBusy) {
        val details = withContext(Dispatchers.IO) { runCatching { api.user(person.id) }.getOrNull() }
        profileDetails = details
        profilePosts = (details?.optJSONArray("posts") ?: JSONArray()).posts()
    }

    fun openRelations(person: User, mode: String) = work {
        relationTitle = if (mode == "following") t("Following", "فالوونگ") else t("Followers", "فالوورز")
        relationUsers = withContext(Dispatchers.IO) { api.relationUsers(person.id, mode) }
        route = V95Route.RELATIONS
    }

    fun loadShops() = work { shops = withContext(Dispatchers.IO) { api.shops() } }

    fun saveShop(existing: Shop?, fields: JSONObject, done: () -> Unit = {}) = work {
        val me = user ?: run {
            error = t("Please sign in to manage a shop.", "دکان مینیج کرنے کے لیے لاگ اِن کریں۔")
            return@work
        }

        if (existing != null && existing.userId != me.id && !me.isAdmin) {
            error = t("You cannot edit this shop.", "آپ اس دکان کو ایڈٹ نہیں کر سکتے۔")
            return@work
        }

        // Re-check the server before creating so a stale/empty local list can never
        // create a second shop for the same user.
        val serverShops = withContext(Dispatchers.IO) { api.shops() }
        val ownedShop = serverShops.firstOrNull { it.userId == me.id }
        val target = existing ?: ownedShop

        withContext(Dispatchers.IO) {
            if (target == null) {
                api.createShop(fields)
            } else {
                api.updateShop(target.id, fields)
            }
        }

        shops = withContext(Dispatchers.IO) { api.shops() }
        val saved = target?.let { current -> shops.firstOrNull { it.id == current.id } }
            ?: shops.firstOrNull { it.userId == me.id }

        if (saved != null) {
            selectedShop = saved
            shopDetails = withContext(Dispatchers.IO) { runCatching { api.shop(saved.id) }.getOrNull() }
        } else {
            selectedShop = null
            shopDetails = null
        }
        done()
    }

    fun deleteMyShop(shop: Shop, done: () -> Unit = {}) = work {
        val me = user ?: run {
            error = t("Please sign in to manage a shop.", "دکان مینیج کرنے کے لیے لاگ اِن کریں۔")
            return@work
        }
        if (shop.userId != me.id && !me.isAdmin) {
            error = t("You cannot delete this shop.", "آپ اس دکان کو حذف نہیں کر سکتے۔")
            return@work
        }

        withContext(Dispatchers.IO) { api.deleteShop(shop.id) }
        shops = withContext(Dispatchers.IO) { api.shops() }

        if (selectedShop?.id == shop.id) {
            selectedShop = null
            selectedShopPosts = emptyList()
            shopDetails = null
        }
        done()
        route = V95Route.SHOPS
    }

    fun createShopPost(
        shop: Shop,
        text: String,
        privacy: String,
        photo: String,
        video: String,
        done: () -> Unit = {}
    ) = work {
        if (shop.userId != user?.id && user?.isAdmin != true) return@work
        withContext(Dispatchers.IO) { api.createShopPost(shop.id, text, privacy, photo, video) }
        selectedShopPosts = withContext(Dispatchers.IO) { api.shopPosts(shop.id) }
        done()
    }

    fun openShop(shop: Shop) = work {
        selectedShop = shop
        val detail = withContext(Dispatchers.IO) { runCatching { api.shop(shop.id) }.getOrNull() }
        shopDetails = detail
        selectedShopPosts = withContext(Dispatchers.IO) {
            runCatching { api.shopPosts(shop.id) }.getOrElse {
                (detail?.optJSONArray("posts") ?: JSONArray()).posts()
                    .map { post -> if (post.shopId > 0L) post else post.copy(shopId = shop.id) }
            }
        }
        route = V95Route.SHOP_DETAIL
    }

    fun openShopFollowers(shop: Shop) = work {
        relationTitle = t("Shop followers", "دکان کے فالوورز")
        relationUsers = withContext(Dispatchers.IO) { api.shopFollowers(shop.id) }
        route = V95Route.RELATIONS
    }

    fun navigateToShop(shop: Shop) {
        val raw = shop.locationUrl.trim()
        val coordinatePattern = Regex("""^-?\\d{1,2}(?:\\.\\d+)?\\s*,\\s*-?\\d{1,3}(?:\\.\\d+)?$""")
        val destination = when {
            raw.isNotBlank() && coordinatePattern.matches(raw) ->
                Uri.parse("google.navigation:q=" + Uri.encode(raw))
            raw.startsWith("google.navigation:", ignoreCase = true) ||
                raw.startsWith("geo:", ignoreCase = true) ->
                Uri.parse(raw)
            raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true) ->
                Uri.parse(raw)
            raw.isNotBlank() ->
                Uri.parse("https://$raw")
            shop.location.isNotBlank() ->
                Uri.parse("google.navigation:q=" + Uri.encode(shop.location.trim()))
            else -> null
        }

        if (destination == null) {
            error = t("Shop location is not set.", "دکان کی لوکیشن سیٹ نہیں ہے۔")
            return
        }

        val mapsIntent = Intent(Intent.ACTION_VIEW, destination).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallback = Intent(Intent.ACTION_VIEW, destination).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { app.startActivity(mapsIntent) }.isFailure) {
            runCatching { app.startActivity(fallback) }
                .onFailure { error = t("No map app could open this location.", "اس لوکیشن کے لیے کوئی میپ ایپ نہیں کھل سکی۔") }
        }
    }

    fun dialPhone(raw: String) {
        val phone = raw.trim()
        if (phone.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(intent) }
            .onFailure { error = t("Phone app could not open.", "فون ایپ نہیں کھل سکی۔") }
    }

    fun openWhatsApp(raw: String) {
        val digits = raw.filter { it.isDigit() }
        if (digits.isBlank()) return
        val uri = Uri.parse("https://wa.me/$digits")
        val whatsapp = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallback = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (runCatching { app.startActivity(whatsapp) }.isFailure) {
            runCatching { app.startActivity(fallback) }
                .onFailure { error = t("WhatsApp link could not open.", "WhatsApp لنک نہیں کھل سکا۔") }
        }
    }

    fun openSocial(kind: String, raw: String) {
        val value = raw.trim()
        if (value.isBlank()) return
        val url = if (value.startsWith("http://") || value.startsWith("https://")) value else when (kind) {
            "facebook" -> "https://facebook.com/" + value.removePrefix("@")
            "instagram" -> "https://instagram.com/" + value.removePrefix("@")
            "youtube" -> "https://youtube.com/" + value.removePrefix("@")
            else -> "https://" + value
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { app.startActivity(intent) }
            .onFailure { error = t("Link could not open.", "لنک نہیں کھل سکا۔") }
    }

    fun toggleShop(shop: Shop) = work(false) {
        withContext(Dispatchers.IO) { api.toggleShopFollow(shop.id) }
        shops = shops.map { if (it.id == shop.id) it.copy(followed = !it.followed) else it }
        selectedShop = selectedShop?.takeIf { it.id != shop.id } ?: selectedShop?.copy(followed = !shop.followed)
    }

    fun loadMessages() = work {
        conversations = withContext(Dispatchers.IO) { api.conversations() }
        messageGroups = withContext(Dispatchers.IO) { runCatching { api.messageGroups() }.getOrDefault(emptyList()) }
    }

    fun createMessageGroup(name: String, usernames: String, done: () -> Unit = {}) = work {
        val id = withContext(Dispatchers.IO) { api.createMessageGroup(name, usernames) }
        messageGroups = withContext(Dispatchers.IO) { api.messageGroups() }
        val group = messageGroups.firstOrNull { it.id == id }
        if (group != null) {
            selectedGroup = group
            val chat = withContext(Dispatchers.IO) { api.groupChat(group.id) }
            selectedGroupName = chat.first
            groupMessages = chat.second
            route = V95Route.GROUP_CHAT
        }
        done()
    }

    fun openGroup(group: MessageGroup) = work {
        selectedGroup = group
        val result = withContext(Dispatchers.IO) { api.groupChat(group.id) }
        selectedGroupName = result.first
        groupMessages = result.second
        route = V95Route.GROUP_CHAT
    }

    fun sendGroupRichMessage(text: String, photo: String, audio: String, done: () -> Unit = {}) = work(false) {
        val group = selectedGroup ?: return@work
        withContext(Dispatchers.IO) { api.sendGroupMessage(group.id, text, photo, audio) }
        val result = withContext(Dispatchers.IO) { api.groupChat(group.id) }
        selectedGroupName = result.first
        groupMessages = result.second
        done()
    }

    fun openChat(person: User) = work {
        selectedChatUser = person
        val result = withContext(Dispatchers.IO) { api.chat(person.id) }
        selectedChatUser = result.first ?: person
        chatMessages = result.second
        route = V95Route.CHAT
    }

    fun sendMessage(text: String, done: () -> Unit) = sendRichMessage(text, "", "", done)

    fun sendRichMessage(text: String, photo: String, audio: String, done: () -> Unit) = work(false) {
        val to = selectedChatUser ?: return@work
        withContext(Dispatchers.IO) { api.sendMessage(to.id, text, photo, audio) }
        val result = withContext(Dispatchers.IO) { api.chat(to.id) }
        chatMessages = result.second
        done()
    }

    fun loadVotes() = work { votes = withContext(Dispatchers.IO) { api.votes() } }

    fun createVote(opponentUsername: String, durationHours: Int, statement: String, done: () -> Unit = {}) = work {
        withContext(Dispatchers.IO) { api.createVote(opponentUsername, durationHours, statement) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        done()
    }

    fun respondVote(vote: Vote, decision: String) = work {
        withContext(Dispatchers.IO) { api.respondVote(vote.id, decision) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        selectedVote = votes.firstOrNull { it.id == vote.id } ?: selectedVote
    }

    fun startVote(vote: Vote) = work {
        withContext(Dispatchers.IO) { api.startVote(vote.id) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        selectedVote = votes.firstOrNull { it.id == vote.id } ?: selectedVote
    }

    fun cancelVote(vote: Vote) = work {
        withContext(Dispatchers.IO) { api.cancelVote(vote.id) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        selectedVote = votes.firstOrNull { it.id == vote.id }
        if (selectedVote == null) route = V95Route.VOTES
    }

    fun leaveVote(vote: Vote) = work {
        withContext(Dispatchers.IO) { api.leaveVote(vote.id) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        selectedVote = votes.firstOrNull { it.id == vote.id }
        if (selectedVote == null) route = V95Route.VOTES
    }

    fun updateVoteStatement(vote: Vote, text: String, done: () -> Unit = {}) = work {
        withContext(Dispatchers.IO) { api.updateVoteStatement(vote.id, text) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        selectedVote = votes.firstOrNull { it.id == vote.id } ?: selectedVote
        done()
    }


    fun openVote(vote: Vote) = work {
        selectedVote = vote
        voteCommentsList = withContext(Dispatchers.IO) { api.voteComments(vote.id).comments() }
        route = V95Route.VOTE_DETAIL
    }

    fun addVoteComment(text: String, done: () -> Unit) = work(false) {
        val vote = selectedVote ?: return@work
        withContext(Dispatchers.IO) { api.addVoteComment(vote.id, text) }
        voteCommentsList = withContext(Dispatchers.IO) { api.voteComments(vote.id).comments() }
        done()
    }

    fun castVote(vote: Vote, userId: Long) = work(false) {
        withContext(Dispatchers.IO) { api.castVote(vote.id, userId) }
        votes = withContext(Dispatchers.IO) { api.votes() }
        selectedVote = votes.firstOrNull { it.id == vote.id } ?: selectedVote
    }

    fun shareVote(vote: Vote) = work(false) {
        withContext(Dispatchers.IO) { api.shareVote(vote.id) }
        val url = "https://chhachh.pages.dev/vote.php?id=${vote.id}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        val chooser = Intent.createChooser(shareIntent, t("Share voting", "ووٹنگ شیئر کریں")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(chooser)
    }

    fun loadAnnouncements() = work {
        val r = withContext(Dispatchers.IO) { api.announcements() }
        announcements = r.first
        announcementUnread = r.second
        withContext(Dispatchers.IO) { runCatching { api.markAnnouncementsRead() } }
    }


    fun uploadFile(file: File, mime: String, kind: String, done: (String) -> Unit) = work {
        val url = withContext(Dispatchers.IO) { api.upload(file, mime, kind) }
        done(url)
    }

    fun openAnnouncement(item: Announcement) = work {
        selectedAnnouncement = item
        announcementCommentsList = withContext(Dispatchers.IO) { api.announcementComments(item.id) }
        route = V95Route.ANNOUNCEMENT_DETAIL
    }

    fun addAnnouncementComment(text: String, done: () -> Unit) = work(false) {
        val item = selectedAnnouncement ?: return@work
        withContext(Dispatchers.IO) { api.addAnnouncementComment(item.id, text) }
        announcementCommentsList = withContext(Dispatchers.IO) { api.announcementComments(item.id) }
        done()
    }

    fun createAnnouncement(text: String, photo: String, audio: String, done: () -> Unit) = work {
        withContext(Dispatchers.IO) { api.createAnnouncement(text, photo, audio, if (user?.isAdmin == true) "admin" else "announcement") }
        done()
        loadAnnouncements()
    }

    fun editAnnouncement(item: Announcement, text: String, done: () -> Unit = {}) = work {
        withContext(Dispatchers.IO) { api.editAnnouncement(item.id, text, item.type) }
        announcements = withContext(Dispatchers.IO) { api.announcements().first }
        selectedAnnouncement = announcements.firstOrNull { it.id == item.id } ?: item.copy(text = text)
        done()
    }

    fun deleteAnnouncement(item: Announcement) = work {
        withContext(Dispatchers.IO) { api.deleteAnnouncement(item.id) }
        announcements = announcements.filterNot { it.id == item.id }
        if (selectedAnnouncement?.id == item.id) selectedAnnouncement = null
        route = V95Route.ANNOUNCEMENTS
    }

    fun reportAnnouncement(item: Announcement) = work(false) {
        withContext(Dispatchers.IO) { api.reportAnnouncement(item.id, "Reported from Android app") }
    }

    fun likeAnnouncement(a: Announcement) = work(false) {
        withContext(Dispatchers.IO) { api.toggleAnnouncementLike(a.id) }
        loadAnnouncements()
    }

    fun loadNotifications() = work {
        val r = withContext(Dispatchers.IO) { api.notifications() }
        notices = r.first; unread = r.second
        withContext(Dispatchers.IO) { runCatching { api.markNotificationsRead() } }
        unread = 0
    }

    fun loadSaved() = work { saved = withContext(Dispatchers.IO) { api.saved() } }

    fun search(q: String) = work {
        searchText = q
        searchBundle = withContext(Dispatchers.IO) { api.search(q) }
        route = V95Route.SEARCH
    }

    fun loadWeather(showBusy: Boolean = true) = work(showBusy) {
        weather = withContext(Dispatchers.IO) { api.weather() }
    }


    fun savePrivacy(
        profileVisibility: String,
        showEmail: Boolean,
        showPhone: Boolean,
        showLocation: Boolean,
        hideFollowers: Boolean,
        acceptMessages: Boolean
    ) = work {
        val fields = JSONObject()
            .put("profile_visibility", profileVisibility)
            .put("show_email", showEmail)
            .put("show_phone", showPhone)
            .put("show_location", showLocation)
            .put("hide_followers", hideFollowers)
            .put("accept_messages", acceptMessages)
        user = withContext(Dispatchers.IO) { api.updatePrivacy(fields) }
    }

    fun changePassword(current: String, next: String, done: () -> Unit) = work {
        withContext(Dispatchers.IO) { api.changePassword(current, next) }
        done()
    }


    fun deleteAccount(password: String) = work {
        withContext(Dispatchers.IO) { api.deleteAccount(password) }
        user = null
        selectedUser = null
        feed = emptyList()
        route = V95Route.HOME
        loadFeed(false)
    }

    fun loadBlockedUsers() = work {
        blockedUsersList = withContext(Dispatchers.IO) { api.blockedUsers() }
    }

    fun toggleBlock(person: User) = work(false) {
        withContext(Dispatchers.IO) { api.toggleBlockUser(person.id) }
        blockedUsersList = blockedUsersList.filterNot { it.id == person.id }
    }

    fun loadAccountTools(showBusy: Boolean = false) = work(showBusy) {
        verificationState = withContext(Dispatchers.IO) { runCatching { api.verificationStatus() }.getOrNull() }
        supportTickets = withContext(Dispatchers.IO) { runCatching { api.supportTickets() }.getOrDefault(emptyList()) }
    }

    fun submitVerification(
        phone: String,
        documentType: String,
        front: String,
        back: String,
        selfie: String,
        done: () -> Unit = {}
    ) = work {
        verificationState = withContext(Dispatchers.IO) {
            api.submitVerification(phone, documentType, front, back, selfie)
        }
        done()
        loadAccountTools(false)
    }

    fun submitSupport(
        category: String,
        subject: String,
        message: String,
        done: () -> Unit = {}
    ) = work {
        withContext(Dispatchers.IO) { api.submitSupport(category, subject, message) }
        done()
        supportTickets = withContext(Dispatchers.IO) { api.supportTickets() }
    }

    fun openAdminSection(section: String) = work {
        adminSection = section
        adminSectionData = if (section == "overview") {
            withContext(Dispatchers.IO) { api.adminState() }
        } else {
            withContext(Dispatchers.IO) { api.adminList(section) }
        }
    }

    fun runAdminAction(action: String, id: Long = 0L, fields: JSONObject = JSONObject()) = work {
        withContext(Dispatchers.IO) { api.adminAction(action, id, fields) }
        if (adminSection == "overview") loadAdmin(false) else {
            adminSectionData = withContext(Dispatchers.IO) { api.adminList(adminSection) }
        }
    }

    fun saveThemeFields(fields: JSONObject) = work {
        val r = withContext(Dispatchers.IO) { api.saveTheme(fields) }
        val saved = r.optJSONObject("settings")
        if (saved != null) {
            features = JSONObject(features.toString()).apply {
                saved.keys().forEach { key -> put(key, saved.opt(key)) }
            }
        }
    }

    fun saveBrandFields(fields: JSONObject) = work {
        val r = withContext(Dispatchers.IO) { api.saveBranding(fields) }
        val saved = r.optJSONObject("settings")
        if (saved != null) {
            features = JSONObject(features.toString()).apply {
                saved.keys().forEach { key -> put(key, saved.opt(key)) }
            }
        }
    }

    fun updateProfileFields(fields: JSONObject, done: () -> Unit = {}) = work {
        user = withContext(Dispatchers.IO) { api.updateProfile(fields) }
        selectedUser = selectedUser?.let { selected ->
            if (selected.id == user?.id) user else selected
        }
        done()
    }

    fun updateProfile(name: String, username: String, bio: String, done: () -> Unit = {}) {
        updateProfileFields(
            JSONObject()
                .put("name", name)
                .put("username", username)
                .put("bio", bio),
            done
        )
    }

    fun loadAdmin(showBusy: Boolean = true) = work(showBusy) {
        if (user?.isAdmin == true) {
            adminState = withContext(Dispatchers.IO) { api.adminState() }
            adminSectionData = adminState
            adminSection = "overview"
        }
    }
}

