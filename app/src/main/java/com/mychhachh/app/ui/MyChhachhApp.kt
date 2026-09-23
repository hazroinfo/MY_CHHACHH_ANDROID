package com.mychhachh.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
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

enum class Screen { HOME, PEOPLE, SHOPS, MAP, MESSAGES, NOTIFICATIONS, ANNOUNCEMENTS, VOTES, SAVED, SEARCH, PROFILE, SHOP_DETAIL, CHAT, SETTINGS, WEATHER, AUTH }

@Composable
fun MyChhachhApp() {
    val context = LocalContext.current
    val api = remember { ApiClient(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var booting by remember { mutableStateOf(true) }
    var me by remember { mutableStateOf<User?>(null) }
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
    var conversationsLoading by remember { mutableStateOf(false) }
    var conversationsError by remember { mutableStateOf<String?>(null) }
    var chatOther by remember { mutableStateOf<User?>(null) }
    var chatMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var chatLoading by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }

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

    var commentPost by remember { mutableStateOf<Post?>(null) }
    var commentText by remember { mutableStateOf("") }
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
        scope.launch { conversationsLoading = true; conversationsError = null; try { conversations = withContext(Dispatchers.IO) { api.conversations() } } catch (e: Exception) { conversationsError = e.message }; conversationsLoading = false }
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
    fun loadWeather() {
        scope.launch { weatherLoading = true; weatherError = null; try { weatherData = withContext(Dispatchers.IO) { api.weather() } } catch (e: Exception) { weatherError = e.message }; weatherLoading = false }
    }

    fun sharePost(post: Post) {
        scope.launch { runCatching { withContext(Dispatchers.IO) { api.sharePost(post.id) } } }
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://chhachh.pages.dev/post.php?id=${post.id}")
        runCatching { context.startActivity(Intent.createChooser(share, "Share post")) }
    }

    LaunchedEffect(Unit) {
        try {
            val b = withContext(Dispatchers.IO) { api.bootstrap() }
            me = b.user; unread = b.unread; announcementUnread = b.announcementUnread
        } catch (_: Exception) { me = null }
        booting = false
        loadFeed(true)
        loadWeather()
    }

    LaunchedEffect(feedMode) { if (!booting) loadFeed(true) }
    LaunchedEffect(route, selectedId, me?.id) {
        if (booting) return@LaunchedEffect
        when (route) {
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
            Screen.WEATHER -> loadWeather()
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
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFDFF7FF), Color(0xFFF5F0FF), Color(0xFFE7F8FF))))) {
        Column(Modifier.fillMaxSize()) {
            if (route != Screen.AUTH) {
                if (currentUser == null) GuestHeader(
                    onLogin = { authMode = "login"; authError = null; route = Screen.AUTH },
                    onRegister = { authMode = "register"; authError = null; route = Screen.AUTH }
                ) else AuthHeader(
                    user = currentUser,
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
                                    unread = b.unread; announcementUnread = b.announcementUnread
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
                                    unread = b.unread; announcementUnread = b.announcementUnread
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
                        onMode = { feedMode = it },
                        onLogin = { authMode = "login"; route = Screen.AUTH }, onRegister = { authMode = "register"; route = Screen.AUTH },
                        onProfile = { if (currentUser != null) open(Screen.PROFILE, it) else { authMode = "login"; route = Screen.AUTH } },
                        onLike = { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p.id) } }; loadFeed(true) } },
                        onComment = { p -> commentPost = p },
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
                    Screen.SHOPS -> ShopsScreen(shops, shopsLoading, shopsError, shopQuery, { shopQuery = it }, ::loadShops, { open(Screen.SHOP_DETAIL, it) }, { id -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.toggleShopFollow(id) } }; loadShops() } })
                    Screen.MESSAGES -> MessagesScreen(conversations, conversationsLoading, conversationsError) { open(Screen.CHAT, it) }
                    Screen.CHAT -> currentUser?.let { u -> ChatScreen(u, chatOther, chatMessages, chatLoading, chatError, { txt -> scope.launch { try { val m = withContext(Dispatchers.IO) { api.sendMessage(selectedId, txt) }; chatMessages = chatMessages + m } catch (e: Exception) { chatError = e.message } } }) }
                    Screen.NOTIFICATIONS -> NotificationsScreen(notices, noticesLoading, noticesError, { scope.launch { runCatching { withContext(Dispatchers.IO) { api.markNotificationsRead() } }; unread = 0; loadNotices() } }, { open(Screen.PROFILE, it) })
                    Screen.ANNOUNCEMENTS -> AnnouncementsScreen(announcements, announcementsLoading, announcementsError) { id -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.toggleAnnouncementLike(id) } }; loadAnnouncements() } }
                    Screen.VOTES -> VotesScreen(votes, votesLoading, votesError) { id, side -> if (currentUser == null) { authMode = "login"; route = Screen.AUTH } else scope.launch { runCatching { withContext(Dispatchers.IO) { api.castVote(id, side) } }; loadVotes() } }
                    Screen.SAVED -> SavedScreen(saved, savedLoading, savedError, { open(Screen.PROFILE, it) }, { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p.id) } }; loadSaved() } }, { commentPost = it }, ::sharePost, { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadSaved() } })
                    Screen.SEARCH -> SearchScreen(searchResult, searchLoading, searchError, searchQuery, { searchQuery = it }, ::doSearch, { if (currentUser != null) open(Screen.PROFILE, it) }, { if (currentUser != null) open(Screen.SHOP_DETAIL, it) })
                    Screen.PROFILE -> currentUser?.let { u -> ProfileScreen(profileData, profileLoading, profileError, u.id, { id -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.followUser(id) } }; loadProfile(id) } }, { id -> open(Screen.CHAT, id) }, { open(Screen.SETTINGS) }, { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p.id) } }; loadProfile(selectedId) } }, { commentPost = it }, ::sharePost, { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadProfile(selectedId) } }) }
                    Screen.SHOP_DETAIL -> ShopDetailScreen(shopData, shopDetailLoading, shopDetailError,
                        { id -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.toggleShopFollow(id) } }; loadShop(id) } },
                        { open(Screen.PROFILE, it) },
                        { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.likePost(p.id) } }; loadShop(selectedId) } },
                        { commentPost = it },
                        ::sharePost,
                        { p -> scope.launch { runCatching { withContext(Dispatchers.IO) { api.savePost(p.id) } }; loadShop(selectedId) } }
                    )
                    Screen.MAP -> MapScreen(mapQuery, { mapQuery = it }, mapData, mapLoading, mapError, {
                        if (mapQuery.isNotBlank()) scope.launch { mapLoading = true; mapError = null; try { val d = withContext(Dispatchers.IO) { api.geocode(mapQuery) }; mapData = d.optJSONArray("items")?.optJSONObject(0) ?: d.optJSONObject("item") ?: d } catch (e: Exception) { mapError = e.message }; mapLoading = false }
                    })
                    Screen.SETTINGS -> currentUser?.let { u -> SettingsScreen(u, settingsBusy, settingsError, { fields, avatarUri ->
                        scope.launch {
                            settingsBusy = true; settingsError = null
                            try {
                                val updatedFields = JSONObject(fields.toString())
                                if (avatarUri != null) {
                                    val avatar = withContext(Dispatchers.IO) { api.uploadUri(avatarUri, "profile-avatar") }
                                    if (avatar.isNotBlank()) updatedFields.put("avatar", avatar)
                                }
                                me = withContext(Dispatchers.IO) { api.updateProfile(updatedFields) }
                            } catch (e: Exception) { settingsError = e.message }
                            settingsBusy = false
                        }
                    }, { scope.launch { withContext(Dispatchers.IO) { api.logout() }; me = null; route = Screen.HOME; selectedId = 0L; backStack.clear(); feedMode = "global"; loadFeed(true) } }) }
                    Screen.WEATHER -> WeatherScreen(weatherData, weatherLoading, weatherError, ::loadWeather)
                }
            }
        }

        if (menuOpen && currentUser != null) {
            SideMenu(currentUser, unread, announcementUnread, onClose = { menuOpen = false }, onOpen = { if (it == Screen.PROFILE) open(it, currentUser.id) else open(it) }, onLogout = {
                scope.launch { withContext(Dispatchers.IO) { api.logout() }; me = null; menuOpen = false; route = Screen.HOME; selectedId = 0L; backStack.clear(); feedMode = "global"; loadFeed(true) }
            })
        }
    }

    commentPost?.let { post ->
        AlertDialog(
            onDismissRequest = { if (!commentBusy) commentPost = null },
            title = { Text("Comment", color = JellyInk, fontWeight = FontWeight.Black) },
            text = { OutlinedTextField(commentText, { commentText = it }, placeholder = { Text("Write a comment") }, minLines = 3, shape = RoundedCornerShape(18.dp)) },
            confirmButton = {
                JellyButton(if (commentBusy) "Sending…" else "Send", primary = true, icon = JellyIcons.Send) {
                    if (!commentBusy && commentText.isNotBlank()) scope.launch {
                        commentBusy = true
                        try {
                            withContext(Dispatchers.IO) { api.addComment(post.id, commentText.trim()) }
                            commentText = ""; commentPost = null
                            when (route) {
                                Screen.HOME -> loadFeed(true)
                                Screen.SAVED -> loadSaved()
                                Screen.PROFILE -> loadProfile(selectedId)
                                Screen.SHOP_DETAIL -> loadShop(selectedId)
                                else -> Unit
                            }
                        } catch (e: Exception) { feedError = e.message }
                        commentBusy = false
                    }
                }
            },
            dismissButton = { JellyButton("Cancel") { commentPost = null } }
        )
    }
}

