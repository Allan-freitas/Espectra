package br.com.espectra.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espelha FeedService + FriendRequestService + SettingsService do web
 * (mesmas rotas relativas ao /api/).
 */
interface FeedApi {
    @GET("posts/feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): List<PostFeed>

    @POST("posts/{id}/like")
    suspend fun likePost(@Path("id") postId: String): retrofit2.Response<Unit>

    @POST("posts")
    suspend fun createPost(@Body dto: CreatePostRequest): PostFeed

    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") postId: String): retrofit2.Response<Unit>

    @GET("posts/{id}/comments")
    suspend fun getComments(@Path("id") postId: String): List<CommentFeed>

    @POST("posts/{id}/comments")
    suspend fun createComment(
        @Path("id") postId: String,
        @Body dto: CreateCommentRequest,
    ): CommentFeed
}

interface SocialApi {
    @GET("friendrequests/suggestions")
    suspend fun getSuggestions(): List<FriendSuggestion>

    @GET("activityfeed")
    suspend fun getActivityFeed(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): List<ActivityFeedItem>

    @GET("friendrequests/upcoming-birthday")
    suspend fun getUpcomingBirthday(): UpcomingBirthday?

    @GET("friendrequests/friends")
    suspend fun getFriends(): List<Friend>

    @POST("friendrequests")
    suspend fun sendFriendRequest(@Body dto: SendFriendRequest): retrofit2.Response<Unit>

    @GET("friendrequests/incoming")
    suspend fun getIncomingRequests(): List<FriendRequestDto>

    @GET("friendrequests/sent")
    suspend fun getSentRequests(): List<FriendRequestDto>

    @POST("friendrequests/{id}/accept")
    suspend fun acceptRequest(@Path("id") requestId: String): retrofit2.Response<Unit>

    @POST("friendrequests/{id}/reject")
    suspend fun rejectRequest(@Path("id") requestId: String): retrofit2.Response<Unit>

    @DELETE("friendrequests/{id}")
    suspend fun cancelRequest(@Path("id") requestId: String): retrofit2.Response<Unit>

    @DELETE("friendrequests/friends/{friendId}")
    suspend fun removeFriend(@Path("friendId") friendId: String): retrofit2.Response<Unit>

    @GET("settings")
    suspend fun getSettings(): UserSettings
}
