package com.mychhachh.app.ui

import android.content.Context
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
    SAVED, RELATIONS, THEME, ANNOUNCEMENT_DETAIL
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
    var menuOpen by mutableStateOf(false)
    var language by mutableStateOf(app.getSharedPreferences("my_chhachh_v95", Context.MODE_PRIVATE).getString("lang", "en") ?: "en")
    var features by mutableStateOf(JSONObject())

    var feedMode by mutableStateOf("for_you")
    var feed by mutableStateOf<List<Post>>(emptyList())
    var people by mutableStateOf<List<User>>(emptyList())
    var shops by mutableStateOf<List<Shop>>(emptyList())
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
    var votes by mutableStateOf<List<Vote>>(emptyList())
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
    var selectedChatUser by mutableStateOf<User?>(null)
    var chatMessages by mutableStateOf<List<Message>>(emptyList())
    var selectedPost by mutableStateOf<Post?>(null)
    var postCommentsList by mutableStateOf<List<Comment>>(emptyList())
    var adminState by mutableStateOf<JSONObject?>(null)
    var adminSection by mutableStateOf("overview")
    var adminSectionData by mutableStateOf<JSONObject?>(null)
    var blockedUsersList by mutableStateOf<List<User>>(emptyList())

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
        withContext(Dispatchers.IO) { api.register(name, username, email, password) }
        user = withContext(Dispatchers.IO) { api.bootstrap().user }
        route = V95Route.HOME
        loadFeed(false)
    }

    fun logout() = work {
        withContext(Dispatchers.IO) { api.logout() }
        user = null
        route = V95Route.HOME
        loadFeed(false)
    }

    fun createPost(text: String, photo: String, video: String, onDone: () -> Unit) = work {
        withContext(Dispatchers.IO) { api.createPost(text = text, photo = photo, video = video) }
        onDone()
        loadFeed(false)
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
        shopDetails = withContext(Dispatchers.IO) { runCatching { api.shop(shop.id) }.getOrNull() }
        route = V95Route.SHOP_DETAIL
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

    fun sendMessage(text: String, done: () -> Unit) = work(false) {
        val to = selectedChatUser ?: return@work
        withContext(Dispatchers.IO) { api.sendMessage(to.id, text) }
        val result = withContext(Dispatchers.IO) { api.chat(to.id) }
        chatMessages = result.second
        done()
    }

    fun loadVotes() = work { votes = withContext(Dispatchers.IO) { api.votes() } }

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

    fun updateProfile(name: String, username: String, bio: String, done: () -> Unit) = work {
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

