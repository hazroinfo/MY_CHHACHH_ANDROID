package com.mychhachh.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.ApiClient
import com.mychhachh.app.data.CheckinPlace
import com.mychhachh.app.data.Comment
import com.mychhachh.app.data.Post
import com.mychhachh.app.data.Shop
import com.mychhachh.app.data.User
import com.mychhachh.app.data.Vote
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.*

@Composable
fun HomeScreen(
    user: User?,
    mode: String,
    posts: List<Post>,
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    peopleSuggestions: List<User>,
    shopSuggestions: List<Shop>,
    voteHighlight: Vote?,
    onMode: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onProfile: (Long) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenShop: (Long) -> Unit,
    onOpenVote: (Long) -> Unit,
    onFollowSuggestion: (Long) -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onEditPost: (Post, String, String) -> Unit,
    onDeletePost: (Post) -> Unit,
    onReportPost: (Post, String) -> Unit,
    onSearchCheckin: suspend (String) -> List<CheckinPlace>,
    onSearchMentions: suspend (String) -> List<User>,
    onCreatePost: (String, String, String, String, Double?, Double?, Uri?, Uri?) -> Unit,
    onLoadMore: () -> Unit
) {
    var composing by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("public") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var feeling by remember { mutableStateOf("") }
    var checkin by remember { mutableStateOf("") }
    var checkinLat by remember { mutableStateOf<Double?>(null) }
    var checkinLng by remember { mutableStateOf<Double?>(null) }
    var feelingDialog by remember { mutableStateOf(false) }
    var checkinDialog by remember { mutableStateOf(false) }
    var mentionDialog by remember { mutableStateOf(false) }
    var checkinLocationStatus by remember { mutableStateOf<String?>(null) }
    val uiScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun attachCurrentCheckin() {
        val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.asSequence()
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider ->
                runCatching {
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) manager.getLastKnownLocation(provider) else null
                }.getOrNull()
            }
            .maxByOrNull { it.time }
        if (location != null) {
            checkin = "My current location"
            checkinLat = location.latitude
            checkinLng = location.longitude
            checkinLocationStatus = "Location attached to this post"
            checkinDialog = false
        } else {
            checkinLocationStatus = "Location is not supported."
        }
    }

    val checkinLocationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) attachCurrentCheckin()
        else checkinLocationStatus = "Location permission was denied."
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { photoUri = uri; videoUri = null }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { videoUri = uri; photoUri = null }
    }

    val homeVote = voteHighlight?.takeIf {
        mode == "global" && it.status in listOf("active", "ended", "forfeit")
    }
    val voteTimeKey = homeVote?.let(::voteFeedTimeKey).orEmpty()
    val voteInsertIndex = if (homeVote == null) -1 else {
        val found = posts.indexOfFirst { postTimeKey(it.createdAt) < voteTimeKey }
        if (found >= 0) found else posts.size
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 7.dp, end = 7.dp, top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (user == null) {
            item {
                JellyGlass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Welcome to My Chhachh", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(
                                "Browse public posts, or sign in to like, comment, message and post.",
                                color = JellyMuted,
                                fontSize = 9.5f.sp
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            JellyButton("Login", Modifier.weight(1f), icon = JellyIcons.User, onClick = onLogin)
                            JellyButton("Create account", Modifier.weight(1f), primary = true, icon = JellyIcons.Plus, onClick = onRegister)
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                HomeTabPill("For You", mode == "global", Modifier.weight(1f)) { onMode("global") }
                if (user != null) HomeTabPill("Following", mode == "following", Modifier.weight(1f)) { onMode("following") }
                HomeTabPill("Shop Posts", mode == "shops", Modifier.weight(1f)) { onMode("shops") }
            }
        }

        if (user != null && mode != "shops") item {
            JellyGlass(
                Modifier.fillMaxWidth(),
                radius = LiveJellyTheme.cardRadius.dp,
                padding = LiveJellyTheme.cardPadding.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = composing,
                        onValueChange = { composing = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        placeholder = { Text("What's on your mind? Type @ to mention someone", fontSize = 11.sp) },
                        shape = RoundedCornerShape(22.dp),
                        minLines = 3,
                        maxLines = 4
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ComposerTool(JellyIcons.Photo, if (photoUri != null) "Photo ✓" else "Photo", Modifier.weight(1f)) { photoPicker.launch("image/*") }
                        ComposerTool(JellyIcons.Video, if (videoUri != null) "Video ✓" else "Video", Modifier.weight(1f)) { videoPicker.launch("video/*") }
                        ComposerTool(JellyIcons.Feeling, if (feeling.isNotBlank()) "Feeling ✓" else "Feeling", Modifier.weight(1f)) { feelingDialog = true }
                        ComposerTool(JellyIcons.Pin, if (checkin.isNotBlank()) "Check in ✓" else "Check in", Modifier.weight(1f)) { checkinDialog = true }
                        ComposerTool(JellyIcons.Mention, "Mention", Modifier.weight(1f)) { mentionDialog = true }
                    }
                    if (feeling.isNotBlank() || checkin.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (feeling.isNotBlank()) Tag(JellyIcons.Feeling, feeling)
                            if (checkin.isNotBlank()) Tag(JellyIcons.Pin, checkin)
                        }
                    }
                    if (photoUri != null || videoUri != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (photoUri != null) "Photo ready" else "Video ready",
                                Modifier.weight(1f),
                                color = JellyMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { photoUri = null; videoUri = null }) {
                                Text("Remove", color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JellyButton(
                            if (privacy == "public") "Everyone" else "Followers",
                            Modifier.weight(1f).height(48.dp),
                            icon = JellyIcons.Eye
                        ) { privacy = if (privacy == "public") "followers" else "public" }
                        JellyButton(
                            "Post",
                            Modifier.weight(.72f).height(48.dp),
                            primary = true,
                            icon = JellyIcons.Send
                        ) {
                            if (composing.isNotBlank() || feeling.isNotBlank() || checkin.isNotBlank() || photoUri != null || videoUri != null) {
                                onCreatePost(composing.trim(), privacy, feeling, checkin, checkinLat, checkinLng, photoUri, videoUri)
                                composing = ""
                                feeling = ""
                                checkin = ""
                                checkinLat = null
                                checkinLng = null
                                photoUri = null
                                videoUri = null
                            }
                        }
                    }
                }
            }
        }

        if (loading && posts.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it) } }
        posts.forEachIndexed { index, post ->
            if (index == voteInsertIndex && homeVote != null) {
                item(key = "feed-vote-${homeVote.id}") {
                    V95VoteFeedCard(homeVote, onOpen = { onOpenVote(homeVote.id) })
                }
            }
            item(key = "post-${post.id}") {
                PostCard(
                    post = post,
                    loggedIn = user != null,
                    onLogin = onLogin,
                    onProfile = { onProfile(post.user.id) },
                    onLike = onLike,
                    onComment = onComment,
                    onShare = onShare,
                    onSave = onSave,
                    onVote = onOpenVote,
                    onEdit = if ((user?.id == post.user.id || user?.isAdmin == true) && post.shopId == 0L) onEditPost else null,
                    onDelete = if ((user?.id == post.user.id || user?.isAdmin == true) && post.shopId == 0L) onDeletePost else null,
                    onReport = if (user != null && user.id != post.user.id && !user.isAdmin && post.shopId == 0L) {
                        { p, reason -> onReportPost(p, reason) }
                    } else null
                )
            }
        }
        if (voteInsertIndex == posts.size && homeVote != null) {
            item(key = "feed-vote-${homeVote.id}") {
                V95VoteFeedCard(homeVote, onOpen = { onOpenVote(homeVote.id) })
            }
        }
        if (!loading && posts.isEmpty() && error == null) {
            item { EmptyCard(if (mode == "shops") "No shop posts yet." else "No posts yet.") }
        }
        if (hasMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    JellyButton(if (loading) "Loading…" else "Load more posts", enabled = !loading, onClick = onLoadMore)
                }
            }
        }
    }
    if (feelingDialog) {
        val feelings = listOf(
            "😊" to "Happy",
            "❤️" to "Loved",
            "🎉" to "Celebrating",
            "🙏" to "Grateful",
            "🤔" to "Thinking",
            "😢" to "Sad",
            "😡" to "Angry"
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { feelingDialog = false },
            title = { Text("Choose a feeling", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    feelings.forEach { (emoji, label) ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 9.dp,
                            onClick = {
                                feeling = label
                                feelingDialog = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 22.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(label, color = JellyInk, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (feeling.isNotBlank()) {
                        JellyButton("Remove feeling", Modifier.fillMaxWidth()) {
                            feeling = ""
                            feelingDialog = false
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { feelingDialog = false } }
        )
    }

    if (mentionDialog) {
        var query by remember { mutableStateOf("") }
        var users by remember { mutableStateOf<List<User>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        var searchError by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mentionDialog = false },
            title = { Text("Mention someone", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search name or username") },
                        singleLine = true,
                        shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp)
                    )
                    JellyButton(
                        if (searching) "Searching…" else "Search people",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Search,
                        enabled = !searching && query.isNotBlank()
                    ) {
                        uiScope.launch {
                            searching = true
                            searchError = null
                            try {
                                users = onSearchMentions(query.trim())
                                if (users.isEmpty()) searchError = "No people found."
                            } catch (e: Exception) {
                                users = emptyList()
                                searchError = e.message ?: "Could not search people."
                            } finally {
                                searching = false
                            }
                        }
                    }
                    searchError?.let { Text(it, color = Color(0xFFB23A55), fontSize = 10.sp) }
                    users.take(8).forEach { person ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 8.dp,
                            onClick = {
                                val mention = "@${person.username}"
                                composing = when {
                                    composing.isBlank() -> mention
                                    composing.endsWith(" ") -> composing + mention
                                    else -> composing + " " + mention
                                }
                                mentionDialog = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(person, 34.dp)
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    UserName(person, 11)
                                    Text("@${person.username}", color = JellyMuted, fontSize = 8.5f.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { mentionDialog = false } }
        )
    }

    if (checkinDialog) {
        var query by remember { mutableStateOf(checkin) }
        var places by remember { mutableStateOf<List<CheckinPlace>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        var searchError by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { checkinDialog = false },
            title = { Text("Check in", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a location from the search results or use current location.", color = JellyMuted, fontSize = 10.5f.sp)
                    JellyButton("Use current location", Modifier.fillMaxWidth(), icon = JellyIcons.Pin) {
                        checkinLocationStatus = "Getting your current location…"
                        val granted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) attachCurrentCheckin()
                        else checkinLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                    checkinLocationStatus?.let { Text(it, color = JellyMuted, fontSize = 9.sp) }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search a place in Chhachh / Hazro") },
                        singleLine = true,
                        shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp)
                    )
                    JellyButton(
                        if (searching) "Searching…" else "Find place",
                        Modifier.fillMaxWidth(),
                        primary = true,
                        icon = JellyIcons.Search,
                        enabled = !searching && query.isNotBlank()
                    ) {
                        uiScope.launch {
                            searching = true
                            searchError = null
                            try {
                                places = onSearchCheckin(query.trim())
                                if (places.isEmpty()) searchError = "Place could not be found."
                            } catch (e: Exception) {
                                places = emptyList()
                                searchError = e.message ?: "Place could not be found."
                            } finally {
                                searching = false
                            }
                        }
                    }
                    searchError?.let { Text(it, color = Color(0xFFB23A55), fontSize = 10.sp) }
                    places.forEach { place ->
                        JellyGlass(
                            Modifier.fillMaxWidth(),
                            radius = 16.dp,
                            padding = 8.dp,
                            onClick = {
                                checkin = place.name.split(",").take(3).joinToString(", ").trim()
                                checkinLat = place.lat
                                checkinLng = place.lng
                                checkinDialog = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                JellyIcon(JellyIcons.Pin, size = 24.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(place.name, Modifier.weight(1f), color = JellyInk, fontSize = 10.sp, maxLines = 2)
                                Text("Select", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                        }
                    }
                    if (checkin.isNotBlank()) {
                        JellyButton("Remove", Modifier.fillMaxWidth()) {
                            checkin = ""
                            checkinLat = null
                            checkinLng = null
                            checkinDialog = false
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { checkinDialog = false } }
        )
    }

}

private fun postTimeKey(raw: String): String =
    raw.trim().replace(' ', 'T').take(19)

private fun voteFeedTimeKey(vote: Vote): String {
    val raw = if (vote.status == "ended" || vote.status == "forfeit") {
        vote.endedAt.ifBlank { vote.updatedAt.ifBlank { vote.createdAt } }
    } else {
        vote.startsAt.ifBlank { vote.updatedAt.ifBlank { vote.createdAt } }
    }
    return postTimeKey(raw)
}

@Composable
private fun V95VoteFeedCard(vote: Vote, onOpen: () -> Unit) {
    val ended = vote.status == "ended" || vote.status == "forfeit"
    val winner = when (vote.winnerUserId) {
        vote.leftUserId -> vote.user1
        vote.rightUserId -> vote.user2
        else -> null
    }
    if (ended && winner != null) {
        val winnerVotes = if (vote.winnerUserId == vote.leftUserId) vote.votes1 else vote.votes2
        JellyGlass(
            Modifier.fillMaxWidth().heightIn(min = 220.dp),
            radius = LiveJellyTheme.cardRadius.dp,
            padding = LiveJellyTheme.cardPadding.dp,
            onClick = onOpen
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                JellyIcon(JellyIcons.Crown, size = 48.dp)
                Avatar(winner, 96.dp)
                Text("CONGRATULATIONS", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.sp)
                UserName(winner, 18)
                if (winner.username.isNotBlank()) Text("@${winner.username}", color = JellyMuted, fontSize = 9.5f.sp)
                Text("Winner of the voting match · $winnerVotes vote${if (winnerVotes == 1) "" else "s"}", color = JellyInk, fontSize = 10.sp)
                Text("View final result →", color = JellyPurple, fontWeight = FontWeight.Black, fontSize = 9.5f.sp)
            }
        }
        return
    }

    JellyGlass(
        Modifier.fillMaxWidth(),
        radius = LiveJellyTheme.cardRadius.dp,
        padding = LiveJellyTheme.cardPadding.dp,
        onClick = onOpen
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(99.dp)).background(JellyGreen))
                Spacer(Modifier.width(6.dp))
                Text("Live Voting", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("Tap to open", color = JellyMuted, fontSize = 8.5f.sp)
            }
            JellyGlass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    VoteTeaserSide(vote.user1, if (vote.resultRevealed) vote.votes1 else null, Modifier.weight(1f))
                    Column(
                        Modifier.width(112.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (vote.endsAt.isNotBlank()) {
                            Text("LIVE", color = JellyGreen, fontWeight = FontWeight.Black, fontSize = 8.sp)
                        }
                        Box(
                            Modifier
                                .size(58.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color.White,
                                            Color(0xFFF678EB).copy(alpha = .82f),
                                            Color(0xFF4BA7FF).copy(alpha = .90f)
                                        )
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = .90f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                    VoteTeaserSide(vote.user2, if (vote.resultRevealed) vote.votes2 else null, Modifier.weight(1f))
                }
            }
            Text(
                if (vote.resultRevealed) "Live vote counts are visible · Open match →"
                else "Vote counts are hidden until the result · Open match →",
                Modifier.fillMaxWidth(),
                color = JellyMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VoteTeaserSide(user: User?, votes: Int?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (user != null) {
            Avatar(user, 64.dp)
            UserName(user, 9)
        } else {
            JellyIcon(JellyIcons.User, size = 48.dp)
        }
        votes?.let { Text("$it votes", color = JellyMuted, fontSize = 8.5f.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HomeTabPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .height(43.dp)
            .clip(shape)
            .clickable { onClick() }
            .background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFFFF5EB7), Color(0xFFBF5DE6)))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = .72f), Color.White.copy(alpha = .72f)))
            )
            .border(1.5.dp, Color.White.copy(alpha = .95f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else JellyInk, fontWeight = FontWeight.Black, fontSize = 10.5f.sp, maxLines = 1)
    }
}

@Composable
private fun ComposerTool(icon: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 2.dp)
    ) {
        val compact = LocalConfiguration.current.screenWidthDp <= 390
        JellyIcon(icon, size = if (compact) 25.dp else 27.dp)
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color(0xFF4C4176), fontSize = (if (compact) 6.9f else 7.5f).sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
fun PostCard(
    post: Post,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onProfile: () -> Unit,
    onLike: (Post) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onSave: (Post) -> Unit,
    onVote: ((Long) -> Unit)? = null,
    onEdit: ((Post, String, String) -> Unit)? = null,
    onDelete: ((Post) -> Unit)? = null,
    onReport: ((Post, String) -> Unit)? = null
) {
    var videoOpen by remember(post.id) { mutableStateOf(false) }
    var moreOpen by remember(post.id) { mutableStateOf(false) }
    var editOpen by remember(post.id) { mutableStateOf(false) }
    var deleteConfirm by remember(post.id) { mutableStateOf(false) }
    var reportOpen by remember(post.id) { mutableStateOf(false) }
    var reactionOpen by remember(post.id) { mutableStateOf(false) }
    var myReaction by remember(post.id, post.myReaction) { mutableStateOf(post.myReaction) }
    var reactionTotal by remember(post.id, post.reactionTotal, post.likes) {
        mutableIntStateOf(if (post.reactionTotal > 0) post.reactionTotal else post.likes)
    }
    var reactionBusy by remember(post.id) { mutableStateOf(false) }
    var reactionError by remember(post.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val reactionScope = rememberCoroutineScope()
    val reactionApi = remember(context) { ApiClient(context.applicationContext) }

    fun setReaction(type: String) {
        if (reactionBusy || post.shopId > 0L) return
        val previous = myReaction
        val previousTotal = reactionTotal
        myReaction = type
        reactionTotal = when {
            previous.isBlank() && type.isNotBlank() -> reactionTotal + 1
            previous.isNotBlank() && type.isBlank() -> (reactionTotal - 1).coerceAtLeast(0)
            else -> reactionTotal
        }
        reactionOpen = false
        reactionBusy = true
        reactionError = null
        reactionScope.launch {
            try {
                withContext(Dispatchers.IO) { reactionApi.reactPost(post.id, type) }
            } catch (e: Exception) {
                myReaction = previous
                reactionTotal = previousTotal
                reactionError = e.message ?: "Reaction could not be saved."
            } finally {
                reactionBusy = false
            }
        }
    }

    JellyGlass(
        Modifier.fillMaxWidth(),
        radius = LiveJellyTheme.cardRadius.dp,
        padding = LiveJellyTheme.cardPadding.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Avatar(post.user, 42.dp, Modifier.clickable { onProfile() })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f).clickable { onProfile() }) {
                    UserName(post.user, 14)
                    Text(
                        "${shortTime(post.createdAt)}  •  ${if (post.privacy == "followers") "Followers" else "Everyone"}",
                        color = JellyMuted,
                        fontSize = 9.5f.sp
                    )
                }
                if (loggedIn) {
                    JellyIconButton(JellyIcons.More, "More", onClick = { moreOpen = true })
                }
            }

            if (!post.feeling.isNullOrBlank() || !post.checkin.isNullOrBlank()) {
                Row(Modifier.padding(horizontal = 11.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.feeling?.let { Tag(JellyIcons.Feeling, it) }
                    post.checkin?.let { label ->
                        val lat = post.checkinLat
                        val lng = post.checkinLng
                        Tag(
                            JellyIcons.Pin,
                            label,
                            onClick = if (lat != null && lng != null) {
                                {
                                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                }
                            } else null
                        )
                    }
                }
            }

            post.photo?.let { url ->
                AsyncImage(
                    url,
                    null,
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .heightIn(min = 170.dp, max = 520.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }

            post.video?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFEAF9FF), Color(0xFFF0E9FF))))
                        .clickable { videoOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        JellyIcon(JellyIcons.Video, size = 48.dp)
                        Text("Play video", color = JellyInk, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        Text("Opens inside the app", color = JellyMuted, fontSize = 9.sp)
                    }
                }
            }

            if (post.voteId > 0L) {
                JellyGlass(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .then(if (onVote != null) Modifier.clickable { onVote?.invoke(post.voteId) } else Modifier),
                    radius = 18.dp,
                    padding = 10.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JellyIcon(JellyIcons.Vote, size = 34.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Voting Match", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 11.5f.sp)
                            Text(
                                "${post.voteLeftName.ifBlank { "Player 1" }}  VS  ${post.voteRightName.ifBlank { "Player 2" }}",
                                color = JellyMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5f.sp
                            )
                        }
                        JellyIcon(JellyIcons.Arrow, size = 21.dp)
                    }
                }
            }

            if (post.text.isNotBlank()) {
                Text(
                    post.text,
                    Modifier.padding(top = 10.dp),
                    color = JellyInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            if (post.shopId == 0L && reactionOpen && loggedIn) {
                val reactionChoices = listOf(
                    Triple("like", "👍", "Like"),
                    Triple("love", "❤️", "Love"),
                    Triple("haha", "😂", "Haha"),
                    Triple("wow", "😮", "Wow"),
                    Triple("sad", "😢", "Sad"),
                    Triple("angry", "😡", "Angry")
                )
                JellyGlass(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    radius = 999.dp,
                    padding = 6.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reactionChoices.forEach { (key, emoji, label) ->
                            Column(
                                Modifier
                                    .width(56.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(enabled = !reactionBusy) { setReaction(key) }
                                    .background(if (myReaction == key) Color.White.copy(alpha = .86f) else Color.Transparent)
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(emoji, fontSize = 27.sp)
                                Text(label, color = JellyInk, fontSize = 7.5f.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        if (myReaction.isNotBlank()) {
                            Column(
                                Modifier
                                    .width(56.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(enabled = !reactionBusy) { setReaction("") }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                JellyIcon(JellyIcons.Close, size = 27.dp)
                                Text("Remove", color = JellyMuted, fontSize = 7.5f.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
            reactionError?.let {
                Text(it, Modifier.padding(top = 5.dp), color = Color(0xFFB23A55), fontSize = 8.5f.sp)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp)
                    .height(1.dp)
                    .background(JellyMuted.copy(alpha = .12f))
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val reactionLabel = when (myReaction) {
                    "love" -> "Love"
                    "haha" -> "Haha"
                    "wow" -> "Wow"
                    "sad" -> "Sad"
                    "angry" -> "Angry"
                    "like" -> "Like"
                    else -> if (post.shopId > 0L && post.liked) "Liked" else "Like"
                }
                PostAction(
                    JellyIcons.Heart,
                    reactionLabel,
                    if (post.shopId > 0L) post.likes else reactionTotal,
                    Modifier.weight(1f)
                ) {
                    if (!loggedIn) onLogin()
                    else if (post.shopId > 0L) onLike(post)
                    else reactionOpen = !reactionOpen
                }
                PostAction(JellyIcons.Comment, "Comment", post.comments, Modifier.weight(1f)) { if (loggedIn) onComment(post) else onLogin() }
                PostStat(JellyIcons.Eye, "Views", post.views, Modifier.weight(1f))
                PostAction(JellyIcons.Share, "Share", post.shares, Modifier.weight(1f)) { if (loggedIn) onShare(post) else onLogin() }
            }
        }
    }

    if (videoOpen) post.video?.let { VideoDialog(it) { videoOpen = false } }

    if (moreOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { moreOpen = false },
            title = { Text("Post options", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.shopId == 0L) {
                        JellyButton(
                            if (post.saved) "Remove from Saved" else "Save post",
                            Modifier.fillMaxWidth(),
                            primary = !post.saved,
                            icon = JellyIcons.Save
                        ) {
                            onSave(post)
                            moreOpen = false
                        }
                    }
                    if (onEdit != null) {
                        JellyButton("Edit post", Modifier.fillMaxWidth(), icon = JellyIcons.Edit) {
                            moreOpen = false
                            editOpen = true
                        }
                    }
                    if (onDelete != null) {
                        JellyButton("Delete post", Modifier.fillMaxWidth(), icon = JellyIcons.Delete) {
                            moreOpen = false
                            deleteConfirm = true
                        }
                    }
                    if (onReport != null) {
                        JellyButton("Report post", Modifier.fillMaxWidth(), icon = JellyIcons.Shield, danger = true) {
                            moreOpen = false
                            reportOpen = true
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { JellyButton("Close") { moreOpen = false } }
        )
    }

    if (editOpen && onEdit != null) {
        var editText by remember(post.id) { mutableStateOf(post.text) }
        var editPrivacy by remember(post.id) { mutableStateOf(post.privacy) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text("Edit post", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 7,
                        shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp)
                    )
                    JellyButton(
                        if (editPrivacy == "followers") "Followers" else "Everyone",
                        icon = JellyIcons.Eye
                    ) {
                        editPrivacy = if (editPrivacy == "followers") "public" else "followers"
                    }
                }
            },
            confirmButton = {
                JellyButton("Save", primary = true, icon = JellyIcons.Check) {
                    onEdit(post, editText.trim(), editPrivacy)
                    editOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { editOpen = false } }
        )
    }

    if (deleteConfirm && onDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("Delete post", color = JellyInk, fontWeight = FontWeight.Black) },
            text = { Text("Delete this post permanently?", color = JellyInk) },
            confirmButton = {
                JellyButton("Delete", icon = JellyIcons.Delete, danger = true) {
                    onDelete(post)
                    deleteConfirm = false
                }
            },
            dismissButton = { JellyButton("Cancel") { deleteConfirm = false } }
        )
    }

    if (reportOpen && onReport != null) {
        var reason by remember(post.id) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("Report post", color = JellyInk, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    reason,
                    { reason = it.take(3000) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Explain the problem…") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp)
                )
            },
            confirmButton = {
                JellyButton("Send Report", primary = true, icon = JellyIcons.Shield, enabled = reason.trim().length >= 3) {
                    onReport(post, reason.trim())
                    reportOpen = false
                }
            },
            dismissButton = { JellyButton("Cancel") { reportOpen = false } }
        )
    }
}

@Composable
private fun Tag(icon: Int, text: String, onClick: (() -> Unit)? = null) {
    var modifier = Modifier
        .clip(RoundedCornerShape(13.dp))
        .background(Color.White.copy(.86f))
    if (onClick != null) modifier = modifier.clickable { onClick() }
    Row(
        modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JellyIcon(icon, size = 18.dp)
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 9.5f.sp, color = JellyInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PostAction(icon: Int, label: String, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val compact = screenWidth <= 390
        JellyIcon(icon, size = if (compact) 25.dp else 28.dp)
        Spacer(Modifier.width(if (compact) 2.dp else 3.dp))
        Text(
            label,
            color = Color(0xFF4C4176),
            fontSize = (if (screenWidth <= 700) 9f else 10f).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        if (count > 0) {
            Spacer(Modifier.width(2.dp))
            if (label.equals("Comment", ignoreCase = true)) {
                Box(
                    Modifier
                        .widthIn(min = if (screenWidth <= 700) 14.dp else 15.dp)
                        .height(if (screenWidth <= 700) 14.dp else 15.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF7B65D7).copy(alpha = .12f))
                        .padding(horizontal = if (screenWidth <= 700) 3.dp else 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        count.toString(),
                        color = Color(0xFF4C4176),
                        fontSize = (if (screenWidth <= 700) 8f else 9f).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            } else {
                Text(count.toString(), color = Color(0xFF4C4176), fontSize = (if (screenWidth <= 700) 9f else 10f).sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PostStat(icon: Int, label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val compact = screenWidth <= 390
        JellyIcon(icon, size = if (compact) 25.dp else 28.dp)
        Spacer(Modifier.width(if (compact) 2.dp else 3.dp))
        Text(label, color = Color(0xFF4C4176), fontSize = (if (screenWidth <= 700) 9f else 10f).sp, fontWeight = FontWeight.Black, maxLines = 1)
        if (count > 0) {
            Spacer(Modifier.width(2.dp))
            Text(count.toString(), color = Color(0xFF4C4176), fontSize = (if (screenWidth <= 700) 9f else 10f).sp, fontWeight = FontWeight.Black)
        }
    }
}


@Composable
fun PostDiscussionDialog(
    post: Post,
    comments: List<Comment>,
    likesUsers: List<User>,
    loading: Boolean,
    error: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSend: (String, Long) -> Unit,
    onLikeComment: (Long) -> Unit,
    onProfile: (Long) -> Unit
) {
    var mode by remember(post.id) { mutableStateOf("comments") }
    var text by remember(post.id) { mutableStateOf("") }
    var replyTo by remember(post.id) { mutableStateOf<Comment?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JellyPill("Comments ${post.comments}", mode == "comments", Modifier.weight(1f)) { mode = "comments" }
                JellyPill("Likes ${post.likes}", mode == "likes", Modifier.weight(1f)) { mode = "likes" }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) LoadingBlock()
                error?.let { ErrorCard(it) }

                if (!loading && mode == "comments") {
                    val roots = comments.filter { it.parentId == 0L }
                    if (roots.isEmpty()) {
                        Text("No comments yet.", color = JellyMuted, fontSize = 11.sp)
                    } else {
                        LazyColumn(
                            Modifier.weight(1f, fill = false).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(roots, key = { "comment-${it.id}" }) { comment ->
                                CommentRow(comment, false, onProfile, onLikeComment) { replyTo = comment }
                                comments.filter { it.parentId == comment.id }.forEach { reply ->
                                    Spacer(Modifier.height(3.dp))
                                    CommentRow(reply, true, onProfile, onLikeComment) { replyTo = comment }
                                }
                            }
                        }
                    }

                    replyTo?.let {
                        JellyGlass(Modifier.fillMaxWidth(), radius = 14.dp, padding = 7.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Replying to @${it.user.username}", Modifier.weight(1f), color = JellyMuted, fontSize = 9.5f.sp)
                                JellyButton("Cancel") { replyTo = null }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            text,
                            { text = it.take(3000) },
                            Modifier.weight(1f),
                            placeholder = { Text(if (replyTo == null) "Write a comment... Type @ to mention" else "Write a reply…") },
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        JellyIconButton(JellyIcons.Send, "Send") {
                            if (!busy && text.isNotBlank()) {
                                onSend(text.trim(), replyTo?.id ?: 0L)
                                text = ""
                                replyTo = null
                            }
                        }
                    }
                }

                if (!loading && mode == "likes") {
                    Text("People who liked this post", color = JellyInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    if (likesUsers.isEmpty()) {
                        Text(
                            if (post.shopId > 0) "Like list is not exposed by the shop-post API." else "No likes yet.",
                            color = JellyMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        LazyColumn(
                            Modifier.weight(1f, fill = false).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(likesUsers, key = { "like-user-${it.id}" }) { user ->
                                JellyGlass(
                                    Modifier.fillMaxWidth(),
                                    radius = 15.dp,
                                    padding = 8.dp,
                                    onClick = { onProfile(user.id) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Avatar(user, 38.dp)
                                        Spacer(Modifier.width(7.dp))
                                        Column {
                                            UserName(user, 11)
                                            if (user.username.isNotBlank()) Text("@${user.username}", color = JellyMuted, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { JellyButton("Close", onClick = onDismiss) }
    )
}

@Composable
private fun CommentRow(
    comment: Comment,
    reply: Boolean,
    onProfile: (Long) -> Unit,
    onLike: (Long) -> Unit,
    onReply: () -> Unit
) {
    JellyGlass(
        Modifier.fillMaxWidth().padding(start = if (reply) 26.dp else 0.dp),
        radius = 16.dp,
        padding = 8.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(comment.user, if (reply) 30.dp else 34.dp, Modifier.clickable { onProfile(comment.user.id) })
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    UserName(comment.user, if (reply) 10 else 11)
                    Text(shortTime(comment.createdAt), color = JellyMuted, fontSize = 8.sp)
                }
            }
            Text(comment.text, color = JellyInk, fontSize = if (reply) 10.5f.sp else 11.5f.sp, lineHeight = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                JellyButton(if (comment.liked) "Liked ${comment.likes}" else "Like ${comment.likes}", icon = JellyIcons.Heart) {
                    onLike(comment.id)
                }
                JellyButton("Reply", icon = JellyIcons.Reply, onClick = onReply)
            }
        }
    }
}
