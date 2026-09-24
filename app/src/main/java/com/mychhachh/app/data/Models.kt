package com.mychhachh.app.data

import org.json.JSONArray
import org.json.JSONObject

private const val BASE = "https://chhachh.pages.dev"

fun mediaUrl(raw: String?): String? {
    val v = raw.orEmpty().trim()
    if (v.isBlank() || v == "null") return null
    return if (v.startsWith("http://") || v.startsWith("https://")) v else BASE + if (v.startsWith("/")) v else "/$v"
}

data class User(
    val id: Long = 0,
    val name: String = "",
    val username: String = "",
    val avatar: String? = null,
    val cover: String? = null,
    val city: String = "",
    val area: String = "",
    val village: String = "",
    val bio: String = "",
    val hometown: String = "",
    val gender: String = "",
    val relationshipStatus: String = "",
    val work: String = "",
    val school: String = "",
    val socialFacebook: String = "",
    val socialInstagram: String = "",
    val socialYoutube: String = "",
    val socialWebsite: String = "",
    val email: String = "",
    val phone: String = "",
    val showEmail: Boolean = false,
    val showPhone: Boolean = false,
    val showLocation: Boolean = false,
    val hideFollowers: Boolean = false,
    val acceptMessages: Boolean = true,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val verified: Boolean = false,
    val isAdmin: Boolean = false,
    val profileVisibility: String = "public",
    val followed: Boolean = false
)

data class Post(
    val id: Long,
    val user: User,
    val text: String,
    val photo: String?,
    val video: String?,
    val createdAt: String,
    val privacy: String,
    val likes: Int,
    val comments: Int,
    val views: Int,
    val shares: Int,
    val liked: Boolean,
    val saved: Boolean,
    val feeling: String?,
    val checkin: String?,
    val checkinLat: Double? = null,
    val checkinLng: Double? = null,
    val shopId: Long = 0L,
    val voteId: Long = 0L,
    val voteLeftName: String = "",
    val voteRightName: String = "",
    val reactionLike: Int = 0,
    val reactionLove: Int = 0,
    val reactionHaha: Int = 0,
    val reactionWow: Int = 0,
    val reactionSad: Int = 0,
    val reactionAngry: Int = 0,
    val reactionTotal: Int = likes,
    val myReaction: String = ""
)

data class CheckinPlace(
    val name: String,
    val lat: Double,
    val lng: Double
)

data class Comment(
    val id: Long,
    val user: User,
    val text: String,
    val parentId: Long,
    val likes: Int,
    val liked: Boolean,
    val createdAt: String
)

data class Shop(
    val id: Long,
    val userId: Long,
    val name: String,
    val username: String,
    val category: String,
    val description: String,
    val photo: String?,
    val cover: String?,
    val city: String,
    val village: String,
    val area: String,
    val location: String = "",
    val phone: String,
    val whatsapp: String,
    val locationUrl: String = "",
    val followers: Int = 0,
    val products: Int = 0,
    val followed: Boolean = false,
    val promoted: Boolean = false
)

data class Conversation(val user: User, val preview: String, val createdAt: String, val unread: Boolean)
data class MessageGroup(val id: Long, val name: String, val memberCount: Int, val preview: String, val createdAt: String)
data class Message(val id: Long, val senderId: Long, val receiverId: Long, val text: String, val photo: String?, val audio: String?, val createdAt: String, val locationLat: Double? = null, val locationLng: Double? = null)
data class Notice(val id: Long, val actor: User?, val text: String, val type: String, val createdAt: String, val read: Boolean)
data class Announcement(val id: Long, val author: User?, val text: String, val photo: String?, val audio: String?, val type: String, val likes: Int, val comments: Int, val liked: Boolean, val createdAt: String)
data class Vote(
    val id: Long,
    val title: String,
    val status: String,
    val user1: User?,
    val user2: User?,
    val votes1: Int,
    val votes2: Int,
    val createdAt: String,
    val leftUserId: Long = user1?.id ?: 0L,
    val rightUserId: Long = user2?.id ?: 0L,
    val leftText: String = "",
    val rightText: String = "",
    val myChoice: Long = 0L,
    val winnerUserId: Long = 0L,
    val resultRevealed: Boolean = true,
    val durationHours: Int = 24,
    val startsAt: String = "",
    val endsAt: String = "",
    val updatedAt: String = "",
    val endedAt: String = "",
    val tie: Boolean = false
)

data class Bootstrap(val user: User?, val unread: Int, val announcementUnread: Int, val features: JSONObject)

data class SearchBundle(val users: List<User>, val shops: List<Shop>, val posts: List<Post>)

