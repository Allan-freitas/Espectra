package br.com.espectra.ui.feed

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ActivityFeedItem
import br.com.espectra.data.ApiClient
import br.com.espectra.data.CommentFeed
import br.com.espectra.data.CreateCommentRequest
import br.com.espectra.data.CreatePostRequest
import br.com.espectra.data.Friend
import br.com.espectra.data.FriendSuggestion
import br.com.espectra.data.PostFeed
import br.com.espectra.data.PostVisibility
import br.com.espectra.data.SendFriendRequest
import br.com.espectra.data.SessionManager
import br.com.espectra.data.UpcomingBirthday
import br.com.espectra.data.WeatherInfo
import br.com.espectra.data.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

enum class ComposerTab { STATUS, MEDIA, BLOG }

data class Feeling(val emoji: String, val label: String)

val ALL_FEELINGS = listOf(
    Feeling("😊", "feliz"),
    Feeling("😇", "abençoado"),
    Feeling("🥰", "amado"),
    Feeling("😞", "triste"),
    Feeling("😍", "adorável"),
    Feeling("🙏", "agradecido"),
    Feeling("😃", "animado"),
    Feeling("😘", "apaixonado"),
    Feeling("😜", "louco"),
    Feeling("😀", "grato"),
    Feeling("😁", "muito feliz"),
    Feeling("🤩", "fantástico"),
    Feeling("🤪", "bobo"),
    Feeling("🎉", "festivo"),
    Feeling("🙂", "maravilhoso"),
    Feeling("😎", "legal"),
    Feeling("😂", "divertido"),
    Feeling("☺️", "relaxado"),
    Feeling("😉", "positivo"),
    Feeling("😌", "tranquilo"),
    Feeling("🌷", "esperançoso"),
    Feeling("😆", "alegre"),
    Feeling("😩", "cansado"),
    Feeling("💪", "motivado"),
    Feeling("😏", "orgulhoso"),
    Feeling("😔", "sozinho"),
    Feeling("🤔", "pensativo"),
    Feeling("👌", "OK"),
)

data class FeedUiState(
    val posts: List<PostFeed> = emptyList(),
    val page: Int = 1,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val posting: Boolean = false,
    val composerTab: ComposerTab = ComposerTab.STATUS,
    val statusText: String = "",
    val mediaDesc: String = "",
    val blogTitle: String = "",
    val blogContent: String = "",
    val showFeelings: Boolean = false,
    val feelingsSearch: String = "",
    val selectedFeeling: Feeling? = null,
    val visibility: PostVisibility = PostVisibility.Public,
    val allowedIds: Set<String> = emptySet(),
    val friends: List<Friend> = emptyList(),
    val friendsSearch: String = "",
    val loadingFriends: Boolean = false,
    val comments: Map<String, List<CommentFeed>> = emptyMap(),
    val expandedPostId: String? = null,
    val commentText: String = "",
    val loadingComments: Boolean = false,
    val deleteTarget: PostFeed? = null,
    val deleting: Boolean = false,
    val suggestions: List<FriendSuggestion> = emptyList(),
    val activities: List<ActivityFeedItem> = emptyList(),
    val birthday: UpcomingBirthday? = null,
    val weather: WeatherInfo? = null,
    val message: String? = null,
    val currentUserId: String? = null,
    val avatarPath: String? = null,
)

