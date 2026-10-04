package br.com.espectra.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espelha friend-request.ts (pedidos) — campos nuláveis porque o Gson
 * injeta null em propriedades Kotlin não-nulas.
 */
data class FriendRequestDto(
    val id: String,
    val userId: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val mutualInfo: String? = null,
    val accepted: Boolean = false,
    val createdAt: String? = null,
)

/** Espelha user-search.ts: resultado da busca global de usuários. */
data class UserSearchResult(
    val userId: String,
    val name: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val isFriend: Boolean = false,
)

/**
 * Espelha album.ts do web. Visibilidade tem ordem própria:
 * FriendsOnly=0, SpecificFriends=1, Public=2.
 */
enum class AlbumVisibility(val value: Int) {
    FriendsOnly(0),
    SpecificFriends(1),
    Public(2),
}

data class CreateAlbumRequest(
    val title: String,
    val visibility: Int,
    val allowedFriendIds: List<String> = emptyList(),
)

data class AlbumCard(
    val id: String,
    val title: String? = null,
    val updatedAt: String? = null,
    val photoCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val previewUrls: List<String> = emptyList(),
    val visibility: Int = 0,
    val allowedFriendIds: List<String>? = null,
)

data class AlbumPhoto(
    val id: String,
    val url: String? = null,
    val thumbnailUrl: String? = null,
    val uploadedAt: String? = null,
)

data class AlbumDetails(
    val id: String,
    val title: String? = null,
    val updatedAt: String? = null,
    val photoCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val previewUrls: List<String> = emptyList(),
    val visibility: Int = 0,
    val allowedFriendIds: List<String>? = null,
    val photos: List<AlbumPhoto> = emptyList(),
)

data class AlbumComment(
    val id: String,
    val userId: String? = null,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
)

data class ShareAlbumRequest(
    val friendUserId: String,
)

data class ShareAlbumResponse(
    val shareCount: Int = 0,
)

/** Espelha AlbumService do web (mesmas rotas). */
interface AlbumApi {
    @GET("albums")
    suspend fun getMine(): List<AlbumCard>

    @POST("albums")
    suspend fun create(@Body request: CreateAlbumRequest): AlbumDetails

    @GET("albums/{id}")
    suspend fun getById(@Path("id") albumId: String): AlbumDetails

    @Multipart
    @POST("albums/{id}/photos")
    suspend fun uploadPhotos(
        @Path("id") albumId: String,
        @Part files: List<MultipartBody.Part>,
    ): retrofit2.Response<Unit>

    @DELETE("albums/{id}")
    suspend fun delete(@Path("id") albumId: String): retrofit2.Response<Unit>

    @DELETE("photos/{id}")
    suspend fun deletePhoto(@Path("id") photoId: String): retrofit2.Response<Unit>

    @GET("albums/{id}/comments")
    suspend fun getComments(@Path("id") albumId: String): List<AlbumComment>

    @POST("albums/{id}/comments")
    suspend fun createComment(
        @Path("id") albumId: String,
        @Body dto: CreateCommentRequest,
    ): AlbumComment

    @POST("albums/{id}/share")
    suspend fun share(
        @Path("id") albumId: String,
        @Body dto: ShareAlbumRequest,
    ): ShareAlbumResponse
}
