package br.com.espectra.ui.photos

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.espectra.data.AlbumCard
import br.com.espectra.data.AlbumVisibility
import br.com.espectra.ui.feed.FeedBrand
import br.com.espectra.ui.feed.FeedBrandDark
import br.com.espectra.ui.feed.FeedCardBg
import br.com.espectra.ui.feed.FeedError
import br.com.espectra.ui.feed.FeedInk
import br.com.espectra.ui.feed.FeedMuted
import br.com.espectra.ui.feed.InitialsAvatar
import br.com.espectra.ui.feed.RemoteImage
import br.com.espectra.ui.feed.formatPostTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val photos = uris.mapNotNull { uri ->
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/*"
                var name = "foto_${System.currentTimeMillis()}.jpg"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) {
                        cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@mapNotNull null
                PickedPhoto(name = name, mime = mime, bytes = bytes)
            } catch (e: Exception) {
                null
            }
        }
        if (photos.isNotEmpty()) viewModel.uploadPhotos(photos)
    }

    Box(modifier = modifier.fillMaxSize()) {
        val selected = state.selected
        if (selected != null) {
            AlbumDetailsView(
                viewModel = viewModel,
                onPickPhotos = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Meus álbuns (${state.albums.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FeedInk,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = viewModel::openCreate,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                    ) {
                        Text(text = "+ Novo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                PullToRefreshBox(
                    isRefreshing = state.loading,
                    onRefresh = viewModel::loadAlbums,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.albums.isEmpty() && !state.loading) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📸", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhum álbum ainda.\nCrie o primeiro no botão + Novo.",
                                    fontSize = 14.sp,
                                    color = FeedMuted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        ) {
                            items(state.albums, key = { it.id }) { album ->
                                AlbumGridCard(
                                    album = album,
                                    onOpen = { viewModel.openAlbum(album) },
                                    onComments = { viewModel.openComments(album) },
                                    onShare = { viewModel.openShare(album) },
                                    onDelete = { viewModel.askDeleteAlbum(album) },
                                )
                            }
                        }
                    }
                }
            }

            if (state.albumLoading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f))) {
                    CircularProgressIndicator(color = FeedBrand)
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        if (state.showCreate) CreateAlbumDialog(viewModel = viewModel)

        state.deleteAlbumTarget?.let { album ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteAlbum,
                title = { Text(text = "Excluir álbum", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Excluir \"${album.title.orEmpty()}\"? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDeleteAlbum,
                        enabled = !state.deletingAlbum,
                        colors = ButtonDefaults.buttonColors(containerColor = FeedError, contentColor = Color.White),
                    ) {
                        Text(text = if (state.deletingAlbum) "Excluindo..." else "Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteAlbum, enabled = !state.deletingAlbum) {
                        Text(text = "Cancelar")
                    }
                },
            )
        }

        state.commentsAlbum?.let {
            AlbumCommentsDialog(viewModel = viewModel)
        }

        if (state.shareAlbum != null) {
            AlbumShareDialog(viewModel = viewModel)
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: AlbumCard,
    onOpen: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.clickable(onClick = onOpen)) {
            val preview = album.previewUrls.firstOrNull()
            RemoteImage(
                pathOrUrl = preview,
                contentDescription = "Capa do álbum",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF1F5F9)),
                fallback = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🖼️", fontSize = 36.sp)
                        Text(text = "${album.photoCount} fotos", fontSize = 11.sp, color = FeedMuted)
                    }
                },
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = album.title.orEmpty().ifBlank { "Sem título" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeedInk,
                    maxLines = 1,
                )
                Text(
                    text = "📷 ${album.photoCount}  ·  💬 ${album.commentCount}  ·  🔄 ${album.shareCount}",
                    fontSize = 11.sp,
                    color = FeedMuted,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = onComments) { Text(text = "💬", fontSize = 15.sp) }
                    TextButton(onClick = onShare) { Text(text = "🔄", fontSize = 15.sp) }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDelete) { Text(text = "🗑️", fontSize = 15.sp) }
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailsView(
    viewModel: AlbumsViewModel,
    onPickPhotos: () -> Unit,
) {
    val state by viewModel.uiState
    val album = state.selected ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            TextButton(onClick = viewModel::closeAlbum) {
                Text(text = "← Álbuns", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeedBrandDark)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.uploading) {
                CircularProgressIndicator(color = FeedBrand, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = onPickPhotos) {
                    Text(text = "+ Fotos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeedBrandDark)
                }
            }
            TextButton(onClick = {
                viewModel.askDeleteAlbum(
                    AlbumCard(
                        id = album.id, title = album.title, updatedAt = album.updatedAt,
                        photoCount = album.photoCount, commentCount = album.commentCount,
                        shareCount = album.shareCount, previewUrls = album.previewUrls,
                        visibility = album.visibility, allowedFriendIds = album.allowedFriendIds,
                    ),
                )
            }) {
                Text(text = "🗑️", fontSize = 16.sp)
            }
        }

        Text(
            text = album.title.orEmpty().ifBlank { "Sem título" },
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FeedInk,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "${album.photos.size} fotos  ·  💬 ${album.commentCount}  ·  🔄 ${album.shareCount}",
            fontSize = 12.sp,
            color = FeedMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (album.photos.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(text = "Álbum vazio.\nToque em + Fotos para enviar.", fontSize = 14.sp, color = FeedMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            ) {
                items(album.photos, key = { it.id }) { photo ->
                    val url = photo.thumbnailUrl ?: photo.url
                    Box {
                        RemoteImage(
                            pathOrUrl = url,
                            contentDescription = "Foto",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .clickable { url?.let { viewModel.openViewer(it) } },
                            fallback = { Text(text = "🖼️", fontSize = 24.sp) },
                        )
                        Text(
                            text = "✕",
                            fontSize = 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                                .clickable { viewModel.askDeletePhoto(photo) }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }

    state.viewerUrl?.let { url ->
        Dialog(onDismissRequest = viewModel::closeViewer, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable(onClick = viewModel::closeViewer),
            ) {
                RemoteImage(
                    pathOrUrl = url,
                    contentDescription = "Foto ampliada",
                    modifier = Modifier.fillMaxWidth(),
                    fallback = { CircularProgressIndicator(color = Color.White) },
                )
            }
        }
    }

    state.deletePhotoTarget?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeletePhoto,
            title = { Text(text = "Excluir foto", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Excluir esta foto do álbum?") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeletePhoto,
                    enabled = !state.deletingPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = FeedError, contentColor = Color.White),
                ) {
                    Text(text = if (state.deletingPhoto) "Excluindo..." else "Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeletePhoto, enabled = !state.deletingPhoto) {
                    Text(text = "Cancelar")
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateAlbumDialog(viewModel: AlbumsViewModel) {
    val state by viewModel.uiState
    AlertDialog(
        onDismissRequest = viewModel::closeCreate,
        title = { Text(text = "Novo álbum", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = state.createTitle,
                    onValueChange = viewModel::onCreateTitle,
                    placeholder = { Text("Título do álbum") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedBrand,
                        focusedLabelColor = FeedBrandDark,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlbumVisibility.entries.forEach { v ->
                        FilterChip(
                            selected = state.createVisibility == v,
                            onClick = { viewModel.onCreateVisibility(v) },
                            label = {
                                Text(
                                    when (v) {
                                        AlbumVisibility.FriendsOnly -> "👥 Amigos"
                                        AlbumVisibility.SpecificFriends -> "🔒 Específicos"
                                        AlbumVisibility.Public -> "🌍 Público"
                                    },
                                    fontSize = 12.sp,
                                )
                            },
                        )
                    }
                }
                if (state.createVisibility == AlbumVisibility.SpecificFriends) {
                    OutlinedTextField(
                        value = state.createFriendsSearch,
                        onValueChange = viewModel::onCreateFriendsSearch,
                        placeholder = { Text("Buscar amigos...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FeedBrand,
                            focusedLabelColor = FeedBrandDark,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.loadingCreateFriends) {
                        Text(text = "Carregando amigos...", fontSize = 13.sp, color = FeedMuted)
                    } else {
                        viewModel.filteredCreateFriends().forEach { friend ->
                            val selected = state.createAllowedIds.contains(friend.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleCreateAllowedFriend(friend.id) }
                                    .padding(vertical = 4.dp),
                            ) {
                                InitialsAvatar(
                                    name = friend.name.orEmpty().ifBlank { "?" },
                                    imagePath = friend.avatar,
                                    modifier = Modifier.size(32.dp),
                                    fontSize = 11.sp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = friend.name.orEmpty(), fontSize = 14.sp, color = FeedInk, modifier = Modifier.weight(1f))
                                Text(text = if (selected) "✓" else "", fontSize = 16.sp, color = FeedBrandDark)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::createAlbum,
                enabled = !state.creating && state.createTitle.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
            ) {
                Text(text = if (state.creating) "Criando..." else "Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeCreate, enabled = !state.creating) {
                Text(text = "Cancelar")
            }
        },
    )
}

@Composable
private fun AlbumCommentsDialog(viewModel: AlbumsViewModel) {
    val state by viewModel.uiState
    AlertDialog(
        onDismissRequest = viewModel::closeComments,
        title = { Text(text = "Comentários", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (state.loadingComments) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        CircularProgressIndicator(color = FeedBrand, modifier = Modifier.size(28.dp))
                    }
                } else if (state.comments.isEmpty()) {
                    Text(text = "Nenhum comentário ainda.", fontSize = 13.sp, color = FeedMuted)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.comments, key = { it.id }) { comment ->
                            Row(verticalAlignment = Alignment.Top) {
                                InitialsAvatar(
                                    name = comment.userName.orEmpty().ifBlank { "?" },
                                    imagePath = comment.userAvatarUrl,
                                    modifier = Modifier.size(32.dp),
                                    fontSize = 11.sp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = comment.userName.orEmpty(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FeedInk)
                                    Text(text = comment.content.orEmpty(), fontSize = 13.sp, color = FeedInk)
                                    Text(
                                        text = comment.createdAt?.let { formatPostTime(it) } ?: "",
                                        fontSize = 11.sp,
                                        color = FeedMuted,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = viewModel::onCommentText,
                        placeholder = { Text("Comentar...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FeedBrand,
                            focusedLabelColor = FeedBrandDark,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = viewModel::postComment,
                        enabled = !state.submittingComment,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                    ) {
                        Text(text = "➤", fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = viewModel::closeComments) { Text(text = "Fechar") }
        },
    )
}

@Composable
private fun AlbumShareDialog(viewModel: AlbumsViewModel) {
    val state by viewModel.uiState
    AlertDialog(
        onDismissRequest = viewModel::closeShare,
        title = { Text(text = "Compartilhar álbum", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.shareSearch,
                    onValueChange = viewModel::onShareSearch,
                    placeholder = { Text("Buscar amigo...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedBrand,
                        focusedLabelColor = FeedBrandDark,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.loadingShareFriends) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        CircularProgressIndicator(color = FeedBrand, modifier = Modifier.size(28.dp))
                    }
                } else if (viewModel.filteredShareFriends().isEmpty()) {
                    Text(text = "Nenhum amigo disponível.", fontSize = 13.sp, color = FeedMuted)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 300.dp),
                    ) {
                        items(viewModel.filteredShareFriends(), key = { it.id }) { friend ->
                            val selected = state.selectedShareFriendId == friend.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) Color(0xFFFFEDD5) else Color.Transparent,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable { viewModel.selectShareFriend(friend.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                InitialsAvatar(
                                    name = friend.name.orEmpty().ifBlank { "?" },
                                    imagePath = friend.avatar,
                                    modifier = Modifier.size(36.dp),
                                    fontSize = 11.sp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = friend.name.orEmpty(), fontSize = 14.sp, color = FeedInk, modifier = Modifier.weight(1f))
                                Text(text = if (selected) "✓" else "", fontSize = 16.sp, color = FeedBrandDark)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::confirmShare,
                enabled = !state.sharing && state.selectedShareFriendId != null,
                colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
            ) {
                Text(text = if (state.sharing) "Enviando..." else "Compartilhar")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeShare, enabled = !state.sharing) {
                Text(text = "Cancelar")
            }
        },
    )
}
