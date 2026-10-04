package br.com.espectra.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.PostVisibility

private val TABS = listOf("Status" to ComposerTab.STATUS, "Mídia" to ComposerTab.MEDIA, "Blog" to ComposerTab.BLOG)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposerCard(
    state: FeedUiState,
    viewModel: FeedViewModel,
    userName: String?,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTabBar(
                labels = TABS.map { it.first },
                selected = TABS.indexOfFirst { it.second == state.composerTab },
                onSelect = { index -> viewModel.setTab(TABS[index].second) },
            )

            when (state.composerTab) {
                ComposerTab.STATUS -> {
                    Row(verticalAlignment = Alignment.Top) {
                        InitialsAvatar(name = userName ?: "?", imagePath = state.avatarPath, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.statusText,
                            onValueChange = viewModel::onStatusChange,
                            placeholder = { Text("Compartilhe o que você está pensando...") },
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = composerFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.selectedFeeling != null) "${state.selectedFeeling.emoji} ${state.selectedFeeling.label}" else "🙂 Sentimentos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FeedBrandDark,
                            modifier = Modifier
                                .background(Color(0xFFFFEDD5), RoundedCornerShape(999.dp))
                                .clickable { viewModel.toggleFeelings() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    if (state.showFeelings) {
                        OutlinedTextField(
                            value = state.feelingsSearch,
                            onValueChange = viewModel::onFeelingsSearch,
                            placeholder = { Text("Buscar sentimento...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = composerFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                        ) {
                            items(viewModel.filteredFeelings()) { feeling ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(
                                            if (state.selectedFeeling?.label == feeling.label) Color(0xFFFFEDD5) else Color(0xFFF8FAFC),
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { viewModel.selectFeeling(feeling) }
                                        .padding(vertical = 8.dp),
                                ) {
                                    Text(text = feeling.emoji, fontSize = 22.sp)
                                    Text(text = feeling.label, fontSize = 11.sp, color = FeedInk)
                                }
                            }
                        }
                    }
                    state.selectedFeeling?.let { feeling ->
                        if (!state.showFeelings) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${feeling.emoji} está se sentindo ${feeling.label}",
                                    fontSize = 13.sp,
                                    color = FeedInk,
                                    modifier = Modifier
                                        .background(Color(0xFFFFEDD5), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "✕",
                                    fontSize = 14.sp,
                                    color = FeedMuted,
                                    modifier = Modifier.clickable { viewModel.clearFeeling() }.padding(6.dp),
                                )
                            }
                        }
                    }
                }
                ComposerTab.MEDIA -> {
                    OutlinedTextField(
                        value = state.mediaDesc,
                        onValueChange = viewModel::onMediaDescChange,
                        placeholder = { Text("Adicione uma descrição...") },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = composerFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "📸 A foto/vídeo pode ser anexada após publicar (em breve).",
                        fontSize = 12.sp,
                        color = FeedMuted,
                    )
                }
                ComposerTab.BLOG -> {
                    OutlinedTextField(
                        value = state.blogTitle,
                        onValueChange = viewModel::onBlogTitleChange,
                        placeholder = { Text("Título do post...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = composerFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.blogContent,
                        onValueChange = viewModel::onBlogContentChange,
                        placeholder = { Text("Escreva seu texto aqui...") },
                        singleLine = false,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = composerFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.visibility == PostVisibility.SpecificFriends) {
                FriendsPicker(state = state, viewModel = viewModel)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PostVisibility.entries.forEach { v ->
                    FilterChip(
                        selected = state.visibility == v,
                        onClick = { viewModel.onVisibilityChange(v) },
                        label = {
                            Text(
                                when (v) {
                                    PostVisibility.Public -> "🌍 Público"
                                    PostVisibility.FriendsOnly -> "👥 Amigos"
                                    PostVisibility.SpecificFriends -> "🔒 Específicos"
                                },
                                fontSize = 12.sp,
                            )
                        },
                    )
                }
            }

            Button(
                onClick = {
                    when (state.composerTab) {
                        ComposerTab.STATUS -> viewModel.postStatus()
                        ComposerTab.MEDIA -> viewModel.postMedia()
                        ComposerTab.BLOG -> viewModel.publishBlog()
                    }
                },
                enabled = !state.posting,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.posting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (state.posting) "Publicando..." else when (state.composerTab) {
                        ComposerTab.STATUS -> "Publicar"
                        ComposerTab.MEDIA -> "Publicar mídia"
                        ComposerTab.BLOG -> "Publicar blog"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FriendsPicker(state: FeedUiState, viewModel: FeedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = state.friendsSearch,
            onValueChange = viewModel::onFriendsSearch,
            placeholder = { Text("Buscar amigos...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = composerFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.loadingFriends) {
            Text(text = "Carregando amigos...", fontSize = 13.sp, color = FeedMuted)
        } else {
            val friends = viewModel.filteredFriends()
            if (friends.isEmpty()) {
                Text(text = "Nenhum amigo encontrado.", fontSize = 13.sp, color = FeedMuted)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                ) {
                    items(friends, key = { it.id }) { friend ->
                        val selected = state.allowedIds.contains(friend.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) Color(0xFFFFEDD5) else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { viewModel.toggleAllowedFriend(friend.id) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            InitialsAvatar(name = friend.name.orEmpty().ifBlank { "?" }, imagePath = friend.avatar, modifier = Modifier.size(32.dp), fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = friend.name.orEmpty(), fontSize = 14.sp, color = FeedInk, modifier = Modifier.weight(1f))
                            Text(text = if (selected) "✓" else "", fontSize = 16.sp, color = FeedBrandDark)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun composerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FeedBrand,
    focusedLabelColor = FeedBrandDark,
)
