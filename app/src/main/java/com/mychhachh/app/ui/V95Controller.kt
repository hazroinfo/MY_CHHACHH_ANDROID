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
    PROFILE, SHOP_DETAIL, CHAT, SEARCH, WEATHER, SETTINGS, ADMIN, AUTH, POST_DETAIL,
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
    var relationUsers by mutableStateOf<List<User>>(emptyList())
    var relationTitle by mutableStateOf("")
    var selectedShop by mutableStateOf<Shop?>(null)
    var shopDetails by mutableStateOf<JSONObject?>(null)
    var selectedShopPosts by mutableStateOf<List<Post>>(emptyList())
    var selectedChatUser by mutableStateOf<User?>(null)
    var chatMessages by mutableStateOf<List<Message>>(emptyList())
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
        profileDetails = withContext(Dispatchers.IO) { runCatching { api.user(person.id) }.getOrNull() }
        route = V95Route.PROFILE
    }

    fun openRelations(person: User, mode: String) = work {
        relationTitle = if (mode == "following") t("Following", "فالوونگ") else t("Followers", "فالوورز")
        relationUsers = withContext(Dispatchers.IO) { api.relationUsers(person.id, mode) }
        route = V95Route.RELATIONS
    }

    fun loadShops() = work { shops = withContext(Dispatchers.IO) { api.shops() } }

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
        val destination = when {
            raw.isNotBlank() -> Uri.parse(raw)
            shop.location.isNotBlank() -> Uri.parse("geo:0,0?q=" + java.net.URLEncoder.encode(shop.location, "UTF-8"))
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

    fun loadMessages() = work { conversations = withContext(Dispatchers.IO) { api.conversations() } }

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
    }

    fun shareVote(vote: Vote) = work(false) { withContext(Dispatchers.IO) { api.shareVote(vote.id) } }

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

    fun loadWeather() = work { weather = withContext(Dispatchers.IO) { api.weather() } }


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

    fun updateProfile(name: String, username: String, bio: String, done: () -> Unit = {}) = work {
        val fields = JSONObject().put("name", name).put("username", username).put("bio", bio)
        user = withContext(Dispatchers.IO) { api.updateProfile(fields) }
        done()
    }

    fun loadAdmin(showBusy: Boolean = true) = work(showBusy) {
        if (user?.isAdmin == true) {
            adminState = withContext(Dispatchers.IO) { api.adminState() }
            adminSectionData = adminState
            adminSection = "overview"
        }
    }
}

