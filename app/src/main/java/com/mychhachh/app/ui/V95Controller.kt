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


internal enum class V95Route {
    HOME, PEOPLE, SHOPS, MAP, MESSAGES, VOTES, ANNOUNCEMENTS, NOTIFICATIONS,
    PROFILE, SHOP_DETAIL, CHAT, SEARCH, WEATHER, SETTINGS, ADMIN, AUTH
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

    var feedMode by mutableStateOf("for_you")
    var feed by mutableStateOf<List<Post>>(emptyList())
    var people by mutableStateOf<List<User>>(emptyList())
    var shops by mutableStateOf<List<Shop>>(emptyList())
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
    var votes by mutableStateOf<List<Vote>>(emptyList())
    var announcements by mutableStateOf<List<Announcement>>(emptyList())
    var notices by mutableStateOf<List<Notice>>(emptyList())
    var saved by mutableStateOf<List<Post>>(emptyList())
    var weather by mutableStateOf<JSONObject?>(null)
    var searchBundle by mutableStateOf<SearchBundle?>(null)
    var searchText by mutableStateOf("")
    var selectedUser by mutableStateOf<User?>(null)
    var selectedShop by mutableStateOf<Shop?>(null)
    var selectedChatUser by mutableStateOf<User?>(null)
    var chatMessages by mutableStateOf<List<Message>>(emptyList())
    var adminState by mutableStateOf<JSONObject?>(null)

    fun dispose() { scope.cancel() }

    fun t(en: String, ur: String) = if (language == "ur") ur else en

    fun setLanguage(value: String) {
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

    fun loadPeople() = work {
        people = withContext(Dispatchers.IO) { api.users().first }
    }

    fun follow(person: User) = work(false) {
        withContext(Dispatchers.IO) { api.followUser(person.id) }
        people = people.map { if (it.id == person.id) it.copy(followed = !it.followed) else it }
    }

    fun openProfile(person: User) {
        selectedUser = person
        route = V95Route.PROFILE
    }

    fun loadShops() = work { shops = withContext(Dispatchers.IO) { api.shops() } }

    fun openShop(shop: Shop) {
        selectedShop = shop
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

    fun updateProfile(name: String, username: String, bio: String, done: () -> Unit) = work {
        val fields = JSONObject().put("name", name).put("username", username).put("bio", bio)
        user = withContext(Dispatchers.IO) { api.updateProfile(fields) }
        done()
    }

    fun loadAdmin() = work {
        if (user?.isAdmin == true) adminState = withContext(Dispatchers.IO) { api.adminState() }
    }
}

