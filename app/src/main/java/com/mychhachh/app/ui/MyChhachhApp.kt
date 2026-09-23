package com.mychhachh.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.mychhachh.app.data.*
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.screens.*
import com.mychhachh.app.ui.theme.*

enum class Screen { HOME, PEOPLE, SHOPS, MAP, MESSAGES, NOTIFICATIONS, ANNOUNCEMENTS, VOTES, SAVED, SEARCH, PROFILE, SHOP_DETAIL, CHAT, GROUP_CHAT, SETTINGS, WEATHER, THEME, ADMIN, AUTH }

@Composable
fun MyChhachhApp() {
    val context = LocalContext.current
    val api = remember { ApiClient(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var booting by remember { mutableStateOf(true) }
    var me by remember { mutableStateOf<User?>(null) }
    var features by remember { mutableStateOf(JSONObject()) }
    var unread by remember { mutableIntStateOf(0) }
    var announcementUnread by remember { mutableIntStateOf(0) }
    var route by remember { mutableStateOf(Screen.HOME) }
    val backStack = remember { mutableStateListOf<Pair<Screen, Long>>() }
    var selectedId by remember { mutableLongStateOf(0L) }
    var menuOpen by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf("login") }
    var authBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var pendingUserId by remember { mutableLongStateOf(0L) }
    var pendingEmail by remember { mutableStateOf("") }
    var resetKey by remember { mutableStateOf("") }

    var feedMode by remember { mutableStateOf("global") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    var nextBefore by remember { mutableLongStateOf(0L) }

    var people by remember { mutableStateOf<List<User>>(emptyList()) }
    var peopleLoading by remember { mutableStateOf(false) }
    var peopleError by remember { mutableStateOf<String?>(null) }
    var peopleQuery by remember { mutableStateOf("") }

    var shops by remember { mutableStateOf<List<Shop>>(emptyList()) }
    var shopsLoading by remember { mutableStateOf(false) }
    var shopsError by remember { mutableStateOf<String?>(null) }
    var shopQuery by remember { mutableStateOf("") }

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var messageGroups by remember { mutableStateOf<List<MessageGroup>>(emptyList()) }
    var conversationsLoading by remember { mutableStateOf(false) }
    var conversationsError by remember { mutableStateOf<String?>(null) }
    var chatOther by remember { mutableStateOf<User?>(null) }
    var chatMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var chatLoading by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var groupChatName by remember { mutableStateOf("Group") }
    var groupChatMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var groupChatLoading by remember { mutableStateOf(false) }
    var groupChatError by remember { mutableStateOf<String?>(null) }

    var notices by remember { mutableStateOf<List<Notice>>(emptyList()) }
    var noticesLoading by remember { mutableStateOf(false) }
    var noticesError by remember { mutableStateOf<String?>(null) }

    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var announcementsLoading by remember { mutableStateOf(false) }
    var announcementsError by remember { mutableStateOf<String?>(null) }

    var votes by remember { mutableStateOf<List<Vote>>(emptyList()) }
    var votesLoading by remember { mutableStateOf(false) }
    var votesError by remember { mutableStateOf<String?>(null) }

    var saved by remember { mutableStateOf<List<Post>>(emptyList()) }
    var savedLoading by remember { mutableStateOf(false) }
    var savedError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf(SearchBundle(emptyList(), emptyList(), emptyList())) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var profileData by remember { mutableStateOf<JSONObject?>(null) }
    var profileLoading by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var shopData by remember { mutableStateOf<JSONObject?>(null) }
    var shopDetailLoading by remember { mutableStateOf(false) }
    var shopDetailError by remember { mutableStateOf<String?>(null) }

    var mapQuery by remember { mutableStateOf("") }
    var mapData by remember { mutableStateOf<JSONObject?>(null) }
    var mapLoading by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var weatherData by remember { mutableStateOf<JSONObject?>(null) }
    var weatherLoading by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    var settingsBusy by remember { mutableStateOf(false) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    var blockedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var verificationState by remember { mutableStateOf<JSONObject?>(null) }
    var supportTickets by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    var adminState by remember { mutableStateOf<JSONObject?>(null) }
    var adminList by remember { mutableStateOf<JSONObject?>(null) }
    var adminSection by remember { mutableStateOf("users") }
    var adminQuery by remember { mutableStateOf("") }
    var adminLoading by remember { mutableStateOf(false) }
    var adminError by remember { mutableStateOf<String?>(null) }
    var themeBusy by remember { mutableStateOf(false) }
    var themeError by remember { mutableStateOf<String?>(null) }

    var commentPost by remember { mutableStateOf<Post?>(null) }
    var discussionComments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var discussionLikes by remember { mutableStateOf<List<User>>(emptyList()) }
    var discussionLoading by remember { mutableStateOf(false) }
    var discussionError by remember { mutableStateOf<String?>(null) }
    var commentBusy by remember { mutableStateOf(false) }

    fun open(screen: Screen, id: Long = 0L) {
        if (route != Screen.AUTH && (route != screen || selectedId != id)) {
            backStack.add(route to selectedId)
        }
        selectedId = id
        route = screen
        menuOpen = false
    }

    fun loadFeed(reset: Boolean = true) {
        if (feedLoading) return
        scope.launch {
            feedLoading = true; feedError = null
            try {
                val before = if (reset) 0L else nextBefore
                val result = withContext(Dispatchers.IO) { api.feed(feedMode, before) }
                if (reset) posts = result.first else posts = posts + result.first
                nextBefore = result.second
            } catch (e: Exception) { feedError = e.message ?: "Could not load posts." }
            finally { feedLoading = false }
        }
    }

    fun loadPeople() {
        scope.launch { peopleLoading = true; peopleError = null; try { people = withContext(Dispatchers.IO) { api.users(peopleQuery).first } } catch (e: Exception) { peopleError = e.message }; peopleLoading = false }
    }
    fun loadShops() {
        scope.launch { shopsLoading = true; shopsError = null; try { shops = withContext(Dispatchers.IO) { api.shops(shopQuery) } } catch (e: Exception) { shopsError = e.message }; shopsLoading = false }
    }
    fun loadMessages() {
        scope.launch {
            conversationsLoading = true
            conversationsError = null
            try {
                val d = withContext(Dispatchers.IO) { api.conversations() to api.messageGroups() }
                conversations = d.first
                messageGroups = d.second
            } catch (e: Exception) {
                conversationsError = e.message
            }
            conversationsLoading = false
        }
    }
    fun loadNotices() {
        scope.launch { noticesLoading = true; noticesError = null; try { val d = withContext(Dispatchers.IO) { api.notifications() }; notices = d.first; unread = d.second } catch (e: Exception) { noticesError = e.message }; noticesLoading = false }
    }
    fun loadAnnouncements() {
        scope.launch { announcementsLoading = true; announcementsError = null; try { val d = withContext(Dispatchers.IO) { api.announcements() }; announcements = d.first; announcementUnread = d.second } catch (e: Exception) { announcementsError = e.message }; announcementsLoading = false }
    }
    fun loadVotes() {
        scope.launch { votesLoading = true; votesError = null; try { votes = withContext(Dispatchers.IO) { api.votes() } } catch (e: Exception) { votesError = e.message }; votesLoading = false }
    }
    fun loadSaved() {
        scope.launch { savedLoading = true; savedError = null; try { saved = withContext(Dispatchers.IO) { api.saved() } } catch (e: Exception) { savedError = e.message }; savedLoading = false }
    }
    fun doSearch() {
        if (searchQuery.isBlank()) return
        scope.launch { searchLoading = true; searchError = null; try { searchResult = withContext(Dispatchers.IO) { api.search(searchQuery.trim()) } } catch (e: Exception) { searchError = e.message }; searchLoading = false }
    }
    fun loadProfile(id: Long) {
        scope.launch { profileLoading = true; profileError = null; profileData = null; try { profileData = withContext(Dispatchers.IO) { api.user(id) } } catch (e: Exception) { profileError = e.message }; profileLoading = false }
    }
    fun loadShop(id: Long) {
        scope.launch { shopDetailLoading = true; shopDetailError = null; shopData = null; try { shopData = withContext(Dispatchers.IO) { api.shop(id) } } catch (e: Exception) { shopDetailError = e.message }; shopDetailLoading = false }
    }
    fun loadChat(id: Long) {
        scope.launch { chatLoading = true; chatError = null; try { val d = withContext(Dispatchers.IO) { api.chat(id) }; chatOther = d.first; chatMessages = d.second } catch (e: Exception) { chatError = e.message }; chatLoading = false }
    }
    fun loadGroupChat(id: Long) {
        scope.launch {
            groupChatLoading = true
            groupChatError = null
            try {
                val d = withContext(Dispatchers.IO) { api.groupChat(id) }
                groupChatName = d.first
                groupChatMessages = d.second
            } catch (e: Exception) {
                groupChatError = e.message
            }
            groupChatLoading = false
        }
    }
    fun loadWeather() {
        scope.launch { weatherLoading = true; weatherError = null; try { weatherData = withContext(Dispatchers.IO) { api.weather() } } catch (e: Exception) { weatherError = e.message }; weatherLoading = false }
    }
    fun loadBlockedUsers() {
        scope.launch {
            try { blockedUsers = withContext(Dispatchers.IO) { api.blockedUsers() } }
            catch (e: Exception) { settingsError = e.message }
        }
    }
    fun loadVerification() {
        scope.launch {
            try { verificationState = withContext(Dispatchers.IO) { api.verificationStatus() } }
            catch (e: Exception) { settingsError = e.message }
        }
    }

    fun loadSupport() {
        scope.launch {
            try { supportTickets = withContext(Dispatchers.IO) { api.supportTickets() } }
            catch (e: Exception) { settingsError = e.message }
        }
    }
    fun loadAdmin(loadState: Boolean = true) {
        if (me?.isAdmin != true) return
        scope.launch {
            adminLoading = true
            adminError = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val state = if (loadState) api.adminState() else null
                    val list = api.adminList(adminSection, query = adminQuery)
                    state to list
                }
                if (result.first != null) adminState = result.first
                adminList = result.second
            } catch (e: Exception) {
                adminError = e.message ?: "Admin data could not be loaded."
            }
            adminLoading = false
        }
    }

    fun applyLiveSettings(update: JSONObject) {
        val merged = JSONObject(features.toString())
        val updateKeys = update.keys()
        while (updateKeys.hasNext()) {
            val key = updateKeys.next()
            if (!update.isNull(key)) merged.put(key, update.get(key))
        }
        features = merged
        LiveJellyTheme.apply(merged)

        val state = JSONObject((adminState ?: JSONObject()).toString())
        val stateSettings = JSONObject((state.optJSONObject("settings") ?: JSONObject()).toString())
        val stateKeys = update.keys()
        while (stateKeys.hasNext()) {
            val key = stateKeys.next()
            if (!update.isNull(key)) stateSettings.put(key, update.get(key))
        }
        state.put("settings", stateSettings)
        adminState = state
    }

    fun sharePost(post: Post) {
        scope.launch { runCatching { withContext(Dispatchers.IO) { api.sharePost(post) } } }
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://chhachh.pages.dev/post.php?id=${post.id}")
        runCatching { context.startActivity(Intent.createChooser(share, "Share post")) }
    }

    fun openDiscussion(post: Post) {
        commentPost = post
        discussionComments = emptyList()
        discussionLikes = emptyList()
        discussionError = null
        discussionLoading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val comments = api.postComments(post).comments()
                    val likes = if (post.shopId == 0L) {
                        api.postDetail(post.id).optJSONArray("likes_users")?.users().orEmpty()
                    } else emptyList()
                    comments to likes
                }
                discussionComments = result.first
                discussionLikes = result.second
            } catch (e: Exception) {
                discussionError = e.message ?: "Could not load comments."
            }
            discussionLoading = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            val b = withContext(Dispatchers.IO) { api.bootstrap() }
            me = b.user; unread = b.unread; announcementUnread = b.announcementUnread; features = b.features; LiveJellyTheme.apply(b.features)
        } catch (_: Exception) { me = null }
        booting = false
        loadFeed(true)
        loadWeather()
    }

    LaunchedEffect(feedMode) { if (!booting) loadFeed(true) }
    LaunchedEffect(route, selectedId, me?.id) {
        if (booting) return@LaunchedEffect
        when (route) {
            Screen.HOME -> if (me != null) {
                if (people.isEmpty()) loadPeople()
                if (shops.isEmpty()) loadShops()
            }
            Screen.PEOPLE -> if (me != null) loadPeople()
            Screen.SHOPS -> if (me != null) loadShops()
            Screen.MESSAGES -> if (me != null) loadMessages()
            Screen.NOTIFICATIONS -> if (me != null) loadNotices()
            Screen.ANNOUNCEMENTS -> if (me != null) {
                loadAnnouncements()
                scope.launch { runCatching { withContext(Dispatchers.IO) { api.markAnnouncementsRead() } }; announcementUnread = 0 }
            }
            Screen.VOTES -> loadVotes()
            Screen.SAVED -> if (me != null) loadSaved()
            Screen.PROFILE -> if (me != null && selectedId > 0) loadProfile(selectedId)
            Screen.SHOP_DETAIL -> if (me != null && selectedId > 0) loadShop(selectedId)
            Screen.CHAT -> if (me != null && selectedId > 0) loadChat(selectedId)
            Screen.GROUP_CHAT -> if (me != null && selectedId > 0) loadGroupChat(selectedId)
            Screen.WEATHER -> loadWeather()
            Screen.SETTINGS -> if (me != null) {
                loadBlockedUsers()
                loadVerification()
                loadSupport()
            }
            Screen.ADMIN -> if (me?.isAdmin == true) loadAdmin(true)
            Screen.THEME -> if (me?.isAdmin == true) loadAdmin(true)
            else -> Unit
        }
    }

    if (booting) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(JellyBg, JellyBg2))), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Brand(); Spacer(Modifier.height(18.dp)); CircularProgressIndicator(color = JellyPurple) }
        }
        return
    }

    val currentUser = me
    val isPrimaryRoute = route == Screen.HOME || route == Screen.PEOPLE || route == Screen.SHOPS || route == Screen.MAP || route == Screen.MESSAGES

    fun goBack() {
        when {
            menuOpen -> menuOpen = false
            route == Screen.AUTH -> { route = Screen.HOME; selectedId = 0L; backStack.clear() }
            backStack.isNotEmpty() -> {
                val (target, id) = backStack.removeAt(backStack.lastIndex)
                route = target
                selectedId = id
            }
            else -> { route = Screen.HOME; selectedId = 0L }
        }
    }

    BackHandler(enabled = menuOpen || route != Screen.HOME) { goBack() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(JellyBg, JellyBg2)))) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (route != Screen.AUTH) {
                if (currentUser == null) GuestHeader(
                    brandName = features.optString("site_name", "My Chhachh"),
                    brandTagline = features.optString("site_tagline", "Connect with people for information."),
                    brandIcon = if (features.optInt("site_icon_enabled", 1) != 0) mediaUrl(features.optString("site_icon", "")) else null,
                    onLogin = { authMode = "login"; authError = null; route = Screen.AUTH },
                    onRegister = { authMode = "register"; authError = null; route = Screen.AUTH }
                ) else AuthHeader(
                    user = currentUser,
                    brandName = features.optString("site_name", "My Chhachh"),
                    brandTagline = features.optString("site_tagline", "Connect with people for information."),
                    brandIcon = if (features.optInt("site_icon_enabled", 1) != 0) mediaUrl(features.optString("site_icon", "")) else null,
                    route = route,
                    unread = unread,
                    announcementUnread = announcementUnread,
                    weatherText = weatherData?.optJSONObject("current")?.let { current ->
                        val temp = current.optDouble("temperature_2m", Double.NaN)
                        if (temp.isNaN()) "Weather" else "${temp.toInt()}°C · Weather"
                    } ?: "Weather",
                    onMenu = { if (isPrimaryRoute) menuOpen = true else goBack() },
                    onSearch = { open(Screen.SEARCH) },
                    onNotifications = { open(Screen.NOTIFICATIONS) },
                    onProfile = { open(Screen.PROFILE, currentUser.id) },
                    onWeather = { open(Screen.WEATHER) },
                    onVotes = { open(Screen.VOTES) },
                    onAnnouncements = { open(Screen.ANNOUNCEMENTS) },
                    settings = features,
                    onNav = {
                        route = it
                        selectedId = 0L
                        backStack.clear()
                        menuOpen = false
                    }
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (route) {
                    Screen.AUTH -> AuthScreen(
                        mode = authMode, busy = authBusy, error = authError, pendingEmail = pendingEmail,
                        onLogin = { identity, password ->
                            scope.launch {
                                authBusy = true; authError = null
                                try {
                                    val u = withContext(Dispatchers.IO) { api.login(identity, password) }
                                    me = u
                                    val b = withContext(Dispatchers.IO) { api.bootstrap() }
                                    unread = b.unread; announcementUnread = b.announcementUnread; features = b.features; LiveJellyTheme.apply(b.features)
                                    route = Screen.HOME; selectedId = 0L; backStack.clear(); feedMode = "global"; loadFeed(true)
                                } catch (e: ApiException) {
                                    if (e.payload?.optBoolean("verify_required", false) == true) {
                                        pendingUserId = e.payload.optLong("pending_user_id")
                                        pendingEmail = identity.takeIf { it.contains('@') }.orEmpty()
                                        authMode = "verify"
                                        authError = "Email verification required. Enter the code sent to your email."
                                    } else authError = e.message
                                } catch (e: Exception) { authError = e.message ?: "Login failed." }
                                authBusy = false
                            }
                        },
                        onRegister = { name, username, email, password ->
                            scope.launch {
                                authBusy = true; authError = null
                                try {
                                    val d = withContext(Dispatchers.IO) { api.register(name, username, email, password) }
                                    val u = d.optJSONObject("user")?.toUser()
                                    if (u != null) {
                                        me = u; route = Screen.HOME; selectedId = 0L; backStack.clear(); feedMode = "global"; loadFeed(true)
                                    } else if (d.optBoolean("verify_required", false)) {
                                        pendingUserId = d.optLong("pending_user_id")
                                        pendingEmail = email
                                        authMode = "verify"
                                        authError = null
                                    } else {
                                        authMode = "login"; authError = "Account created. Please login."
                                    }
                                } catch (e: Exception) { authError = e.message ?: "Registration failed." }
                                authBusy = false
                            }
                        },
                        onVerify = { code ->
                            scope.launch {
                                authBusy = true; authError = null
                                try {
                                    if (pendingUserId <= 0) throw ApiException("Verification session is missing. Please login again.")
                                    val u = withContext(Dispatchers.IO) { api.verifyEmail(pendingUserId, code) }
                                    me = u; pendingUserId = 0; pendingEmail = ""
                                    val b = withContext(Dispatchers.IO) { api.bootstrap() }
                                    unread = b.unread; announcementUnread = b.announcementUnread; features = b.features; LiveJellyTheme.apply(b.features)
                                    route = Screen.HOME; selectedId = 0L; backStack.clear(); feedMode = "global"; loadFeed(true)
                                } catch (e: Exception) { authError = e.message ?: "Verification failed." }
                                authBusy = false
                            }
                        },
                        onResend = {
                            scope.launch {
                                authBusy = true; authError = null
                                try {
                                    if (pendingUserId <= 0) throw ApiException("Verification session is missing. Please login again.")
                                    withContext(Dispatchers.IO) { api.resendCode(pendingUserId) }
                                    authError = "A new verification code was sent."
                                } catch (e: Exception) { authError = e.message ?: "Could not resend code." }
                                authBusy = false
                            }
                        },
                        onForgotSend = { email ->
                            scope.launch {
                                authBusy = true; authError = null
                                try {
                                    resetKey = withContext(Dispatchers.IO) { api.forgotSend(email) }
                                    pendingEmail = email
                                    authMode = "reset"
                                } catch (e: Exception) { authError = e.message ?: "Could not send reset code." }
                                authBusy = false
                            }
                        },
                        onForgotReset = { code, password ->
                            scope.launch {
                                authBusy = true; authError = null
                                try {
                                    if (resetKey.isBlank()) throw ApiException("Password reset session is missing. Start again.")
                                    withContext(Dispatchers.IO) { api.forgotReset(resetKey, code, password) }
                                    resetKey = ""; authMode = "login"; authError = "Password changed. You can login now."
                                } catch (e: Exception) { authError = e.message ?: "Password reset failed." }
                                authBusy = false
                            }
                        },
                        onSwitch = { authMode = it; authError = null },
                        onBack = { route = Screen.HOME; selectedId = 0L; backStack.clear() }
                    )
                    Screen.HOME -> HomeScreen(
                        currentUser, feedMode, posts, feedLoading, feedError, nextBefore > 0,
                        peopleSuggestions = people,
                        shopSuggestions = shops,
                        onMode = { feedMode = it },
                        onLogin = { authMode = "login"; route = Screen.AUTH }, onRegister = { authMode = "register"; route = Screen.AUTH },
                        onProfile = { if (currentUser != null) open(Screen.PROFILE, it) else { authMode = "login"; route = Screen.AUTH } },
                        onOpenPeople = { open(Screen.PEOPLE) },
                        onOpenShops = { open(Screen.SHOPS) },
                        onOpenShop = { open(Screen.SHOP_DETAIL, it) },
                        onFollowSuggestion = { id ->
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { api.followUser(id) } }
                                loadPeople()
                            }
                        },
                        onLike = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p) } }; loadFeed(true) } },
                        onComment = { p -> openDiscussion(p) },
                        onShare = ::sharePost,
                        onSave = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadFeed(true) } },
                        onEditPost = { p, text, privacy ->
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { api.updatePost(p.id, text, privacy) } }
                                    .onFailure { feedError = it.message }
                                loadFeed(true)
                            }
                        },
                        onDeletePost = { p ->
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { api.deletePost(p.id) } }
                                    .onFailure { feedError = it.message }
                                loadFeed(true)
                            }
                        },
                        onReportPost = { p, reason ->
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { api.reportPost(p.id, reason) } }
                                    .onFailure { feedError = it.message }
                            }
                        },
                        onSearchCheckin = { term -> withContext(Dispatchers.IO) { api.geocodePlaces(term) } },
                        onSearchMentions = { term -> withContext(Dispatchers.IO) { api.users(term).first } },
                        onCreatePost = { text, privacy, feeling, checkin, checkinLat, checkinLng, photoUri, videoUri ->
                            scope.launch {
                                try {
                                    feedError = null
                                    withContext(Dispatchers.IO) {
                                        val photo = photoUri?.let { api.uploadUri(it, "post-image") }.orEmpty()
                                        val video = videoUri?.let { api.uploadUri(it, "post-video") }.orEmpty()
                                        api.createPost(
                                            text = text,
                                            privacy = privacy,
                                            checkin = checkin,
                                            feeling = feeling,
                                            photo = photo,
                                            video = video,
                                            checkinLat = checkinLat,
                                            checkinLng = checkinLng
                                        )
                                    }
                                    loadFeed(true)
                                } catch (e: Exception) { feedError = e.message ?: "Post could not be published." }
                            }
                        },
                        onLoadMore = { loadFeed(false) }
                    )
                    Screen.PEOPLE -> currentUser?.let { u -> PeopleScreen(u.id, people, peopleLoading, peopleError, peopleQuery, { peopleQuery = it }, ::loadPeople, { open(Screen.PROFILE, it) }, { id -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.followUser(id) } }; loadPeople() } }) }
                    Screen.SHOPS -> currentUser?.let { u ->
                        ShopsScreen(
                            meId = u.id,
                            shops = shops,
                            loading = shopsLoading,
                            error = shopsError,
                            query = shopQuery,
                            onQuery = { shopQuery = it },
                            onSearch = ::loadShops,
                            onOpen = { open(Screen.SHOP_DETAIL, it) },
                            onFollow = { id ->
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { api.toggleShopFollow(id) } }
                                        .onFailure { shopsError = it.message }
                                    loadShops()
                                }
                            },
                            onCreate = { fields, photoUri, coverUri ->
                                scope.launch {
                                    try {
                                        shopsError = null
                                        val body = JSONObject(fields.toString())
                                        withContext(Dispatchers.IO) {
                                            photoUri?.let { body.put("photo", api.uploadUri(it, "shop")) }
                                            coverUri?.let { body.put("cover_photo", api.uploadUri(it, "shop-cover")) }
                                            api.createShop(body)
                                        }
                                        loadShops()
                                    } catch (e: Exception) {
                                        shopsError = e.message ?: "Shop could not be created."
                                    }
                                }
                            }
                        )
                    }
                    Screen.MESSAGES -> MessagesScreen(
                        conversations = conversations,
                        groups = messageGroups,
                        loading = conversationsLoading,
                        error = conversationsError,
                        onOpen = { open(Screen.CHAT, it) },
                        onOpenGroup = { open(Screen.GROUP_CHAT, it) },
                        onCreateGroup = { name, usernames ->
                            scope.launch {
                                try {
                                    conversationsError = null
                                    val id = withContext(Dispatchers.IO) { api.createMessageGroup(name, usernames) }
                                    loadMessages()
                                    if (id > 0) open(Screen.GROUP_CHAT, id)
                                } catch (e: Exception) {
                                    conversationsError = e.message ?: "Group could not be created."
                                }
                            }
                        }
                    )
                    Screen.CHAT -> currentUser?.let { u ->
                        ChatScreen(
                            me = u,
                            other = chatOther,
                            messages = chatMessages,
                            loading = chatLoading,
                            error = chatError,
                            onSearchLocation = { term -> withContext(Dispatchers.IO) { api.geocodePlaces(term) } },
                            onSend = { txt, photoUri, audioFile, place ->
                                scope.launch {
                                    try {
                                        chatError = null
                                        val m = withContext(Dispatchers.IO) {
                                            val photo = photoUri?.let { api.uploadUri(it, "message-image") }.orEmpty()
                                            val audio = audioFile?.let { api.upload(it, "audio/mp4", "message-audio") }.orEmpty()
                                            api.sendMessage(
                                                to = selectedId,
                                                text = txt,
                                                photo = photo,
                                                audio = audio,
                                                locationLat = place?.lat,
                                                locationLng = place?.lng
                                            )
                                        }
                                        chatMessages = chatMessages + m
                                    } catch (e: Exception) {
                                        chatError = e.message ?: "Message could not be sent."
                                    }
                                }
                            }
                        )
                    }
                    Screen.GROUP_CHAT -> currentUser?.let { u ->
                        ChatScreen(
                            me = u,
                            other = null,
                            messages = groupChatMessages,
                            loading = groupChatLoading,
                            error = groupChatError,
                            onSearchLocation = { term -> withContext(Dispatchers.IO) { api.geocodePlaces(term) } },
                            onSend = { txt, photoUri, audioFile, place ->
                                scope.launch {
                                    try {
                                        groupChatError = null
                                        val m = withContext(Dispatchers.IO) {
                                            val photo = photoUri?.let { api.uploadUri(it, "message-image") }.orEmpty()
                                            val audio = audioFile?.let { api.upload(it, "audio/mp4", "message-audio") }.orEmpty()
                                            api.sendGroupMessage(
                                                groupId = selectedId,
                                                text = txt,
                                                photo = photo,
                                                audio = audio,
                                                locationLat = place?.lat,
                                                locationLng = place?.lng
                                            )
                                        }
                                        groupChatMessages = groupChatMessages + m
                                    } catch (e: Exception) {
                                        groupChatError = e.message ?: "Message could not be sent."
                                    }
                                }
                            },
                            headerTitle = groupChatName,
                            headerSubtitle = "Chhachh community conversation"
                        )
                    }
                    Screen.NOTIFICATIONS -> NotificationsScreen(notices, noticesLoading, noticesError, { scope.launch { runCatching { withContext(Dispatchers.IO) { api.markNotificationsRead() } }; unread = 0; loadNotices() } }, { open(Screen.PROFILE, it) })
                    Screen.ANNOUNCEMENTS -> AnnouncementsScreen(
                        items = announcements,
                        loading = announcementsLoading,
                        error = announcementsError,
                        onLike = { id ->
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { api.toggleAnnouncementLike(id) } }
                                loadAnnouncements()
                            }
                        },
                        onLoadComments = { id -> withContext(Dispatchers.IO) { api.announcementComments(id) } },
                        onAddComment = { id, text ->
                            withContext(Dispatchers.IO) { api.addAnnouncementComment(id, text) }
                            loadAnnouncements()
                        },
                        onPublish = { text, photoUri, audioFile ->
                            scope.launch {
                                try {
                                    announcementsError = null
                                    withContext(Dispatchers.IO) {
                                        val photo = photoUri?.let { api.uploadUri(it, "announcement-image") }.orEmpty()
                                        val audio = audioFile?.let { api.upload(it, "audio/mp4", "announcement-audio") }.orEmpty()
                                        api.createAnnouncement(text = text, photo = photo, audio = audio)
                                    }
                                    loadAnnouncements()
                                } catch (e: Exception) {
                                    announcementsError = e.message ?: "Announcement could not be published."
                                }
                            }
                        }
                    )
                    Screen.VOTES -> {
                        val u = currentUser
                        if (u == null) {
                            VotesScreen(
                                items = votes,
                                loading = votesLoading,
                                error = votesError,
                                meId = 0L,
                                onSearchOpponent = { emptyList() },
                                onCreateChallenge = { _, _, _ -> authMode = "login"; route = Screen.AUTH },
                                onCast = { _, _ -> authMode = "login"; route = Screen.AUTH },
                                onRespond = { _, _ -> authMode = "login"; route = Screen.AUTH },
                                onStart = { authMode = "login"; route = Screen.AUTH },
                                onCancel = { authMode = "login"; route = Screen.AUTH },
                                onLeave = { authMode = "login"; route = Screen.AUTH },
                                onShare = { authMode = "login"; route = Screen.AUTH },
                                onStatement = { _, _ -> authMode = "login"; route = Screen.AUTH }
                            )
                        } else {
                            VotesScreen(
                                items = votes,
                                loading = votesLoading,
                                error = votesError,
                                meId = u.id,
                                onSearchOpponent = { term -> withContext(Dispatchers.IO) { api.users(term).first } },
                                onCreateChallenge = { username, hours, line ->
                                    scope.launch {
                                        try {
                                            votesError = null
                                            withContext(Dispatchers.IO) { api.createVote(username, hours, line) }
                                        } catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                },
                                onCast = { id, choiceUserId ->
                                    scope.launch {
                                        try {
                                            votesError = null
                                            withContext(Dispatchers.IO) { api.castVote(id, choiceUserId) }
                                        } catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                },
                                onRespond = { id, decision ->
                                    scope.launch {
                                        try {
                                            votesError = null
                                            withContext(Dispatchers.IO) { api.respondVote(id, decision) }
                                        } catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                },
                                onStart = { id ->
                                    scope.launch {
                                        try { withContext(Dispatchers.IO) { api.startVote(id) } }
                                        catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                },
                                onCancel = { id ->
                                    scope.launch {
                                        try { withContext(Dispatchers.IO) { api.cancelVote(id) } }
                                        catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                },
                                onLeave = { id ->
                                    scope.launch {
                                        try { withContext(Dispatchers.IO) { api.leaveVote(id) } }
                                        catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                },
                                onShare = { id ->
                                    scope.launch {
                                        try { withContext(Dispatchers.IO) { api.shareVote(id) } }
                                        catch (e: Exception) { votesError = e.message }
                                    }
                                },
                                onStatement = { id, text ->
                                    scope.launch {
                                        try { withContext(Dispatchers.IO) { api.updateVoteStatement(id, text) } }
                                        catch (e: Exception) { votesError = e.message }
                                        loadVotes()
                                    }
                                }
                            )
                        }
                    }
                    Screen.SAVED -> SavedScreen(saved, savedLoading, savedError, { open(Screen.PROFILE, it) }, { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p) } }; loadSaved() } }, { openDiscussion(it) }, ::sharePost, { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadSaved() } })
                    Screen.SEARCH -> SearchScreen(searchResult, searchLoading, searchError, searchQuery, { searchQuery = it }, ::doSearch, { if (currentUser != null) open(Screen.PROFILE, it) }, { if (currentUser != null) open(Screen.SHOP_DETAIL, it) })
                    Screen.PROFILE -> currentUser?.let { u ->
                        ProfileScreen(
                            data = profileData,
                            loading = profileLoading,
                            error = profileError,
                            meId = u.id,
                            onFollow = { id ->
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { api.followUser(id) } }
                                        .onFailure { profileError = it.message }
                                    loadProfile(id)
                                }
                            },
                            onMessage = { id -> open(Screen.CHAT, id) },
                            onEdit = { open(Screen.SETTINGS) },
                            onShop = { id -> open(Screen.SHOP_DETAIL, id) },
                            onBlock = { id ->
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { api.toggleBlockUser(id) } }
                                        .onFailure { profileError = it.message }
                                    loadProfile(id)
                                }
                            },
                            onReport = { id, reason ->
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { api.reportProfile(id, reason) } }
                                        .onFailure { profileError = it.message }
                                }
                            },
                            onLoadRelations = { id, mode -> withContext(Dispatchers.IO) { api.relationUsers(id, mode) } },
                            onLike = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p) } }; loadProfile(selectedId) } },
                            onComment = { openDiscussion(it) },
                            onShare = ::sharePost,
                            onSave = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadProfile(selectedId) } }
                        )
                    }
                    Screen.SHOP_DETAIL -> currentUser?.let { u ->
                        ShopDetailScreen(
                            data = shopData,
                            loading = shopDetailLoading,
                            error = shopDetailError,
                            meId = u.id,
                            onFollow = { id ->
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { api.toggleShopFollow(id) } }
                                        .onFailure { shopDetailError = it.message }
                                    loadShop(id)
                                }
                            },
                            onProfile = { open(Screen.PROFILE, it) },
                            onMessage = { open(Screen.CHAT, it) },
                            onUpdate = { id, fields, photoUri, coverUri ->
                                scope.launch {
                                    try {
                                        shopDetailError = null
                                        val body = JSONObject(fields.toString())
                                        withContext(Dispatchers.IO) {
                                            photoUri?.let { body.put("photo", api.uploadUri(it, "shop")) }
                                            coverUri?.let { body.put("cover_photo", api.uploadUri(it, "shop-cover")) }
                                            api.updateShop(id, body)
                                        }
                                        loadShop(id)
                                        loadShops()
                                    } catch (e: Exception) {
                                        shopDetailError = e.message ?: "Shop could not be updated."
                                    }
                                }
                            },
                            onDelete = { id ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { api.deleteShop(id) }
                                        route = Screen.SHOPS
                                        selectedId = 0L
                                        backStack.clear()
                                        loadShops()
                                    } catch (e: Exception) {
                                        shopDetailError = e.message ?: "Shop could not be deleted."
                                    }
                                }
                            },
                            onCreatePost = { id, text, privacy, photoUri, videoUri ->
                                scope.launch {
                                    try {
                                        shopDetailError = null
                                        withContext(Dispatchers.IO) {
                                            val photo = photoUri?.let { api.uploadUri(it, "shop-post-image") }.orEmpty()
                                            val video = videoUri?.let { api.uploadUri(it, "shop-post-video") }.orEmpty()
                                            api.createShopPost(id, text, privacy, photo, video)
                                        }
                                        loadShop(id)
                                    } catch (e: Exception) {
                                        shopDetailError = e.message ?: "Shop post could not be published."
                                    }
                                }
                            },
                            onLike = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p) } }; loadShop(selectedId) } },
                            onComment = { openDiscussion(it) },
                            onShare = ::sharePost,
                            onSave = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadShop(selectedId) } }
                        )
                    }
                    Screen.MAP -> MapScreen(mapQuery, { mapQuery = it }, mapData, mapLoading, mapError, {
                        if (mapQuery.isNotBlank()) scope.launch { mapLoading = true; mapError = null; try { val d = withContext(Dispatchers.IO) { api.geocode(mapQuery) }; mapData = d.optJSONArray("items")?.optJSONObject(0) ?: d.optJSONObject("item") ?: d } catch (e: Exception) { mapError = e.message }; mapLoading = false }
                    })
                    Screen.SETTINGS -> currentUser?.let { u ->
                        SettingsScreen(
                            me = u,
                            busy = settingsBusy,
                            error = settingsError,
                            blockedUsers = blockedUsers,
                            verification = verificationState,
                            supportTickets = supportTickets,
                            onSave = { fields, avatarUri ->
                                scope.launch {
                                    settingsBusy = true
                                    settingsError = null
                                    try {
                                        val updatedFields = JSONObject(fields.toString())
                                        if (avatarUri != null) {
                                            val avatar = withContext(Dispatchers.IO) { api.uploadUri(avatarUri, "profile-avatar") }
                                            if (avatar.isNotBlank()) updatedFields.put("avatar", avatar)
                                        }
                                        me = withContext(Dispatchers.IO) { api.updateProfile(updatedFields) }
                                    } catch (e: Exception) {
                                        settingsError = e.message
                                    }
                                    settingsBusy = false
                                }
                            },
                            onPrivacy = { fields ->
                                scope.launch {
                                    settingsBusy = true
                                    settingsError = null
                                    try { me = withContext(Dispatchers.IO) { api.updatePrivacy(fields) } }
                                    catch (e: Exception) { settingsError = e.message }
                                    settingsBusy = false
                                }
                            },
                            onLocation = { lat, lng ->
                                scope.launch {
                                    try { withContext(Dispatchers.IO) { api.updateLocation(lat, lng) } }
                                    catch (e: Exception) { settingsError = e.message }
                                }
                            },
                            onPassword = { current, next ->
                                scope.launch {
                                    try { withContext(Dispatchers.IO) { api.changePassword(current, next) } }
                                    catch (e: Exception) { settingsError = e.message }
                                }
                            },
                            onSubmitVerification = { phone, type, frontUri, backUri, selfieUri ->
                                scope.launch {
                                    settingsBusy = true
                                    settingsError = null
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val front = api.uploadUri(frontUri, "id-front")
                                            val back = backUri?.let { api.uploadUri(it, "id-back") }.orEmpty()
                                            val selfie = api.uploadUri(selfieUri, "selfie")
                                            api.submitVerification(phone, type, front, back, selfie)
                                        }
                                        verificationState = withContext(Dispatchers.IO) { api.verificationStatus() }
                                        me = withContext(Dispatchers.IO) { api.profileMe() }
                                    } catch (e: Exception) {
                                        settingsError = e.message ?: "Verification could not be submitted."
                                    }
                                    settingsBusy = false
                                }
                            },
                            onRefreshBlocked = ::loadBlockedUsers,
                            onUnblock = { id ->
                                scope.launch {
                                    try { withContext(Dispatchers.IO) { api.toggleBlockUser(id) } }
                                    catch (e: Exception) { settingsError = e.message }
                                    loadBlockedUsers()
                                }
                            },
                            onSubmitSupport = { category, subject, message ->
                                scope.launch {
                                    settingsBusy = true
                                    settingsError = null
                                    try {
                                        withContext(Dispatchers.IO) { api.submitSupport(category, subject, message) }
                                        supportTickets = withContext(Dispatchers.IO) { api.supportTickets() }
                                    } catch (e: Exception) {
                                        settingsError = e.message ?: "Support request could not be sent."
                                    }
                                    settingsBusy = false
                                }
                            },
                            onDeleteAccount = { password ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { api.deleteAccount(password) }
                                        api.clearSession()
                                        me = null
                                        route = Screen.HOME
                                        selectedId = 0L
                                        backStack.clear()
                                        feedMode = "global"
                                        loadFeed(true)
                                    } catch (e: Exception) {
                                        settingsError = e.message
                                    }
                                }
                            },
                            onLogout = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { api.logout() }
                                    me = null
                                    route = Screen.HOME
                                    selectedId = 0L
                                    backStack.clear()
                                    feedMode = "global"
                                    loadFeed(true)
                                }
                            }
                        )
                    }
                    Screen.WEATHER -> WeatherScreen(weatherData, weatherLoading, weatherError, ::loadWeather)
                    Screen.ADMIN -> if (currentUser?.isAdmin == true) {
                        AdminCenterScreen(
                            state = adminState,
                            list = adminList,
                            section = adminSection,
                            query = adminQuery,
                            loading = adminLoading,
                            error = adminError,
                            onSection = {
                                adminSection = it
                                scope.launch {
                                    adminLoading = true
                                    adminError = null
                                    try { adminList = withContext(Dispatchers.IO) { api.adminList(it, query = adminQuery) } }
                                    catch (e: Exception) { adminError = e.message }
                                    adminLoading = false
                                }
                            },
                            onQuery = { adminQuery = it },
                            onRefresh = { loadAdmin(true) },
                            onAction = { action, id, fields ->
                                scope.launch {
                                    adminLoading = true
                                    adminError = null
                                    try {
                                        withContext(Dispatchers.IO) { api.adminAction(action, id, fields) }
                                        val result = withContext(Dispatchers.IO) {
                                            api.adminState() to api.adminList(adminSection, query = adminQuery)
                                        }
                                        adminState = result.first
                                        adminList = result.second
                                    } catch (e: Exception) {
                                        adminError = e.message ?: "Admin action failed."
                                    }
                                    adminLoading = false
                                }
                            },
                            onProfile = { open(Screen.PROFILE, it) }
                        )
                    }
                    Screen.THEME -> if (currentUser?.isAdmin == true) {
                        NativeThemeScreen(
                            settings = adminState?.optJSONObject("settings") ?: features,
                            busy = themeBusy,
                            error = themeError,
                            onSaveTheme = { fields ->
                                scope.launch {
                                    themeBusy = true
                                    themeError = null
                                    try {
                                        val d = withContext(Dispatchers.IO) { api.saveTheme(fields) }
                                        d.optJSONObject("settings")?.let(::applyLiveSettings)
                                    } catch (e: Exception) {
                                        themeError = e.message ?: "Theme could not be saved."
                                    }
                                    themeBusy = false
                                }
                            },
                            onSaveBrand = { fields, iconUri ->
                                scope.launch {
                                    themeBusy = true
                                    themeError = null
                                    try {
                                        val body = JSONObject(fields.toString())
                                        withContext(Dispatchers.IO) {
                                            iconUri?.let {
                                                val icon = api.uploadUri(it, "site-brand-icon")
                                                if (icon.isNotBlank()) body.put("site_icon", icon)
                                            }
                                            val d = api.saveBranding(body)
                                            d.optJSONObject("settings")?.let(::applyLiveSettings)
                                        }
                                    } catch (e: Exception) {
                                        themeError = e.message ?: "Branding could not be saved."
                                    }
                                    themeBusy = false
                                }
                            }
                        )
                    }
                }
            }

        }

        if (menuOpen && currentUser != null) {
            SideMenu(currentUser, unread, announcementUnread, settings = features, onClose = { menuOpen = false }, onOpen = { if (it == Screen.PROFILE) open(it, currentUser.id) else open(it) }, onLogout = {
                scope.launch { withContext(Dispatchers.IO) { api.logout() }; me = null; menuOpen = false; route = Screen.HOME; selectedId = 0L; backStack.clear(); feedMode = "global"; loadFeed(true) }
            })
        }
    }

    commentPost?.let { post ->
        PostDiscussionDialog(
            post = post,
            comments = discussionComments,
            likesUsers = discussionLikes,
            loading = discussionLoading,
            error = discussionError,
            busy = commentBusy,
            onDismiss = { if (!commentBusy) commentPost = null },
            onSend = { text, parentId ->
                scope.launch {
                    commentBusy = true
                    discussionError = null
                    try {
                        withContext(Dispatchers.IO) { api.addComment(post, text, parentId) }
                        discussionComments = withContext(Dispatchers.IO) { api.postComments(post).comments() }
                        when (route) {
                            Screen.HOME -> loadFeed(true)
                            Screen.SAVED -> loadSaved()
                            Screen.PROFILE -> loadProfile(selectedId)
                            Screen.SHOP_DETAIL -> loadShop(selectedId)
                            else -> Unit
                        }
                    } catch (e: Exception) {
                        discussionError = e.message ?: "Comment could not be sent."
                    }
                    commentBusy = false
                }
            },
            onLikeComment = { commentId ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { api.likeComment(commentId) }
                        discussionComments = withContext(Dispatchers.IO) { api.postComments(post).comments() }
                    } catch (e: Exception) {
                        discussionError = e.message ?: "Comment like failed."
                    }
                }
            },
            onProfile = { id -> open(Screen.PROFILE, id) }
        )
    }
}

