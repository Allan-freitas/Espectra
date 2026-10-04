package br.com.espectra.ui.photos

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.AlbumCard
import br.com.espectra.data.AlbumComment
import br.com.espectra.data.AlbumDetails
import br.com.espectra.data.AlbumPhoto
import br.com.espectra.data.AlbumVisibility
import br.com.espectra.data.ApiClient
import br.com.espectra.data.CreateAlbumRequest
import br.com.espectra.data.CreateCommentRequest
import br.com.espectra.data.Friend
import br.com.espectra.data.ShareAlbumRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/** Foto já lida do picker (a tela converte Uri -> bytes). */
data class PickedPhoto(
    val name: String,
    val mime: String,
    val bytes: ByteArray,
)

data class AlbumsUiState(
    val albums: List<AlbumCard> = emptyList(),
    val loading: Boolean = false,
    val selected: AlbumDetails? = null,
    val albumLoading: Boolean = false,
    val uploading: Boolean = false,
    val viewerUrl: String? = null,
    val deleteAlbumTarget: AlbumCard? = null,
    val deletingAlbum: Boolean = false,
    val deletePhotoTarget: AlbumPhoto? = null,
    val deletingPhoto: Boolean = false,
    // criar
    val showCreate: Boolean = false,
    val createTitle: String = "",
    val createVisibility: AlbumVisibility = AlbumVisibility.FriendsOnly,
    val createAllowedIds: Set<String> = emptySet(),
    val createFriends: List<Friend> = emptyList(),
    val createFriendsSearch: String = "",
    val loadingCreateFriends: Boolean = false,
    val creating: Boolean = false,
    // comentários
    val commentsAlbum: AlbumCard? = null,
    val comments: List<AlbumComment> = emptyList(),
    val loadingComments: Boolean = false,
    val commentText: String = "",
    val submittingComment: Boolean = false,
    // compartilhar
    val shareAlbum: AlbumCard? = null,
    val shareFriends: List<Friend> = emptyList(),
    val shareSearch: String = "",
    val loadingShareFriends: Boolean = false,
    val selectedShareFriendId: String? = null,
    val sharing: Boolean = false,
    val message: String? = null,
)

/** Espelha photos.component (álbuns próprios; leitura + CRUD + upload + share). */
class AlbumsViewModel : ViewModel() {

    var uiState = mutableStateOf(AlbumsUiState())
        private set

    init {
        loadAlbums()
    }

    private fun update(transform: (AlbumsUiState) -> AlbumsUiState) {
        uiState.value = transform(uiState.value)
    }

    fun consumeMessage() = update { it.copy(message = null) }

    fun loadAlbums() {
        viewModelScope.launch {
            update { it.copy(loading = true) }
            val result = runCatching { ApiClient.albumApi.getMine() }
            update {
                it.copy(
                    albums = result.getOrDefault(emptyList()),
                    loading = false,
                    message = result.exceptionOrNull()?.let { "Falha ao carregar álbuns." },
                )
            }
        }
    }

    // ---------- criar ----------

    fun openCreate() {
        update {
            it.copy(
                showCreate = true, createTitle = "",
                createVisibility = AlbumVisibility.FriendsOnly, createAllowedIds = emptySet(),
                createFriendsSearch = "",
            )
        }
        loadFriendsForCreate()
    }

    fun closeCreate() {
        if (uiState.value.creating) return
        update { it.copy(showCreate = false) }
    }

    fun onCreateTitle(v: String) = update { it.copy(createTitle = v) }
    fun onCreateVisibility(v: AlbumVisibility) = update { it.copy(createVisibility = v) }
    fun onCreateFriendsSearch(v: String) = update { it.copy(createFriendsSearch = v) }

    fun filteredCreateFriends(): List<Friend> {
        val q = uiState.value.createFriendsSearch.trim().lowercase()
        if (q.isEmpty()) return uiState.value.createFriends
        return uiState.value.createFriends.filter { (it.name ?: "").lowercase().contains(q) }
    }

    fun toggleCreateAllowedFriend(id: String) {
        update {
            val set = it.createAllowedIds.toMutableSet()
            if (!set.add(id)) set.remove(id)
            it.copy(createAllowedIds = set)
        }
    }

    private fun loadFriendsForCreate() {
        viewModelScope.launch {
            update { it.copy(loadingCreateFriends = true) }
            val friends = runCatching { ApiClient.socialApi.getFriends() }.getOrDefault(emptyList())
            update { it.copy(createFriends = friends, loadingCreateFriends = false) }
        }
    }

