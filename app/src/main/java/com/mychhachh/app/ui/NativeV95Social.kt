package com.mychhachh.app.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
internal fun NativePeople(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.people.isEmpty()) c.loadPeople() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("People", "لوگ"), c.t("People you may know", "لوگ جنہیں آپ جانتے ہوں")) }
        if (c.people.isEmpty() && !c.busy) item { NVEmpty(c.t("No people found", "کوئی یوزر نہیں ملا")) }
        items(c.people, key = { it.id }) { person ->
            NVCard(radius = 22.dp, padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NVAvatar(person, 52.dp, Modifier.clickable { c.openProfile(person) })
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { c.openProfile(person) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            if (person.verified) {
                                Spacer(Modifier.width(3.dp))
                                Image(painterResource(NVIcons.Check), null, Modifier.size(16.dp))
                            }
                        }
                        Text("@" + person.username, color = NVMuted, fontSize = 10.sp)
                        val place = listOf(person.village, person.city).filter { it.isNotBlank() }.joinToString(" · ")
                        if (place.isNotBlank()) Text(place, color = NVMuted, fontSize = 9.5.sp)
                    }
                    if (c.user?.id != person.id) {
                        NVButton(
                            if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                            primary = !person.followed,
                            icon = NVIcons.Follow
                        ) {
                            if (c.user == null) c.route = V95Route.AUTH else c.follow(person)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeShops(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.shops.isEmpty()) c.loadShops() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Shops", "دکانیں"), c.t("Local shops and services", "مقامی دکانیں اور سروسز")) }
        if (c.shops.isEmpty() && !c.busy) item { NVEmpty(c.t("No shops found", "کوئی دکان نہیں ملی")) }
        items(c.shops, key = { it.id }) { shop ->
            NVCard(radius = 22.dp, padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NVShopAvatar(shop, 54.dp, Modifier.clickable { c.openShop(shop) })
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { c.openShop(shop) }) {
                        Text(shop.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(shop.category, color = NVMuted, fontSize = 10.sp)
                        val place = listOf(shop.village, shop.city).filter { it.isNotBlank() }.joinToString(" · ")
                        if (place.isNotBlank()) Text(place, color = NVMuted, fontSize = 9.5.sp)
                    }
                    NVButton(
                        if (shop.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"),
                        primary = !shop.followed
                    ) {
                        if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeMessages(c: V95Controller) {
    if (c.user == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { if (c.conversations.isEmpty()) c.loadMessages() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { NVHeading(c.t("Messages", "پیغامات"), c.t("Your conversations", "آپ کی گفتگو")) }
        if (c.conversations.isEmpty() && !c.busy) item { NVEmpty(c.t("No conversations yet", "ابھی کوئی گفتگو نہیں")) }
        items(c.conversations, key = { it.user.id }) { conv ->
            NVCard(radius = 20.dp, padding = 10.dp) {
                Row(
                    Modifier.fillMaxWidth().clickable { c.openChat(conv.user) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NVAvatar(conv.user, 48.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(conv.user.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(conv.preview.ifBlank { c.t("Open conversation", "گفتگو کھولیں") }, color = NVMuted, fontSize = 10.sp, maxLines = 1)
                    }
                    if (conv.unread) Box(Modifier.size(9.dp).clip(CircleShape).background(NVPink))
                }
            }
        }
    }
}

@Composable
internal fun NativeChat(c: V95Controller) {
    val other = c.selectedChatUser
    if (c.user == null || other == null) {
        NativeRequireLogin(c)
        return
    }
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var audio by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "message_photo") { photo = it }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "message_audio") { audio = it }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        NVHeading(other.name, "@" + other.username)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(c.chatMessages, key = { it.id }) { msg ->
                val mine = msg.senderId == c.user?.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier
                            .fillMaxWidth(.78f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (mine) nvPrimaryBrush() else nvGlassBrush())
                            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
                            .padding(10.dp)
                    ) {
                        if (msg.text.isNotBlank()) Text(msg.text, color = if (mine) Color.White else NVInk, fontSize = 12.sp)
                        if (!msg.photo.isNullOrBlank()) AsyncImage(msg.photo, null, Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        if (!msg.audio.isNullOrBlank()) Text(c.t("Voice message", "وائس پیغام"), color = if (mine) Color.White else NVPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(msg.createdAt, color = if (mine) Color.White.copy(.82f) else NVMuted, fontSize = 8.sp)
                    }
                }
            }
        }

        if (photo.isNotBlank() || audio.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (photo.isNotBlank()) c.t("Photo attached", "فوٹو منسلک ہے") else c.t("Voice attached", "وائس منسلک ہے"),
                    color = NVGreen, fontSize = 10.sp, modifier = Modifier.weight(1f)
                )
                Text(c.t("Remove", "ہٹائیں"), color = NVPink, fontSize = 10.sp, modifier = Modifier.clickable { photo = ""; audio = "" })
            }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            NVIconButton(NVIcons.Photo, size = 40.dp, iconSize = 28.dp) { photoPicker.launch("image/*") }
            NVIconButton(NVIcons.Message, size = 40.dp, iconSize = 28.dp) { audioPicker.launch("audio/*") }
            NVInput(text, c.t("Message…", "پیغام…"), Modifier.weight(1f)) { text = it }
            NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 32.dp) {
                if (text.isNotBlank() || photo.isNotBlank() || audio.isNotBlank()) {
                    c.sendRichMessage(text, photo, audio) {
                        text = ""; photo = ""; audio = ""
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativeVotes(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.votes.isEmpty()) c.loadVotes() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Voting", "ووٹنگ"), c.t("Live community voting", "لائیو کمیونٹی ووٹنگ")) }
        if (c.votes.isEmpty() && !c.busy) item { NVEmpty(c.t("No active voting", "کوئی ووٹنگ موجود نہیں")) }
        items(c.votes, key = { it.id }) { vote -> NativeVoteCard(c, vote) }
    }
}

@Composable
private fun NativeVoteCard(c: V95Controller, vote: Vote, detail: Boolean = false) {
    var now by remember(vote.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vote.id, vote.endsAt, vote.status) {
        while (vote.status.lowercase() in setOf("active", "started", "live")) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timer = nativeVoteRemaining(vote.endsAt, now, c)

    NVCard(modifier = Modifier.clickable { c.openVote(vote) }, radius = 28.dp, padding = 14.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vote.status.uppercase(), color = NVMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(vote.title.ifBlank { (vote.user1?.name ?: "") + " VS " + (vote.user2?.name ?: "") }, color = NVInk, fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(timer, color = NVPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NativeVoteSide(c, vote.user1, vote.leftText, vote.votes1, vote.leftUserId, vote, Modifier.weight(1f))
            Box(Modifier.size(47.dp).clip(CircleShape).background(nvPrimaryBrush()).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            NativeVoteSide(c, vote.user2, vote.rightText, vote.votes2, vote.rightUserId, vote, Modifier.weight(1f))
        }
        if (detail) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NVButton(c.t("Comments", "کمنٹس"), Modifier.weight(1f), icon = NVIcons.Comment) { }
                NVButton(c.t("Share", "شیئر"), Modifier.weight(1f), primary = true, icon = NVIcons.Share) {
                    if (c.user == null) c.route = V95Route.AUTH else c.shareVote(vote)
                }
            }
        }
    }
}

@Composable
private fun NativeVoteSide(c: V95Controller, person: User?, statement: String, count: Int, id: Long, vote: Vote, modifier: Modifier) {
    Column(modifier.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (person != null) NVAvatar(person, 62.dp) else Box(Modifier.size(62.dp).clip(CircleShape).background(nvInputBrush()))
        Spacer(Modifier.height(4.dp))
        Text(person?.name ?: statement.ifBlank { c.t("Option", "آپشن") }, color = NVInk, fontWeight = FontWeight.Black, fontSize = 10.5.sp, maxLines = 2, textAlign = TextAlign.Center)
        Text(count.toString(), color = NVPurple, fontWeight = FontWeight.Black, fontSize = 15.sp)
        NVButton(c.t("Vote", "ووٹ"), primary = vote.myChoice != id) {
            if (c.user == null) c.route = V95Route.AUTH else if (id > 0) c.castVote(vote, id)
        }
    }
}

@Composable
internal fun NativeVoteDetail(c: V95Controller) {
    val vote = c.selectedVote
    if (vote == null) {
        NVEmpty(c.t("Voting unavailable", "ووٹنگ دستیاب نہیں"))
        return
    }
    var comment by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NativeVoteCard(c, vote, detail = true) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NVInput(comment, c.t("Write a comment…", "کمنٹ لکھیں…"), Modifier.weight(1f)) { comment = it }
                NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 31.dp) {
                    if (comment.isNotBlank()) c.addVoteComment(comment) { comment = "" }
                }
            }
        }
        items(c.voteCommentsList, key = { it.id }) { NativeCommentCard(it) }
    }
}

@Composable
internal fun NativeAnnouncements(c: V95Controller) {
    LaunchedEffect(Unit) { if (c.announcements.isEmpty()) c.loadAnnouncements() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Announcements", "اعلانات"), c.t("Voice and community notices", "وائس اور کمیونٹی اعلانات")) }
        if (c.user != null) item { NativeAnnouncementComposer(c) }
        if (c.announcements.isEmpty() && !c.busy) item { NVEmpty(c.t("No announcements", "کوئی اعلان نہیں")) }
        items(c.announcements, key = { it.id }) { item -> NativeAnnouncementCard(c, item) }
    }
}

@Composable
private fun NativeAnnouncementComposer(c: V95Controller) {
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var audio by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "announcement_photo") { photo = it }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) c.upload(uri, "announcement_audio") { audio = it }
    }
    NVCard(radius = 28.dp, padding = 12.dp) {
        Text(c.t("New announcement", "نیا اعلان"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
        NVInput(text, c.t("Write an announcement…", "اعلان لکھیں…"), Modifier.fillMaxWidth(), singleLine = false) { text = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NVButton(c.t("Photo", "فوٹو"), Modifier.weight(1f), icon = NVIcons.Photo) { photoPicker.launch("image/*") }
            NVButton(c.t("Voice", "وائس"), Modifier.weight(1f), icon = NVIcons.Message) { audioPicker.launch("audio/*") }
            NVButton(c.t("Post", "پوسٹ"), Modifier.weight(1f), primary = true) {
                if (text.isNotBlank() || photo.isNotBlank() || audio.isNotBlank()) {
                    c.createAnnouncement(text, photo, audio) { text = ""; photo = ""; audio = "" }
                }
            }
        }
    }
}

@Composable
private fun NativeAnnouncementCard(c: V95Controller, item: Announcement) {
    NVCard(modifier = Modifier.clickable { c.openAnnouncement(item) }, radius = 22.dp, padding = 11.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val author = item.author
            if (author != null) NVAvatar(author, 42.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(author?.name ?: c.t("Announcement", "اعلان"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                Text(item.createdAt, color = NVMuted, fontSize = 9.sp)
            }
            Image(painterResource(NVIcons.Announcement), null, Modifier.size(28.dp))
        }
        if (item.text.isNotBlank()) Text(item.text, color = NVInk, fontSize = 12.sp, lineHeight = 18.sp)
        if (!item.photo.isNullOrBlank()) AsyncImage(item.photo, null, Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
        if (!item.audio.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(NVIcons.Message), null, Modifier.size(28.dp))
                Spacer(Modifier.width(6.dp))
                Text(c.t("Voice announcement", "وائس اعلان"), color = NVPurple, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            NativeSmallAction((if (item.liked) c.t("Liked", "لائکڈ") else c.t("Like", "لائک")) + " " + item.likes, NVIcons.Heart) {
                if (c.user == null) c.route = V95Route.AUTH else c.likeAnnouncement(item)
            }
            NativeSmallAction(c.t("Comments", "کمنٹس") + " " + item.comments, NVIcons.Comment) { c.openAnnouncement(item) }
            NativeSmallAction(c.t("Report", "رپورٹ"), NVIcons.More) {
                if (c.user == null) c.route = V95Route.AUTH else c.reportAnnouncement(item)
            }
        }
    }
}

@Composable
internal fun NativeAnnouncementDetail(c: V95Controller) {
    val item = c.selectedAnnouncement
    if (item == null) {
        NVEmpty(c.t("Announcement unavailable", "اعلان دستیاب نہیں"))
        return
    }
    var comment by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NativeAnnouncementCard(c, item) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NVInput(comment, c.t("Write a comment…", "کمنٹ لکھیں…"), Modifier.weight(1f)) { comment = it }
                NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 31.dp) {
                    if (comment.isNotBlank()) c.addAnnouncementComment(comment) { comment = "" }
                }
            }
        }
        items(c.announcementCommentsList, key = { it.id }) { NativeCommentCard(it) }
    }
}

@Composable
internal fun NativeNotifications(c: V95Controller) {
    if (c.user == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { c.loadNotifications() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { NVHeading(c.t("Notifications", "اطلاعات")) }
        if (c.notices.isEmpty() && !c.busy) item { NVEmpty(c.t("No notifications", "کوئی اطلاع نہیں")) }
        items(c.notices, key = { it.id }) { notice ->
            NVCard(radius = 22.dp, padding = 11.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    notice.actor?.let { NVAvatar(it, 40.dp) }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(notice.text, color = NVInk, fontWeight = if (notice.read) FontWeight.SemiBold else FontWeight.Black, fontSize = 11.5.sp)
                        Text(notice.createdAt, color = NVMuted, fontSize = 9.sp)
                    }
                    if (!notice.read) Box(Modifier.size(9.dp).clip(CircleShape).background(NVPink))
                }
            }
        }
    }
}

