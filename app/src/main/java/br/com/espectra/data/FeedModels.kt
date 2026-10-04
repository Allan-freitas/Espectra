package br.com.espectra.data

/**
 * Espelha os models do web (post-feed.ts, friend-suggestion.ts,
 * activity-feed.ts, upcoming-birthday.ts, friend-request.ts, user-settings.ts).
 * A API serializa em camelCase — os nomes já batem com o Gson.
 */

enum class PostVisibility(val value: Int) {
    Public(0),
    FriendsOnly(1),
    SpecificFriends(2),
}

data class CreatePostRequest(
    val content: String,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val visibility: Int = 0,
    val allowedFriendIds: List<String> = emptyList(),
)

data class PostFeed(
    val id: String,
    val userId: String,
    val userName: String,
    val userUsername: String,
    val userAvatarUrl: String?,
    val content: String,
    val mediaUrl: String?,
    val mediaType: String?,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByCurrentUser: Boolean,
    val createdAt: String,
    val visibility: Int,
    val allowedFriendIds: List<String>,
)

data class CommentFeed(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val content: String,
    val createdAt: String,
)

data class CreateCommentRequest(
    val content: String,
)

data class FriendSuggestion(
    val userId: String,
    val name: String,
    val lastName: String,
    val avatarUrl: String?,
    val mutualFriendsCount: Int,
)

data class ActivityFeedItem(
    val id: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val activityType: String,
    val description: String,
    val referenceId: String?,
    val referenceType: String?,
    val referenceContent: String?,
    val createdAt: String,
)

data class UpcomingBirthday(
    val userId: String,
    val name: String,
    val lastName: String,
    val avatarUrl: String?,
    val birthday: String,
    val daysUntilBirthday: Int,
)

data class Friend(
    val id: String,
    val name: String? = null,
    val avatar: String? = null,
    val mutualInfo: String? = null,
    val friendsSince: String? = null,
)

data class SendFriendRequest(
    val receiverId: String,
)

data class UserSettings(
    val whoCanFriend: String,
    val whoCanViewPosts: String,
    val notificationsSound: Boolean,
    val notificationsEmail: Boolean,
    val friendsBirthdays: Boolean,
    val chatMessageSound: Boolean,
)