/** Espelha FeedComponent do web (mesmas regras e fluxos). */
class FeedViewModel(
    private val session: SessionManager,
) : ViewModel() {

    var uiState = mutableStateOf(
        FeedUiState(
            currentUserId = session.getUserId(),
            avatarPath = session.getUserAvatar(),
        ),
    )
        private set

    init {
        refresh()
    }

    private fun update(transform: (FeedUiState) -> FeedUiState) {
        uiState.value = transform(uiState.value)
    }

    fun consumeMessage() = update { it.copy(message = null) }

    // ---------- carga ----------

    fun refresh() {
        viewModelScope.launch {
            update { it.copy(refreshing = true, page = 1) }
            try {
                val feed = ApiClient.feedApi.getFeed(1, 10)
                update { it.copy(posts = feed, page = 1, refreshing = false) }
            } catch (e: Exception) {
                update { it.copy(refreshing = false, message = "Falha ao carregar o feed.") }
            }
            launch { loadSecondary() }
            launch { loadDefaultVisibility() }
        }
    }

    private fun loadSecondary() {
        viewModelScope.launch {
            val suggestions = async { runCatching { ApiClient.socialApi.getSuggestions() }.getOrDefault(emptyList()) }
            val activities = async { runCatching { ApiClient.socialApi.getActivityFeed() }.getOrDefault(emptyList()) }
            val birthday = async { runCatching { ApiClient.socialApi.getUpcomingBirthday() }.getOrNull() }
            val weather = async { WeatherRepository.getForecast() }
            val suggestionsList = suggestions.await()
            val activitiesList = activities.await()
            val birthdayValue = birthday.await()
            val weatherInfo = weather.await()
            update {
                it.copy(
                    suggestions = suggestionsList,
                    activities = activitiesList,
                    birthday = birthdayValue,
                    weather = weatherInfo,
                )
            }
        }
    }

    private fun loadDefaultVisibility() {
        viewModelScope.launch {
            val settings = runCatching { ApiClient.socialApi.getSettings() }.getOrNull() ?: return@launch
            val visibility = when (settings.whoCanViewPosts) {
                "friendsOnly" -> PostVisibility.FriendsOnly
                else -> PostVisibility.Public
            }
            update { it.copy(visibility = visibility) }
        }
    }

    fun loadMore() {
        val s = uiState.value
        if (s.loading || s.refreshing) return
        viewModelScope.launch {
            update { it.copy(loading = true) }
            try {
                val next = s.page + 1
                val more = ApiClient.feedApi.getFeed(next, 10)
                update { it.copy(posts = s.posts + more, page = next, loading = false) }
            } catch (e: Exception) {
                update { it.copy(loading = false) }
            }
        }
    }

    // ---------- composer ----------

    fun setTab(tab: ComposerTab) = update { it.copy(composerTab = tab) }
    fun onStatusChange(v: String) = update { it.copy(statusText = v) }
    fun onMediaDescChange(v: String) = update { it.copy(mediaDesc = v) }
    fun onBlogTitleChange(v: String) = update { it.copy(blogTitle = v) }
    fun onBlogContentChange(v: String) = update { it.copy(blogContent = v) }

    fun toggleFeelings() = update { it.copy(showFeelings = !it.showFeelings, feelingsSearch = "") }
    fun onFeelingsSearch(v: String) = update { it.copy(feelingsSearch = v) }
    fun selectFeeling(f: Feeling) = update { it.copy(selectedFeeling = f, showFeelings = false) }
    fun clearFeeling() = update { it.copy(selectedFeeling = null) }

    fun filteredFeelings(): List<Feeling> {
        val q = uiState.value.feelingsSearch.trim().lowercase()
        if (q.isEmpty()) return ALL_FEELINGS
        return ALL_FEELINGS.filter { it.label.lowercase().contains(q) }
    }

    fun onVisibilityChange(v: PostVisibility) {
        update { it.copy(visibility = v) }
        if (v == PostVisibility.SpecificFriends && uiState.value.friends.isEmpty()) {
            loadFriends()
        }
    }

    private fun loadFriends() {
        viewModelScope.launch {
            update { it.copy(loadingFriends = true) }
            val friends = runCatching { ApiClient.socialApi.getFriends() }.getOrDefault(emptyList())
            update { it.copy(friends = friends, loadingFriends = false) }
        }
    }

    fun onFriendsSearch(v: String) = update { it.copy(friendsSearch = v) }

    fun filteredFriends(): List<Friend> {
        val q = uiState.value.friendsSearch.trim().lowercase()
        if (q.isEmpty()) return uiState.value.friends
        return uiState.value.friends.filter { (it.name ?: "").lowercase().contains(q) }
    }

    fun toggleAllowedFriend(id: String) {
        update {
            val set = it.allowedIds.toMutableSet()
            if (!set.add(id)) set.remove(id)
            it.copy(allowedIds = set)
        }
    }

    private fun buildCreateDto(content: String, mediaType: String? = null): CreatePostRequest {
        val s = uiState.value
        return CreatePostRequest(
            content = content,
            mediaType = mediaType,
            visibility = s.visibility.value,
            allowedFriendIds = if (s.visibility == PostVisibility.SpecificFriends) s.allowedIds.toList() else emptyList(),
        )
    }

    private fun validateVisibility(): Boolean {
        val s = uiState.value
        if (s.visibility == PostVisibility.SpecificFriends && s.allowedIds.isEmpty()) {
            update { it.copy(message = "Selecione ao menos um amigo para visibilidade Específicos.") }
            return false
        }
        return true
    }

    private fun resetComposer() {
        update {
            it.copy(
                statusText = "", mediaDesc = "", blogTitle = "", blogContent = "",
                selectedFeeling = null, showFeelings = false,
                visibility = PostVisibility.Public, allowedIds = emptySet(), friendsSearch = "",
            )
        }
    }

    fun postStatus() {
        val content = uiState.value.statusText.trim()
        if (content.isEmpty() || !validateVisibility()) return
        val feeling = uiState.value.selectedFeeling
        val withFeeling = if (feeling != null) "$content — está se sentindo ${feeling.label} ${feeling.emoji}" else content
        createPost(buildCreateDto(withFeeling))
    }

    fun postMedia() {
        // Paridade com o web: cria o post com mediaType image (sem upload real).
        val content = uiState.value.mediaDesc.trim()
        if (content.isEmpty() || !validateVisibility()) return
        createPost(buildCreateDto(content, "image"))
    }

    fun publishBlog() {
        val title = uiState.value.blogTitle.trim()
        val content = uiState.value.blogContent.trim()
        if (title.isEmpty() || content.isEmpty() || !validateVisibility()) return
        createPost(buildCreateDto("**$title**\n\n$content"))
    }

    private fun createPost(dto: CreatePostRequest) {
        viewModelScope.launch {
            update { it.copy(posting = true) }
            try {
                val post = ApiClient.feedApi.createPost(dto)
                update { it.copy(posts = listOf(post) + it.posts, posting = false) }
                resetComposer()
            } catch (e: Exception) {
                update { it.copy(posting = false, message = postErrorMessage(e)) }
            }
        }
    }

    // ---------- likes ----------

    fun toggleLike(post: PostFeed) {
        val liked = !post.isLikedByCurrentUser
        val delta = if (liked) 1 else -1
        update { s ->
            s.copy(posts = s.posts.map {
                if (it.id == post.id) it.copy(isLikedByCurrentUser = liked, likesCount = it.likesCount + delta) else it
            })
        }
        viewModelScope.launch {
            try {
                val res = ApiClient.feedApi.likePost(post.id)
                if (!res.isSuccessful) throw HttpException(res)
            } catch (e: Exception) {
                // rollback otimista
                update { s ->
                    s.copy(posts = s.posts.map {
                        if (it.id == post.id) post else it
                    }, message = apiMessage(e) ?: "Erro ao curtir. Tente novamente.")
                }
            }
        }
    }

    // ---------- comentários ----------

    fun toggleComments(postId: String) {
        val s = uiState.value
        if (s.expandedPostId == postId) {
            update { it.copy(expandedPostId = null) }
            return
        }
        update { it.copy(expandedPostId = postId) }
        if (!s.comments.containsKey(postId)) {
            viewModelScope.launch {
                update { it.copy(loadingComments = true) }
                val comments = runCatching { ApiClient.feedApi.getComments(postId) }.getOrDefault(emptyList())
                update {
                    it.copy(comments = it.comments + (postId to comments), loadingComments = false)
                }
            }
        }
    }

    fun onCommentText(v: String) = update { it.copy(commentText = v) }

    fun postComment(postId: String) {
        val content = uiState.value.commentText.trim()
        if (content.isEmpty()) return
        viewModelScope.launch {
            try {
                val comment = ApiClient.feedApi.createComment(postId, CreateCommentRequest(content))
                update { s ->
                    val list = (s.comments[postId] ?: emptyList()) + comment
                    val posts = s.posts.map {
                        if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it
                    }
                    s.copy(comments = s.comments + (postId to list), posts = posts, commentText = "", message = "Comentário publicado!")
                }
            } catch (e: Exception) {
                val status = (e as? HttpException)?.code()
                update { it.copy(message = if (status == 403) "Você não tem permissão" else apiMessage(e) ?: "Falha ao publicar comentário. Tente novamente.") }
            }
        }
    }

    // ---------- excluir ----------

    fun openDelete(post: PostFeed) = update { it.copy(deleteTarget = post) }
    fun closeDelete() {
        if (uiState.value.deleting) return
        update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val post = uiState.value.deleteTarget ?: return
        if (uiState.value.deleting) return
        viewModelScope.launch {
            update { it.copy(deleting = true) }
            try {
                val res = ApiClient.feedApi.deletePost(post.id)
                if (!res.isSuccessful && res.code() != 404) throw HttpException(res)
                update {
                    it.copy(
                        posts = it.posts.filter { p -> p.id != post.id },
                        deleteTarget = null, deleting = false,
                        expandedPostId = if (it.expandedPostId == post.id) null else it.expandedPostId,
                        message = if (res.code() == 404) "Post não encontrado." else "Post excluído.",
                    )
                }
            } catch (e: Exception) {
                val status = (e as? HttpException)?.code()
                update {
                    it.copy(
                        deleting = false,
                        message = if (status == 403) "Você não tem permissão" else apiMessage(e) ?: "Erro ao excluir post.",
                    )
                }
            }
        }
    }

    // ---------- amizades ----------

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                val res = ApiClient.socialApi.sendFriendRequest(SendFriendRequest(userId))
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(suggestions = it.suggestions.filter { s -> s.userId != userId }, message = "Pedido de amizade enviado!") }
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao enviar pedido.") }
            }
        }
    }

    // ---------- erros ----------

    private fun postErrorMessage(e: Exception): String {
        val status = (e as? HttpException)?.code()
        val api = apiMessage(e)
        return when (status) {
            403 -> api ?: "Você não tem permissão"
            400 -> api ?: "Dados inválidos. Verifique o conteúdo."
            else -> api ?: "Erro ao criar post. Tente novamente."
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

class FeedViewModelFactory(
    private val session: SessionManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeedViewModel(session) as T
    }
}