@Composable
private fun GuestHeader(
    brandName: String,
    brandTagline: String,
    brandIcon: String?,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp)) {
        JellyGlass(
            Modifier.fillMaxWidth(),
            radius = LiveJellyTheme.headerRadius.dp,
            padding = 10.dp,
            surfaceColor = LiveJellyTheme.headerColor,
            surfaceOpacity = LiveJellyTheme.headerOpacity
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HeaderBrand(brandName, brandTagline, brandIcon, Modifier.weight(1f), 28)
                JellyButton("Login", onClick = onLogin)
                Spacer(Modifier.width(6.dp))
                JellyButton("Sign up", primary = true, onClick = onRegister)
            }
        }
    }
}

@Composable
private fun AuthHeader(
    user: User,
    brandName: String,
    brandTagline: String,
    brandIcon: String?,
    route: Screen,
    unread: Int,
    announcementUnread: Int,
    weatherText: String,
    onMenu: () -> Unit, onSearch: () -> Unit, onNotifications: () -> Unit, onProfile: () -> Unit,
    onWeather: () -> Unit, onVotes: () -> Unit, onAnnouncements: () -> Unit,
    settings: JSONObject,
    onNav: (Screen) -> Unit
) {
    val primary = route == Screen.HOME || route == Screen.PEOPLE || route == Screen.SHOPS || route == Screen.MAP || route == Screen.MESSAGES

    if (!primary) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JellyIconButton(JellyIcons.Arrow, "Back", onClick = onMenu)
            Spacer(Modifier.width(6.dp))
            Text(routeTitle(route), Modifier.weight(1f), color = JellyInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
            if (route != Screen.SEARCH) JellyIconButton(JellyIcons.Search, "Search", onClick = onSearch)
            if (route != Screen.NOTIFICATIONS) JellyIconButton(JellyIcons.Bell, "Notifications", badge = unread, onClick = onNotifications)
        }
        return
    }

    val navOrder = settings.optString("theme_header_items", "home,people,shop,map,messages")
        .split(",").map { it.trim() }.filter { it.isNotBlank() }
    val votingEnabled = settings.optInt("voting", 1) != 0

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = LiveJellyTheme.headerRadius.dp, bottomEnd = LiveJellyTheme.headerRadius.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFDDF7FF).copy(alpha = .88f),
                        Color(0xFFE4F4FF).copy(alpha = .73f),
                        Color(0xFFEEEAFF).copy(alpha = .68f)
                    )
                )
            )
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clickable { onMenu() },
                contentAlignment = Alignment.Center
            ) {
                JellyIcon(JellyIcons.Menu, size = 39.dp, contentDescription = "Menu")
            }

            HeaderBrand(
                name = brandName,
                tagline = brandTagline,
                iconUrl = brandIcon,
                modifier = Modifier.weight(1f),
                fontSize = 30
            )

            Box(Modifier.size(45.dp).clickable { onNotifications() }, contentAlignment = Alignment.Center) {
                JellyIcon(JellyIcons.Bell, size = 40.dp, contentDescription = "Notifications")
                if (unread > 0) CountBadge(unread, Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.width(5.dp))
            Box(Modifier.size(47.dp).clickable { onProfile() }, contentAlignment = Alignment.Center) {
                Avatar(user, 45.dp)
            }
        }

        JellyGlass(
            Modifier.fillMaxWidth().height(54.dp),
            radius = 999.dp,
            padding = 9.dp,
            onClick = onSearch,
            surfaceColor = LiveJellyTheme.inputColor,
            surfaceOpacity = LiveJellyTheme.inputOpacity
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                JellyIcon(JellyIcons.Search, size = 32.dp, contentDescription = "Search")
                Spacer(Modifier.width(5.dp))
                Text(
                    "Search people, posts, places…",
                    Modifier.weight(1f),
                    color = Color(0xFF858EB1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                JellyIcon(JellyIcons.Filter, size = 32.dp, contentDescription = "Filters")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (votingEnabled) {
                QuickHeader("Voting", JellyIcons.Vote, Modifier.weight(1f), onVotes)
            } else {
                Spacer(Modifier.weight(1f))
            }
            QuickHeader(weatherText, JellyIcons.Weather, Modifier.weight(1f), onWeather)
            Box(Modifier.weight(1f)) {
                QuickHeader("Announcements", JellyIcons.Announcement, Modifier.fillMaxWidth(), onAnnouncements)
                if (announcementUnread > 0) CountBadge(announcementUnread, Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
        }

        JellyGlass(
            Modifier.fillMaxWidth(),
            radius = LiveJellyTheme.navRadius.dp,
            padding = 4.dp,
            surfaceColor = LiveJellyTheme.navColor,
            surfaceOpacity = LiveJellyTheme.navOpacity
        ) {
            Row(Modifier.fillMaxWidth()) {
                navOrder.forEach { key ->
                    when (key) {
                        "home" -> NavItem("Home", JellyIcons.Home, route == Screen.HOME, Modifier.weight(1f)) { onNav(Screen.HOME) }
                        "people" -> NavItem("People", JellyIcons.People, route == Screen.PEOPLE, Modifier.weight(1f)) { onNav(Screen.PEOPLE) }
                        "shop" -> NavItem("Shop", JellyIcons.Shop, route == Screen.SHOPS, Modifier.weight(1f)) { onNav(Screen.SHOPS) }
                        "map" -> NavItem("Map", JellyIcons.Map, route == Screen.MAP, Modifier.weight(1f)) { onNav(Screen.MAP) }
                        "messages" -> NavItem("Messages", JellyIcons.Message, route == Screen.MESSAGES, Modifier.weight(1f)) { onNav(Screen.MESSAGES) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBrand(
    name: String,
    tagline: String,
    iconUrl: String?,
    modifier: Modifier = Modifier,
    fontSize: Int = 30
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Brand(name = name.ifBlank { "My Chhachh" }, fontSize = fontSize)
            if (tagline.isNotBlank()) {
                Text(
                    tagline,
                    color = JellyMuted,
                    fontSize = 8.2f.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = .1.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun routeTitle(route: Screen): String = when (route) {
    Screen.NOTIFICATIONS -> "Notifications"
    Screen.ANNOUNCEMENTS -> "Announcements"
    Screen.VOTES -> "Voting"
    Screen.SAVED -> "Saved"
    Screen.SEARCH -> "Search"
    Screen.PROFILE -> "Profile"
    Screen.SHOP_DETAIL -> "Shop"
    Screen.CHAT -> "Chat"
    Screen.GROUP_CHAT -> "Group"
    Screen.SETTINGS -> "Settings"
    Screen.WEATHER -> "Weather"
    Screen.THEME -> "Theme"
    Screen.ADMIN -> "Admin Center"
    else -> "My Chhachh"
}

@Composable
private fun QuickHeader(text: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    JellyGlass(modifier.height(82.dp), radius = 21.dp, padding = 5.dp, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            JellyIcon(icon, size = if (icon == JellyIcons.Weather) 37.dp else 34.dp)
            Spacer(Modifier.height(3.dp))
            Text(
                text,
                color = Color(0xFF433476),
                fontWeight = FontWeight.Black,
                fontSize = 8.8f.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NavItem(text: String, icon: Int, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .background(
                if (active) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFFF0F9).copy(alpha = .68f),
                            Color.White.copy(alpha = .45f)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JellyIcon(icon, size = 32.dp, contentDescription = text)
        Spacer(Modifier.height(2.dp))
        Text(
            text,
            color = Color(0xFF392B72),
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            maxLines = 1
        )
        if (active) {
            Box(
                Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF67BA), Color(0xFFFF43AA))
                        )
                    )
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(999.dp)).background(JellyPink).padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(if (count > 99) "99+" else count.toString(), color = Color.White, fontSize = 7.5f.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SideMenu(
    user: User,
    unread: Int,
    announcementUnread: Int,
    settings: JSONObject,
    onClose: () -> Unit,
    onOpen: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    val order = settings.optString("theme_menu_items", "votes,saved,settings,theme,admin,logout")
        .split(",").map { it.trim() }.filter { it.isNotBlank() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x3D0A203A))
            .clickable { onClose() }
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(.88f)
                .widthIn(max = 350.dp)
                .clickable(enabled = false) {}
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF0FBFF).copy(alpha = .96f),
                            Color(0xFFE8F0FF).copy(alpha = .93f),
                            Color(0xFFFDEBF9).copy(alpha = .91f)
                        )
                    )
                )
                .border(2.dp, Color.White, RoundedCornerShape(0.dp))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            JellyIconButton(
                JellyIcons.Close,
                "Close",
                Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                onClick = onClose
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 14.dp, top = 58.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                order.forEach { key ->
                    when (key) {
                        "home" -> MenuRow("Home", JellyIcons.Home) { onOpen(Screen.HOME) }
                        "people" -> MenuRow("People", JellyIcons.People) { onOpen(Screen.PEOPLE) }
                        "shop" -> MenuRow("Shop", JellyIcons.Shop) { onOpen(Screen.SHOPS) }
                        "votes" -> if (settings.optInt("voting", 1) != 0) MenuRow("Voting", JellyIcons.Vote) { onOpen(Screen.VOTES) }
                        "saved" -> MenuRow("Saved", JellyIcons.Save) { onOpen(Screen.SAVED) }
                        "map" -> MenuRow("Chhachh Map", JellyIcons.Map) { onOpen(Screen.MAP) }
                        "messages" -> if (settings.optInt("messaging", 1) != 0) MenuRow("Messages", JellyIcons.Message) { onOpen(Screen.MESSAGES) }
                        "announcements" -> MenuRow(
                            if (announcementUnread > 0) "Announcements ($announcementUnread)" else "Announcements",
                            JellyIcons.Announcement
                        ) { onOpen(Screen.ANNOUNCEMENTS) }
                        "notifications" -> MenuRow(
                            if (unread > 0) "Notifications ($unread)" else "Notifications",
                            JellyIcons.Bell
                        ) { onOpen(Screen.NOTIFICATIONS) }
                        "profile" -> MenuRow("Profile", JellyIcons.User) { onOpen(Screen.PROFILE) }
                        "settings" -> MenuRow("Settings", JellyIcons.Gear) { onOpen(Screen.SETTINGS) }
                        "theme" -> if (user.isAdmin && settings.optInt("theme_theme_icon_enabled", 1) != 0) {
                            MenuRow("Theme Builder", JellyIcons.Palette) { onOpen(Screen.THEME) }
                        }
                        "admin" -> if (user.isAdmin) MenuRow("Admin Center", JellyIcons.Shield) { onOpen(Screen.ADMIN) }
                        "logout" -> {
                            Spacer(Modifier.weight(1f))
                            MenuRow("Logout", JellyIcons.Logout, danger = true, onClick = onLogout)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, icon: Int, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .heightIn(min = 46.dp)
            .padding(horizontal = 7.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JellyIcon(icon, size = 34.dp, contentDescription = text)
        Spacer(Modifier.width(13.dp))
        Text(
            text,
            color = if (danger) JellyDanger else Color(0xFF3B2D72),
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}