fun JSONObject.toUser(): User = User(
    id = optLong("id"),
    name = optString("name", optString("full_name", "User")),
    username = optString("username", ""),
    avatar = mediaUrl(optString("avatar", optString("photo", ""))),
    cover = mediaUrl(optString("cover_photo", "")),
    city = optString("city", ""), area = optString("area", ""), village = optString("village", ""),
    bio = optString("bio", ""),
    hometown = optString("hometown", ""),
    gender = optString("gender", ""),
    relationshipStatus = optString("relationship_status", ""),
    work = optString("work", ""),
    school = optString("school", ""),
    socialFacebook = optString("social_facebook", ""),
    socialInstagram = optString("social_instagram", ""),
    socialYoutube = optString("social_youtube", ""),
    socialWebsite = optString("social_website", ""),
    email = optString("email", ""),
    phone = optString("phone", ""),
    showEmail = optBoolean("show_email", false),
    showPhone = optBoolean("show_phone", false),
    showLocation = optBoolean("show_location", false),
    hideFollowers = optBoolean("hide_followers", false),
    acceptMessages = optBoolean("accept_messages", true),
    locationLat = optString("location_lat", "").toDoubleOrNull(),
    locationLng = optString("location_lng", "").toDoubleOrNull(),
    verified = optBoolean("verified", false), isAdmin = optBoolean("is_admin", false),
    profileVisibility = optString("profile_visibility", "public"),
    followed = optBoolean("following", optBoolean("followed", false))
)

fun JSONObject.toPost(): Post {
    val a = optJSONObject("author") ?: optJSONObject("user") ?: JSONObject().apply {
        put("id", optLong("user_id")); put("name", optString("user_name", "User")); put("username", optString("username", ""))
    }
    return Post(
        id = optLong("id"), user = a.toUser(), text = optString("text", ""),
        photo = mediaUrl(optString("photo", "")), video = mediaUrl(optString("video", "")),
        createdAt = optString("created_at", ""), privacy = optString("privacy", "public"),
        likes = optInt("likes", 0), comments = optInt("comments", 0), views = optInt("views", 0), shares = optInt("shares", 0),
        liked = optBoolean("liked", false), saved = optBoolean("saved", false),
        feeling = optString("feeling", "").takeIf { it.isNotBlank() && it != "null" },
        checkin = optString("checkin", "").takeIf { it.isNotBlank() && it != "null" },
        checkinLat = optString("checkin_lat", "").toDoubleOrNull(),
        checkinLng = optString("checkin_lng", "").toDoubleOrNull(),
        shopId = optLong("shop_id", 0L),
        voteId = optLong("vote_id", 0L),
        voteLeftName = optString("vote_left_name", ""),
        voteRightName = optString("vote_right_name", ""),
        reactionLike = optJSONObject("reactions")?.optInt("like", 0) ?: 0,
        reactionLove = optJSONObject("reactions")?.optInt("love", 0) ?: 0,
        reactionHaha = optJSONObject("reactions")?.optInt("haha", 0) ?: 0,
        reactionWow = optJSONObject("reactions")?.optInt("wow", 0) ?: 0,
        reactionSad = optJSONObject("reactions")?.optInt("sad", 0) ?: 0,
        reactionAngry = optJSONObject("reactions")?.optInt("angry", 0) ?: 0,
        reactionTotal = optJSONObject("reactions")?.optInt("total", optInt("likes", 0)) ?: optInt("likes", 0),
        myReaction = optString("my_reaction", "")
    )
}

fun JSONObject.toShop(): Shop = Shop(
    id = optLong("id"), userId = optLong("user_id"), name = optString("name", "Shop"), username = optString("username", ""),
    category = optString("category", ""), description = optString("description", ""), photo = mediaUrl(optString("photo", "")), cover = mediaUrl(optString("cover_photo", "")),
    city = optString("city", ""), village = optString("village", ""), area = optString("area", ""), location = optString("location", ""), phone = optString("phone", ""), whatsapp = optString("whatsapp", ""),
    locationUrl = optString("location_url", optString("map_url", optString("google_maps_url", optString("location_link", "")))),
    followers = optInt("followers", 0),
    products = optInt("products", 0),
    followed = optBoolean("following", optBoolean("followed", false)),
    promoted = optBoolean("promoted", optInt("promoted", 0) != 0)
)


fun JSONObject.toComment(): Comment {
    val u = optJSONObject("user") ?: JSONObject().put("id", optLong("user_id")).put("name", "User")
    return Comment(
        id = optLong("id"),
        user = u.toUser(),
        text = optString("text", ""),
        parentId = optLong("parent_id", 0L),
        likes = optInt("likes", 0),
        liked = optBoolean("liked", false),
        createdAt = optString("created_at", "")
    )
}

fun JSONArray.users(): List<User> = (0 until length()).mapNotNull { optJSONObject(it)?.toUser() }
fun JSONArray.posts(): List<Post> = (0 until length()).mapNotNull { optJSONObject(it)?.toPost() }
fun JSONArray.comments(): List<Comment> = (0 until length()).mapNotNull { optJSONObject(it)?.toComment() }
fun JSONArray.shops(): List<Shop> = (0 until length()).mapNotNull { optJSONObject(it)?.toShop() }
