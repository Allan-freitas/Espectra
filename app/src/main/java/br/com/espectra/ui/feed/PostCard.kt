package br.com.espectra.ui.feed

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.CommentFeed
import br.com.espectra.data.PostFeed

@Composable
fun PostCard(
    post: PostFeed,
    isMine: Boolean,
    expanded: Boolean,
    comments: List<CommentFeed>,
    loadingComments: Boolean,
    commentText: String,
    onLike: () -> Unit,
    onToggleComments: () -> Unit,
    onCommentChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onDelete: () -> Unit,
    onAuthorClick: (userId: String, userName: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = post.userName, imagePath = post.userAvatarUrl, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.userName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FeedInk,
                        modifier = Modifier.clickable { onAuthorClick(post.userId, post.userName) },
                    )
                    Text(text = formatPostTime(post.createdAt), fontSize = 12.sp, color = FeedMuted)
                }
                Text(
                    text = "${visibilityEmoji(post.visibility)} ${visibilityLabel(post.visibility)}",
                    fontSize = 11.sp,
                    color = FeedMuted,
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                if (isMine) {
                    TextButton(onClick = onDelete) {
                        Text(text = "🗑️", fontSize = 16.sp)
                    }
                }
            }

            Text(text = post.content, fontSize = 14.sp, color = FeedInk, lineHeight = 20.sp)

            if (!post.mediaUrl.isNullOrBlank()) {
                RemoteImage(
                    pathOrUrl = post.mediaUrl,
                    contentDescription = "Mídia do post",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9)),
                    fallback = { Text(text = "🖼️", fontSize = 40.sp) },
                )
            } else if (post.mediaType == "image") {
                MediaPlaceholder(emoji = "🖼️", label = "Prévia da imagem")
            } else if (post.mediaType == "video") {
                MediaPlaceholder(emoji = "▶️", label = "Prévia do vídeo")
            }

            Row {
                Text(
                    text = "❤️ ${post.likesCount} Curtidas",
                    fontSize = 12.sp,
                    color = if (post.isLikedByCurrentUser) FeedBrand else FeedMuted,
                    fontWeight = if (post.isLikedByCurrentUser) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "💬 ${post.commentsCount} Comentários", fontSize = 12.sp, color = FeedMuted)
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                FeedAction(label = if (post.isLikedByCurrentUser) "❤️ Curtido" else "🤍 Curtir", onClick = onLike)
                FeedAction(label = "💬 Comentar", onClick = onToggleComments)
                FeedAction(
                    label = "🔄 Compartilhar",
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${post.userName}: ${post.content}")
                        }
                        context.startActivity(Intent.createChooser(send, "Compartilhar post"))
                    },
                )
            }

            if (expanded) {
                if (loadingComments) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = FeedBrand)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Carregando comentários...", fontSize = 13.sp, color = FeedMuted)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        comments.forEach { comment ->
                            Row(verticalAlignment = Alignment.Top) {
                                InitialsAvatar(
                                    name = comment.userName,
                                    imagePath = comment.userAvatarUrl,
                                    modifier = Modifier.size(32.dp),
                                    fontSize = 11.sp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                ) {
                                    Text(
                                        text = comment.userName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FeedInk,
                                        modifier = Modifier.clickable { onAuthorClick(comment.userId, comment.userName) },
                                    )
                                    Text(text = comment.content, fontSize = 13.sp, color = FeedInk)
                                    Text(text = formatPostTime(comment.createdAt), fontSize = 11.sp, color = FeedMuted)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = onCommentChange,
                        placeholder = { Text("Escreva um comentário...", fontSize = 13.sp) },
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
                        onClick = onPostComment,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                    ) {
                        Text(text = "➤", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPlaceholder(emoji: String, label: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 40.sp)
            Text(text = label, fontSize = 12.sp, color = FeedMuted)
        }
    }
}

@Composable
private fun FeedAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FeedInk)
    }
}

@Composable
fun DeleteConfirmDialog(
    contentPreview: String,
    deleting: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "Excluir post", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = "Tem certeza que deseja excluir este post? Esta ação não pode ser desfeita.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contentPreview.take(200),
                    fontSize = 13.sp,
                    color = FeedMuted,
                    modifier = Modifier
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(containerColor = FeedError, contentColor = Color.White),
            ) {
                Text(text = if (deleting) "Excluindo..." else "Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !deleting) {
                Text(text = "Cancelar")
            }
        },
    )
}