    fun createAlbum() {
        val s = uiState.value
        val title = s.createTitle.trim()
        if (title.isEmpty() || s.creating) return
        if (s.createVisibility == AlbumVisibility.SpecificFriends && s.createAllowedIds.isEmpty()) {
            update { it.copy(message = "Selecione ao menos um amigo.") }
            return
        }
        viewModelScope.launch {
            update { it.copy(creating = true) }
            try {
                ApiClient.albumApi.create(
                    CreateAlbumRequest(
                        title = title,
                        visibility = s.createVisibility.value,
                        allowedFriendIds = if (s.createVisibility == AlbumVisibility.SpecificFriends) s.createAllowedIds.toList() else emptyList(),
                    ),
                )
                update { it.copy(creating = false, showCreate = false, message = "Álbum criado!") }
                loadAlbums()
            } catch (e: Exception) {
                update { it.copy(creating = false, message = apiMessage(e) ?: "Erro ao criar álbum.") }
            }
        }
    }

    // ---------- detalhes / upload ----------

    fun openAlbum(album: AlbumCard) {
        viewModelScope.launch {
            update { it.copy(albumLoading = true, viewerUrl = null) }
            val details = runCatching { ApiClient.albumApi.getById(album.id) }.getOrNull()
            update { it.copy(selected = details, albumLoading = false) }
        }
    }

    fun closeAlbum() = update { it.copy(selected = null, viewerUrl = null) }

    fun openViewer(url: String) = update { it.copy(viewerUrl = url) }
    fun closeViewer() = update { it.copy(viewerUrl = null) }

