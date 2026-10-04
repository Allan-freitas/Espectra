package br.com.espectra.ui.friends

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.Friend
import br.com.espectra.data.FriendRequestDto
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

enum class FriendsTab { FRIENDS, INCOMING, SENT }

data class FriendsUiState(
    val tab: FriendsTab = FriendsTab.FRIENDS,
    val friends: List<Friend> = emptyList(),
    val incoming: List<FriendRequestDto> = emptyList(),
    val sent: List<FriendRequestDto> = emptyList(),
    val search: String = "",
    val loading: Boolean = false,
    val message: String? = null,
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

    fun onSearch(v: String) = update { it.copy(search = v) }

    fun refresh() {
        viewModelScope.launch {
            update { it.copy(loading = true) }
            val friends = runCatching { ApiClient.socialApi.getFriends() }.getOrDefault(emptyList())
            val incoming = runCatching { ApiClient.socialApi.getIncomingRequests() }.getOrDefault(emptyList())
            val sent = runCatching { ApiClient.socialApi.getSentRequests() }.getOrDefault(emptyList())
            update { it.copy(friends = friends, incoming = incoming, sent = sent, loading = false) }
        }
    }

    fun filteredFriends(): List<Friend> {
        val q = uiState.value.search.trim().lowercase()
        if (q.isEmpty()) return uiState.value.friends
        return uiState.value.friends.filter { (it.name ?: "").lowercase().contains(q) }
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