@Composable
internal fun NativeProfile(c: V95Controller) {
    val person = c.selectedUser ?: c.user
    if (person == null) {
        NativeRequireLogin(c)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            NVCard(radius = 28.dp, padding = 0.dp) {
                Box(
                    Modifier.fillMaxWidth().height(145.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))
                ) {
                    if (!person.cover.isNullOrBlank()) AsyncImage(person.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-44).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NVAvatar(person, 90.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        if (person.verified) {
                            Spacer(Modifier.width(4.dp))
                            Image(painterResource(NVIcons.Check), null, Modifier.size(19.dp))
                        }
                    }
                    Text("@" + person.username, color = NVMuted, fontSize = 11.sp)
                    if (person.bio.isNotBlank()) Text(person.bio, color = NVInk, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(7.dp))
                    val mine = c.user?.id == person.id
                    if (mine) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            NVButton(c.t("Edit profile", "پروفائل ایڈٹ"), Modifier.weight(1f), primary = true, icon = NVIcons.Edit) { c.route = V95Route.SETTINGS }
                            NVButton(c.t("Settings", "ترتیبات"), Modifier.weight(1f), icon = NVIcons.Gear) { c.route = V95Route.SETTINGS }
                        }
                    } else {
                        NVButton(if (person.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), primary = !person.followed, icon = NVIcons.Follow) {
                            if (c.user == null) c.route = V95Route.AUTH else c.follow(person)
                        }
                    }
                }
            }
        }
        item {
            val details = listOf(
                NVIcons.Pin to person.village,
                NVIcons.Pin to person.area,
                NVIcons.Map to person.city,
                NVIcons.Home to person.hometown,
                NVIcons.User to person.gender,
                NVIcons.Heart to person.relationshipStatus,
                NVIcons.Info to person.work,
                NVIcons.Info to person.school,
                NVIcons.Phone to if (person.showPhone) person.phone else "",
                NVIcons.Message to if (person.showEmail) person.email else ""
            ).filter { it.second.isNotBlank() }

            if (details.isNotEmpty()) {
                NVCard(radius = 22.dp, padding = 12.dp) {
                    details.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { (icon, value) ->
                                Row(
                                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.54f)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(painterResource(icon), null, Modifier.size(25.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(value, color = NVInk, fontSize = 9.5.sp, maxLines = 2)
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NVButton(c.t("Followers", "فالوورز"), Modifier.weight(1f)) { c.openRelations(person, "followers") }
                NVButton(c.t("Following", "فالوونگ"), Modifier.weight(1f)) { c.openRelations(person, "following") }
            }
        }
    }
}

@Composable
internal fun NativeShopDetail(c: V95Controller) {
    val shop = c.selectedShop
    if (shop == null) {
        NVEmpty(c.t("Shop unavailable", "دکان دستیاب نہیں"))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            NVCard(radius = 28.dp, padding = 0.dp) {
                Box(
                    Modifier.fillMaxWidth().height(145.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0x596BD3FF), Color(0x4DA580FF), Color(0x45FF78C2))))
                ) {
                    if (!shop.cover.isNullOrBlank()) AsyncImage(shop.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-50).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NVShopAvatar(shop, 90.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(shop.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(shop.category, color = NVMuted, fontSize = 11.sp)
                    if (shop.description.isNotBlank()) Text(shop.description, color = NVInk, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        NVButton(if (shop.followed) c.t("Following", "فالوونگ") else c.t("Follow", "فالو"), Modifier.weight(1f), primary = !shop.followed, icon = NVIcons.Follow) {
                            if (c.user == null) c.route = V95Route.AUTH else c.toggleShop(shop)
                        }
                        NVButton(c.t("Map", "نقشہ"), Modifier.weight(1f), icon = NVIcons.Map) { c.route = V95Route.MAP }
                    }
                }
            }
        }
        item {
            NVCard(radius = 22.dp, padding = 12.dp) {
                val details = listOf(
                    c.t("Village", "گاؤں") to shop.village,
                    c.t("Area", "علاقہ") to shop.area,
                    c.t("City", "شہر") to shop.city,
                    c.t("Phone", "فون") to shop.phone,
                    "WhatsApp" to shop.whatsapp,
                    c.t("Address", "پتہ") to shop.location,
                    c.t("Username", "یوزرنیم") to shop.username
                ).filter { it.second.isNotBlank() }
                details.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = NVMuted, fontSize = 10.sp)
                        Text(value, color = NVInk, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                NVButton(c.t("Followers", "فالوورز"), Modifier.fillMaxWidth()) { c.openShopFollowers(shop) }
            }
        }
    }
}