    fun uploadPhotos(photos: List<PickedPhoto>) {
        val album = uiState.value.selected ?: return
        if (photos.isEmpty() || uiState.value.uploading) return
        viewModelScope.launch {
            update { it.copy(uploading = true) }
            try {
                val parts = photos.map { photo ->
                    // OkHttp 3.x (transitivo do Retrofit 2.11): API RequestBody.create / MediaType.parse.
                    val mediaType = MediaType.parse(photo.mime.ifBlank { "image/*" })
                    val body = RequestBody.create(mediaType, photo.bytes)
                    MultipartBody.Part.createFormData("files", photo.name, body)
                }
                val res = ApiClient.albumApi.uploadPhotos(album.id, parts)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(uploading = false, message = "Fotos enviadas!") }
                refreshSelected(album.id)
                loadAlbums()
            } catch (e: Exception) {
                update { it.copy(uploading = false, message = apiMessage(e) ?: "Erro ao enviar fotos.") }
            }
        }
    }

    private suspend fun refreshSelected(albumId: String) {
        val details = runCatching { ApiClient.albumApi.getById(albumId) }.getOrNull()
        if (details != null) update { it.copy(selected = details) }
    }

    // ---------- excluir ----------

    fun askDeleteAlbum(album: AlbumCard) = update { it.copy(deleteAlbumTarget = album) }
    fun cancelDeleteAlbum() {
        if (uiState.value.deletingAlbum) return
        update { it.copy(deleteAlbumTarget = null) }
    }

    fun confirmDeleteAlbum() {
        val album = uiState.value.deleteAlbumTarget ?: return
        viewModelScope.launch {
            update { it.copy(deletingAlbum = true) }
            try {
                val res = ApiClient.albumApi.delete(album.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(deletingAlbum = false, deleteAlbumTarget = null, selected = null, message = "Álbum excluído.") }
                loadAlbums()
            } catch (e: Exception) {
                update { it.copy(deletingAlbum = false, message = apiMessage(e) ?: "Erro ao excluir.") }
            }
        }
    }

    fun askDeletePhoto(photo: AlbumPhoto) = update { it.copy(deletePhotoTarget = photo) }
    fun cancelDeletePhoto() {
        if (uiState.value.deletingPhoto) return
        update { it.copy(deletePhotoTarget = null) }
    }

    fun confirmDeletePhoto() {
        val photo = uiState.value.deletePhotoTarget ?: return
        val albumId = uiState.value.selected?.id ?: return
        viewModelScope.launch {
            update { it.copy(deletingPhoto = true) }
            try {
                val res = ApiClient.albumApi.deletePhoto(photo.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(deletingPhoto = false, deletePhotoTarget = null, message = "Foto excluída.") }
                refreshSelected(albumId)
                loadAlbums()
            } catch (e: Exception) {
                update { it.copy(deletingPhoto = false, message = apiMessage(e) ?: "Erro ao excluir foto.") }
            }
        }
    }

    // ---------- comentários ----------

    fun openComments(album: AlbumCard) {
        update { it.copy(commentsAlbum = album, comments = emptyList(), commentText = "", loadingComments = true) }
        viewModelScope.launch {
            val comments = runCatching { ApiClient.albumApi.getComments(album.id) }.getOrDefault(emptyList())
            update { it.copy(comments = comments, loadingComments = false) }
        }
    }

    fun closeComments() = update { it.copy(commentsAlbum = null, comments = emptyList()) }
    fun onCommentText(v: String) = update { it.copy(commentText = v) }

    fun postComment() {
        val album = uiState.value.commentsAlbum ?: return
        val content = uiState.value.commentText.trim()
        if (content.isEmpty() || uiState.value.submittingComment) return
        viewModelScope.launch {
            update { it.copy(submittingComment = true) }
            try {
                val comment = ApiClient.albumApi.createComment(album.id, CreateCommentRequest(content))
                update {
                    it.copy(
                        comments = it.comments + comment, commentText = "",
                        submittingComment = false, message = "Comentário publicado!",
                    )
                }
                loadAlbums()
            } catch (e: Exception) {
                update { it.copy(submittingComment = false, message = apiMessage(e) ?: "Falha ao comentar.") }
            }
        }
    }

    // ---------- compartilhar ----------

    fun openShare(album: AlbumCard) {
        update {
            it.copy(
                shareAlbum = album, selectedShareFriendId = null,
                shareSearch = "", shareFriends = emptyList(), loadingShareFriends = true,
            )
        }
        viewModelScope.launch {
            val all = runCatching { ApiClient.socialApi.getFriends() }.getOrDefault(emptyList())
            val filtered = if (album.visibility == AlbumVisibility.SpecificFriends.value &&
                !album.allowedFriendIds.isNullOrEmpty()
            ) {
                all.filter { f -> album.allowedFriendIds.contains(f.id) }
            } else {
                all
            }
            update { it.copy(shareFriends = filtered, loadingShareFriends = false) }
        }
    }

    fun closeShare() {
        if (uiState.value.sharing) return
        update { it.copy(shareAlbum = null, selectedShareFriendId = null) }
    }

    fun onShareSearch(v: String) = update { it.copy(shareSearch = v) }

    fun filteredShareFriends(): List<Friend> {
        val q = uiState.value.shareSearch.trim().lowercase()
        if (q.isEmpty()) return uiState.value.shareFriends
        return uiState.value.shareFriends.filter { (it.name ?: "").lowercase().contains(q) }
    }

    fun selectShareFriend(id: String) {
        update { it.copy(selectedShareFriendId = if (it.selectedShareFriendId == id) null else id) }
    }

    fun confirmShare() {
        val album = uiState.value.shareAlbum ?: return
        val friendId = uiState.value.selectedShareFriendId ?: return
        viewModelScope.launch {
            update { it.copy(sharing = true) }
            try {
                ApiClient.albumApi.share(album.id, ShareAlbumRequest(friendId))
                update { it.copy(sharing = false, shareAlbum = null, selectedShareFriendId = null, message = "Álbum compartilhado!") }
                loadAlbums()
            } catch (e: Exception) {
                update { it.copy(sharing = false, message = apiMessage(e) ?: "Falha ao compartilhar.") }
            }
        }
    }

    private fun apiMessage(e: Exception): String? {
        if (e is IOException) return "Sem conexão. Verifique sua internet."
        val http = e as? HttpException ?: return e.message
        return try {
            val body = http.response()?.errorBody()?.string() ?: return null
            val json = JSONObject(body)
            if (json.has("errors")) {
                val errors = json.getJSONObject("errors")
                val parts = mutableListOf<String>()
                for (key in errors.keys()) {
                    val arr = errors.getJSONArray(key)
                    for (i in 0 until arr.length()) parts += arr.getString(i)
                }
                parts.joinToString(" ").takeIf { it.isNotBlank() }
            } else {
                json.optString("message").takeIf { it.isNotBlank() }
            }
        } catch (e2: Exception) {
            null
        }
    }
}

class AlbumsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumsViewModel() as T
    }
}
