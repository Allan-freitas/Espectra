package br.com.espectra.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.Friend
import br.com.espectra.data.FriendRequestDto
import br.com.espectra.ui.feed.AppTabBar
import br.com.espectra.ui.feed.FeedBrand
import br.com.espectra.ui.feed.FeedBrandDark
import br.com.espectra.ui.feed.FeedCardBg
import br.com.espectra.ui.feed.FeedError
import br.com.espectra.ui.feed.FeedInk
import br.com.espectra.ui.feed.FeedMuted
import br.com.espectra.ui.feed.InitialsAvatar

private val TABS = listOf(
    "Amigos" to FriendsTab.FRIENDS,
    "Recebidos" to FriendsTab.INCOMING,
    "Enviados" to FriendsTab.SENT,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    userName: String?,
    onAuthorClick: (userId: String, userName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState
    val snackbar = remember { SnackbarHostState() }
    var toRemove by remember { mutableStateOf<Friend?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "${userName?.split(" ")?.firstOrNull() ?: "Meus"} amigos (${state.friends.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FeedInk,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            AppTabBar(
                labels = listOf(
                    "Amigos (${state.friends.size})",
                    "Recebidos (${state.incoming.size})",
                    "Enviados (${state.sent.size})",
                ),
                selected = TABS.indexOfFirst { it.second == state.tab },
                onSelect = { index -> viewModel.setTab(TABS[index].second) },
            )

            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (state.tab) {
                    FriendsTab.FRIENDS -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        ) {
                            item {
                                OutlinedTextField(
                                    value = state.search,
                                    onValueChange = viewModel::onSearch,
                                    placeholder = { Text("Buscar amigos ou pessoas...", fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FeedBrand,
                                        focusedLabelColor = FeedBrandDark,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                )
                            }
                            val friends = viewModel.filteredFriends()
                            if (friends.isEmpty() && !state.loading && state.search.trim().length < 2) {
                                item { EmptyHint("Nenhum amigo encontrado. 🤝") }
                            } else {
                                items(friends, key = { "f-${it.id}" }) { friend ->
                                    FriendRow(
                                        name = friend.name.orEmpty().ifBlank { "Usuário" },
                                        avatar = friend.avatar,
                                        subtitle = friend.mutualInfo
                                            ?: friend.friendsSince?.let { "Amigos desde $it" },
                                        onClick = {
                                            onAuthorClick(friend.id, friend.name.orEmpty())
                                        },
                                        action = {
                                            TextButton(onClick = { toRemove = friend }) {
                                                Text(text = "Remover", fontSize = 12.sp, color = FeedError)
                                            }
                                        },
                                    )
                                }
                            }
                            if (state.search.trim().length >= 2) {
                                item {
                                    Text(
                                        text = "Pessoas na Espectra",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FeedInk,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                                if (state.searchingGlobal && state.globalResults.isEmpty()) {
                                    item {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            CircularProgressIndicator(color = FeedBrand, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                } else if (state.globalResults.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Nenhum usuário encontrado para \"${state.search.trim()}\".",
                                            fontSize = 13.sp,
                                            color = FeedMuted,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                        )
                                    }
                                } else {
                                    items(state.globalResults, key = { "g-${it.userId}" }) { person ->
                                        val fullName = "${person.name.orEmpty()} ${person.lastName.orEmpty()}".trim().ifBlank { "Usuário" }
                                        val requested = state.requestedIds.contains(person.userId)
                                        FriendRow(
                                            name = fullName,
                                            avatar = person.avatarUrl,
                                            subtitle = when {
                                                person.isFriend -> "Já é seu amigo ✓"
                                                requested -> "Pedido enviado ✓"
                                                else -> null
                                            },
                                            onClick = { onAuthorClick(person.userId, fullName) },
                                            action = {
                                                if (!person.isFriend && !requested) {
                                                    Button(
                                                        onClick = { viewModel.sendRequest(person.userId) },
                                                        shape = RoundedCornerShape(999.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = FeedBrand,
                                                            contentColor = Color.White,
                                                        ),
                                                    ) {
                                                        Text(text = "+ Adicionar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            if (state.loading && friends.isEmpty()) {
                                item { CenterSpinner() }
                            }
                        }
                    }
                    FriendsTab.INCOMING -> {
                        RequestList(
                            requests = state.incoming,
                            loading = state.loading,
                            emptyText = "Nenhum pedido recebido. 📭",
                            onAuthorClick = onAuthorClick,
                            primaryLabel = "Aceitar",
                            onPrimary = viewModel::accept,
                            secondaryLabel = "Recusar",
                            onSecondary = viewModel::reject,
                        )
                    }
                    FriendsTab.SENT -> {
                        RequestList(
                            requests = state.sent,
                            loading = state.loading,
                            emptyText = "Nenhum pedido enviado. 📤",
                            onAuthorClick = onAuthorClick,
                            primaryLabel = null,
                            onPrimary = {},
                            secondaryLabel = "Cancelar",
                            onSecondary = viewModel::cancel,
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        toRemove?.let { friend ->
            AlertDialog(
                onDismissRequest = { toRemove = null },
                title = { Text(text = "Remover amigo", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Remover ${friend.name.orEmpty()} da sua lista de amigos?") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.removeFriend(friend); toRemove = null },
                        colors = ButtonDefaults.buttonColors(containerColor = FeedError, contentColor = Color.White),
                    ) {
                        Text(text = "Remover")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { toRemove = null }) { Text(text = "Cancelar") }
                },
            )
        }
    }
}

@Composable
private fun RequestList(
    requests: List<FriendRequestDto>,
    loading: Boolean,
    emptyText: String,
    onAuthorClick: (String, String) -> Unit,
    primaryLabel: String?,
    onPrimary: (FriendRequestDto) -> Unit,
    secondaryLabel: String,
    onSecondary: (FriendRequestDto) -> Unit,
) {
    if (requests.isEmpty()) {
        if (loading) {
            CenterSpinner()
        } else {
            EmptyHint(emptyText)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            items(requests, key = { it.id }) { req ->
                FriendRow(
                    name = req.name.orEmpty().ifBlank { "Usuário" },
                    avatar = req.avatar,
                    subtitle = req.mutualInfo,
                    onClick = {
                        val uid = req.userId
                        if (uid != null) onAuthorClick(uid, req.name.orEmpty())
                    },
                    action = {
                        if (primaryLabel != null) {
                            Button(
                                onClick = { onPrimary(req) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                            ) {
                                Text(text = primaryLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        TextButton(onClick = { onSecondary(req) }) {
                            Text(text = secondaryLabel, fontSize = 12.sp, color = FeedMuted)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FriendRow(
    name: String,
    avatar: String?,
    subtitle: String?,
    onClick: () -> Unit,
    action: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
        ) {
            InitialsAvatar(name = name, imagePath = avatar, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FeedInk)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, fontSize = 12.sp, color = FeedMuted)
                }
            }
            action()
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "👥", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, fontSize = 14.sp, color = FeedMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CenterSpinner() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        CircularProgressIndicator(color = FeedBrand)
    }
}
