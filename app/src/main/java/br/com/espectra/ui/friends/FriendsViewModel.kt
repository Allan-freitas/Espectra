package br.com.espectra.ui.friends

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.Friend
import br.com.espectra.data.FriendRequestDto
import br.com.espectra.data.UserSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.text.Normalizer

enum class FriendsTab { FRIENDS, INCOMING, SENT }

data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val friends: List<Friend> = emptyList(),
    val incoming: List<FriendRequestDto> = emptyList(),
    val sent: List<FriendRequestDto> = emptyList(),
    val search: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    /** Resultado da busca global (qualquer usuário cadastrado). */
    val globalResults: List<UserSearchResult> = emptyList(),
    val searchingGlobal: Boolean = false,
    /** IDs com pedido já enviado nesta sessão. */
    val requestedIds: Set<String> = emptySet(),
)

/** Espelha friends.component + friend-requests (listar, buscar, aceitar, recusar, cancelar, remover). */
class FriendsViewModel : ViewModel() {

    var uiState = mutableStateOf(FriendsUiState())
        private set

    init {
        refresh()
    }

    private fun update(transform: (FriendsUiState) -> FriendsUiState) {
        uiState.value = transform(uiState.value)
    }

    fun consumeMessage() = update { it.copy(message = null) }

    fun setTab(tab: FriendsTab) = update { it.copy(tab = tab) }

    fun onSearch(v: String) {
        update { it.copy(search = v) }
        scheduleGlobalSearch(v)
    }

    private var searchJob: Job? = null

    /** Busca global com debounce (só a partir de 2 letras). */
    private fun scheduleGlobalSearch(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            update { it.copy(globalResults = emptyList(), searchingGlobal = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            update { it.copy(searchingGlobal = true) }
            val results = runCatching { ApiClient.socialApi.searchUsers(q) }.getOrDefault(emptyList())
            update { it.copy(globalResults = results, searchingGlobal = false) }
        }
    }

    /** Minúsculo + sem acentos: "joao" acha "João". */
    private fun norm(s: String): String {
        val nfd = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
    }

    fun refresh() {
        viewModelScope.launch {
            update { it.copy(loading = true) }
            val friends = runCatching { ApiClient.socialApi.getFriends() }
            val incoming = runCatching { ApiClient.socialApi.getIncomingRequests() }
            val sent = runCatching { ApiClient.socialApi.getSentRequests() }
            val failed = friends.isFailure && incoming.isFailure && sent.isFailure
            update {
                it.copy(
                    friends = friends.getOrDefault(emptyList()),
                    incoming = incoming.getOrDefault(emptyList()),
                    sent = sent.getOrDefault(emptyList()),
                    loading = false,
                    message = if (failed) "Falha ao carregar. Toque para tentar de novo." else it.message,
                )
            }
        }
    }

    fun filteredFriends(): List<Friend> {
        val q = norm(uiState.value.search)
        if (q.isEmpty()) return uiState.value.friends
        return uiState.value.friends.filter { norm(it.name ?: "").contains(q) }
    }

    /** Envia pedido para um usuário da busca global. */
    fun sendRequest(userId: String) {
        if (uiState.value.requestedIds.contains(userId)) return
        viewModelScope.launch {
            try {
                val res = ApiClient.socialApi.sendFriendRequest(
                    br.com.espectra.data.SendFriendRequest(userId),
                )
                if (!res.isSuccessful) throw HttpException(res)
                update {
                    it.copy(
                        requestedIds = it.requestedIds + userId,
                        message = "Pedido de amizade enviado!",
                    )
                }
                val sent = runCatching { ApiClient.socialApi.getSentRequests() }.getOrDefault(uiState.value.sent)
                update { it.copy(sent = sent) }
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao enviar pedido.") }
            }
        }
    }

    fun accept(request: FriendRequestDto) {
        viewModelScope.launch {
            try {
                val res = ApiClient.socialApi.acceptRequest(request.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(incoming = it.incoming.filter { r -> r.id != request.id }, message = "Amizade aceita!") }
                refreshFriendsOnly()
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao aceitar.") }
            }
        }
    }

    fun reject(request: FriendRequestDto) {
        viewModelScope.launch {
            try {
                val res = ApiClient.socialApi.rejectRequest(request.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(incoming = it.incoming.filter { r -> r.id != request.id }, message = "Pedido recusado.") }
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao recusar.") }
            }
        }
    }

    fun cancel(request: FriendRequestDto) {
        viewModelScope.launch {
            try {
                val res = ApiClient.socialApi.cancelRequest(request.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(sent = it.sent.filter { r -> r.id != request.id }, message = "Pedido cancelado.") }
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao cancelar.") }
            }
        }
    }

    fun removeFriend(friend: Friend) {
        viewModelScope.launch {
            try {
                val res = ApiClient.socialApi.removeFriend(friend.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(friends = it.friends.filter { f -> f.id != friend.id }, message = "Amigo removido.") }
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao remover.") }
            }
        }
    }

    private suspend fun refreshFriendsOnly() {
        val friends = runCatching { ApiClient.socialApi.getFriends() }.getOrDefault(uiState.value.friends)
        update { it.copy(friends = friends) }
    }

    private fun apiMessage(e: Exception): String? {
        if (e is IOException) return "Sem conexão. Verifique sua internet."
        val http = e as? HttpException ?: return e.message
        return try {
            val body = http.response()?.errorBody()?.string() ?: return null
            JSONObject(body).optString("message").takeIf { it.isNotBlank() }
        } catch (e2: Exception) {
            null
        }
    }
}

class FriendsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FriendsViewModel() as T
    }
}
