package br.com.espectra.ui.feed

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import br.com.espectra.ui.calendar.CalendarScreen
import br.com.espectra.ui.calendar.CalendarViewModel
import br.com.espectra.ui.friends.FriendsScreen
import br.com.espectra.ui.friends.FriendsViewModel
import br.com.espectra.ui.photos.AlbumsScreen
import br.com.espectra.ui.photos.AlbumsViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    friendsViewModel: FriendsViewModel,
    albumsViewModel: AlbumsViewModel,
    calendarViewModel: CalendarViewModel,
    userName: String?,
    onLogout: () -> Unit,
    onAuthorClick: (userId: String, userName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(FeedCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(text = "🧩", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "auticonecta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FeedInk,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onLogout) {
                    Text(text = "Sair", fontSize = 13.sp, color = FeedMuted)
                }
            }

            AppTabBar(
                labels = listOf("Feed", "Descobrir", "Amigos", "Fotos", "Agenda"),
                selected = tab,
                onSelect = {
                    if (it == tab) {
                        // Tocar na aba ativa recarrega (padrão mobile).
                        when (it) {
                            0, 1 -> viewModel.refresh()
                            2 -> friendsViewModel.refresh()
                            3 -> {
                                if (albumsViewModel.uiState.value.selected != null) {
                                    albumsViewModel.closeAlbum()
                                }
                                albumsViewModel.loadAlbums()
                            }
                            4 -> calendarViewModel.load()
                        }
                    } else {
                        tab = it
                    }
                },
            )

            if (tab == 0) {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.posts.isEmpty() && !state.refreshing && !state.loading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        ) {
                            ComposerCard(state = state, viewModel = viewModel, userName = userName)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(text = "♾️", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum post por aqui ainda.\nCompartilhe algo no seu ritmo. 🧩",
                                fontSize = 14.sp,
                                color = FeedMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            item {
                                ComposerCard(state = state, viewModel = viewModel, userName = userName)
                            }
                            itemsIndexed(state.posts, key = { _, p -> p.id }) { index, post ->
                                if (index >= state.posts.lastIndex - 2) {
                                    LaunchedEffect(postsSizeKey(state.posts.size, index)) {
                                        viewModel.loadMore()
                                    }
                                }
                                PostCard(
                                    post = post,
                                    isMine = state.currentUserId != null && state.currentUserId == post.userId,
                                    expanded = state.expandedPostId == post.id,
                                    comments = state.comments[post.id] ?: emptyList(),
                                    loadingComments = state.loadingComments && state.expandedPostId == post.id,
                                    commentText = if (state.expandedPostId == post.id) state.commentText else "",
                                    onLike = { viewModel.toggleLike(post) },
                                    onToggleComments = { viewModel.toggleComments(post.id) },
                                    onCommentChange = viewModel::onCommentText,
                                    onPostComment = { viewModel.postComment(post.id) },
                                    onDelete = { viewModel.openDelete(post) },
                                    onAuthorClick = onAuthorClick,
                                )
                            }
                            if (state.loading) {
                                item {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        CircularProgressIndicator(color = FeedBrand, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FeedCream)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    item {
                        DiscoverSection(
                            birthday = state.birthday,
                            suggestions = state.suggestions,
                            activities = state.activities,
                            weather = state.weather,
                            onAddFriend = viewModel::sendFriendRequest,
                        )
                    }
                }
            } else if (tab == 2) {
                FriendsScreen(
                    viewModel = friendsViewModel,
                    userName = userName,
                    onAuthorClick = onAuthorClick,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (tab == 3) {
                AlbumsScreen(
                    viewModel = albumsViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CalendarScreen(
                    viewModel = calendarViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        state.deleteTarget?.let { target ->
            DeleteConfirmDialog(
                contentPreview = target.content,
                deleting = state.deleting,
                onCancel = viewModel::closeDelete,
                onConfirm = viewModel::confirmDelete,
            )
        }
    }
}

/** Chave estável para o efeito de paginação não disparar em loop. */
private fun postsSizeKey(size: Int, index: Int): String = "$size-$index"