@Composable
private fun GuestHeader(onLogin: () -> Unit, onRegister: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp)) {
        JellyGlass(Modifier.fillMaxWidth(), radius = 28.dp, padding = 10.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Brand(fontSize = 28)
                    Text(
                        "PEOPLE · KNOWLEDGE · COMMUNITIES",
                        color = JellyMuted,
                        fontSize = 7.5f.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = .5.sp
                    )
                }
                JellyButton("Login", onClick = onLogin)
                Spacer(Modifier.width(6.dp))
                JellyButton("Sign up", primary = true, onClick = onRegister)
            }
        }
    }
}

@Composable
private fun AuthHeader(
    user: User, route: Screen, unread: Int, announcementUnread: Int, weatherText: String,
    onMenu: () -> Unit, onSearch: () -> Unit, onNotifications: () -> Unit, onProfile: () -> Unit,
    onWeather: () -> Unit, onVotes: () -> Unit, onAnnouncements: () -> Unit,
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

    Box(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp)) {
        JellyGlass(Modifier.fillMaxWidth(), radius = 28.dp, padding = 9.dp) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).clickable { onMenu() },
                        contentAlignment = Alignment.Center
                    ) {
                        JellyIcon(JellyIcons.Menu, size = 38.dp, contentDescription = "Menu")
                    }

                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Brand(fontSize = 30)
                        Text(
                            "Connect with people for information.",
                            color = JellyMuted,
                            fontSize = 8.2f.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = .1.sp,
                            maxLines = 1
                        )
                    }

                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        JellyIconButton(JellyIcons.Bell, "Notifications", badge = unread, onClick = onNotifications)
                    }
                    Spacer(Modifier.width(2.dp))
                    Box(Modifier.clickable { onProfile() }) { Avatar(user, 42.dp) }
                }

                JellyGlass(
                    Modifier.fillMaxWidth().height(58.dp),
                    radius = 999.dp,
                    padding = 8.dp,
                    onClick = onSearch
                ) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(JellyIcons.Search, size = 34.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "Search people, posts, places...",
                            Modifier.weight(1f),
                            color = JellyMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        JellyIcon(JellyIcons.Filter, size = 32.dp)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickHeader("Voting", JellyIcons.Vote, Modifier.weight(1f), onVotes)
                    QuickHeader(weatherText, JellyIcons.Weather, Modifier.weight(1f), onWeather)
                    Box(Modifier.weight(1f)) {
                        QuickHeader("Announcements", JellyIcons.Announcement, Modifier.fillMaxWidth(), onAnnouncements)
                        if (announcementUnread > 0) CountBadge(announcementUnread, Modifier.align(Alignment.TopEnd))
                    }
                }

                JellyGlass(Modifier.fillMaxWidth(), radius = 25.dp, padding = 4.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NavItem("Home", JellyIcons.Home, route == Screen.HOME, Modifier.weight(1f)) { onNav(Screen.HOME) }
                        NavItem("People", JellyIcons.People, route == Screen.PEOPLE, Modifier.weight(1f)) { onNav(Screen.PEOPLE) }
                        NavItem("Shop", JellyIcons.Shop, route == Screen.SHOPS, Modifier.weight(1f)) { onNav(Screen.SHOPS) }
                        NavItem("Map", JellyIcons.Map, route == Screen.MAP, Modifier.weight(1f)) { onNav(Screen.MAP) }
                        NavItem("Messages", JellyIcons.Message, route == Screen.MESSAGES, Modifier.weight(1f)) { onNav(Screen.MESSAGES) }
                    }
                }
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
    Screen.SETTINGS -> "Settings"
    Screen.WEATHER -> "Weather"
    else -> "My Chhachh"
}