@Composable
internal fun NativeSaved(c: V95Controller) {
    if (c.user == null) {
        NativeRequireLogin(c)
        return
    }
    LaunchedEffect(Unit) { if (c.saved.isEmpty()) c.loadSaved() }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NVHeading(c.t("Saved", "محفوظ"), c.t("Posts saved to your profile", "آپ کی محفوظ پوسٹس")) }
        if (c.saved.isEmpty() && !c.busy) item { NVEmpty(c.t("Nothing saved yet", "ابھی کچھ محفوظ نہیں")) }
        items(c.saved, key = { it.id }) { NativePostCard(c, it) }
    }
}

@Composable
internal fun NativeRelations(c: V95Controller) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { NVHeading(c.relationTitle) }
        items(c.relationUsers, key = { it.id }) { person ->
            NVCard(radius = 20.dp, padding = 10.dp) {
                Row(Modifier.fillMaxWidth().clickable { c.openProfile(person) }, verticalAlignment = Alignment.CenterVertically) {
                    NVAvatar(person, 46.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(person.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                        Text("@" + person.username, color = NVMuted, fontSize = 9.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NativePostDetail(c: V95Controller) {
    val post = c.selectedPost
    if (post == null) {
        NVEmpty(c.t("Post unavailable", "پوسٹ دستیاب نہیں"))
        return
    }
    var comment by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { NativePostCard(c, post) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NVInput(comment, c.t("Write a comment…", "کمنٹ لکھیں…"), Modifier.weight(1f)) { comment = it }
                NVIconButton(NVIcons.Send, size = 44.dp, iconSize = 31.dp) {
                    if (comment.isNotBlank()) c.addPostComment(comment) { comment = "" }
                }
            }
        }
        items(c.postCommentsList, key = { it.id }) { NativeCommentCard(it) }
    }
}

@Composable
private fun NativeCommentCard(item: Comment) {
    NVCard(radius = 18.dp, padding = 9.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NVAvatar(item.user, 34.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(item.user.name, color = NVInk, fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                Text(item.createdAt, color = NVMuted, fontSize = 8.sp)
            }
            if (item.likes > 0) Text(item.likes.toString(), color = NVMuted, fontSize = 8.sp)
        }
        Text(item.text, color = NVInk, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun NativeSmallAction(text: String, icon: Int, onClick: () -> Unit) {
    Column(Modifier.height(42.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(icon), null, Modifier.size(23.dp))
        Text(text, color = NVPurple, fontWeight = FontWeight.Black, fontSize = 8.2.sp, maxLines = 1)
    }
}

@Composable
private fun NativeRequireLogin(c: V95Controller) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        NVCard {
            Text(c.t("Login required", "لاگ اِن درکار ہے"), color = NVInk, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(c.t("Login to use this feature.", "یہ فیچر استعمال کرنے کے لیے لاگ اِن کریں۔"), color = NVMuted, fontSize = 11.sp)
            NVButton(c.t("Login / Register", "لاگ اِن / رجسٹریشن"), Modifier.fillMaxWidth(), primary = true) { c.route = V95Route.AUTH }
        }
    }
}

private fun nativeVoteRemaining(raw: String, nowMs: Long, c: V95Controller): String {
    if (raw.isBlank()) return c.t("Live", "لائیو")
    val end = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    val remaining = Duration.between(Instant.ofEpochMilli(nowMs), end)
    if (remaining.isNegative || remaining.isZero) return c.t("Ended", "ختم")
    val total = remaining.seconds
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
