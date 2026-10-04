package br.com.espectra.ui.feed

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.ImageLoader
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

val FeedCream = Color(0xFFFFFBEB)
val FeedInk = Color(0xFF1E293B)
val FeedMuted = Color(0xFF64748B)
val FeedBrand = Color(0xFFF97316)
val FeedBrandDark = Color(0xFFEA580C)
val FeedError = Color(0xFFEF4444)
val FeedCardBg = Color.White

/** Baixa e exibe imagem autenticada; mostra [fallback] enquanto carrega/falha. */
@Composable
fun RemoteImage(
    pathOrUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {},
) {
    val url = ImageLoader.resolve(pathOrUrl)
    if (url == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
        return
    }
    val bitmap = produceState<Bitmap?>(initialValue = null, url) {
        value = ImageLoader.load(url)
    }.value
    if (bitmap == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
    } else {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    imagePath: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).map { it[0] }.joinToString("")
    RemoteImage(
        pathOrUrl = imagePath,
        contentDescription = "Avatar de $name",
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFFFEDD5)),
        fallback = {
            Text(text = initials.ifBlank { "?" }, fontSize = fontSize, fontWeight = FontWeight.Bold, color = FeedBrandDark)
        },
    )
}

fun visibilityLabel(v: Int): String = when (v) {
    1 -> "Amigos"
    2 -> "Específicos"
    else -> "Público"
}

fun visibilityEmoji(v: Int): String = when (v) {
    1 -> "👥"
    2 -> "🔒"
    else -> "🌍"
}

/** Relativo em PT, como o DatePipe do web mas amigável no mobile. */
fun formatPostTime(iso: String): String {
    return try {
        val date = OffsetDateTime.parse(iso)
        val now = OffsetDateTime.now(date.offset)
        val mins = Duration.between(date, now).toMinutes()
        when {
            mins < 1 -> "agora"
            mins < 60 -> "há $mins min"
            mins < 60 * 24 -> "há ${mins / 60} h"
            mins < 60 * 48 -> "ontem"
            else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    } catch (e: Exception) {
        iso.take(10)
    }
}

fun formatDayName(date: String): String {
    return try {
        java.time.LocalDate.parse(date)
            .dayOfWeek.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.Builder().setLanguage("pt").setRegion("BR").build(),
            )
    } catch (e: Exception) {
        date
    }
}