@Composable
private fun QuickHeader(text: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    JellyGlass(modifier.height(88.dp), radius = 24.dp, padding = 5.dp, onClick = onClick) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            JellyIcon(icon, size = 40.dp)
            Spacer(Modifier.height(3.dp))
            Text(text, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 9.5f.sp, maxLines = 1)
        }
    }
}

@Composable
private fun NavItem(text: String, icon: Int, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .background(if (active) Color(0x66FFF0F9) else Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JellyIcon(icon, size = 32.dp, contentDescription = text)
        Spacer(Modifier.height(2.dp))
        Text(text, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 9.2f.sp, maxLines = 1)
        if (active) Box(Modifier.width(28.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(JellyPink))
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
    onClose: () -> Unit,
    onOpen: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0x66405070)).clickable { onClose() }) {
        JellyGlass(
            Modifier.fillMaxHeight().widthIn(max = 330.dp).fillMaxWidth(.84f).clickable(enabled = false) {},
            radius = 0.dp,
            padding = 14.dp
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(user, 52.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        UserName(user, 15)
                        Text("@${user.username}", color = JellyMuted, fontSize = 9.5f.sp)
                    }
                    JellyIconButton(JellyIcons.Close, "Close", onClick = onClose)
                }
                Spacer(Modifier.height(6.dp))
                MenuRow("Saved", JellyIcons.Save) { onOpen(Screen.SAVED) }
                MenuRow("Settings", JellyIcons.Gear) { onOpen(Screen.SETTINGS) }
                Spacer(Modifier.weight(1f))
                MenuRow("Logout", JellyIcons.Logout, onLogout)
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JellyIcon(icon, size = 28.dp, contentDescription = text)
        Spacer(Modifier.width(9.dp))
        Text(text, color = JellyInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.5f.sp)
    }
}
