package br.com.espectra.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.ActivityFeedItem
import br.com.espectra.data.FriendSuggestion
import br.com.espectra.data.UpcomingBirthday
import br.com.espectra.data.WeatherInfo

/** Aba "Descobrir": clima, aniversário, sugestões e atividades do web. */
@Composable
fun DiscoverSection(
    birthday: UpcomingBirthday?,
    suggestions: List<FriendSuggestion>,
    activities: List<ActivityFeedItem>,
    weather: WeatherInfo?,
    onAddFriend: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WeatherCard(weather = weather)

        birthday?.let {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FeedCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Text(text = "🎂", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aniversário de ${it.name} ${it.lastName}!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FeedInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (it.daysUntilBirthday == 0) "Hoje · Envie parabéns" else "${it.daysUntilBirthday} dias restantes · Envie parabéns",
                            fontSize = 12.sp,
                            color = FeedMuted,
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FeedCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Sugestões de amizade", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FeedInk)
                if (suggestions.isEmpty()) {
                    Text(text = "Nenhuma sugestão no momento.", fontSize = 13.sp, color = FeedMuted)
                } else {
                    suggestions.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name = "${s.name} ${s.lastName}", imagePath = s.avatarUrl, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${s.name} ${s.lastName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FeedInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${s.mutualFriendsCount} amigos em comum",
                                    fontSize = 12.sp,
                                    color = FeedMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Button(
                                onClick = { onAddFriend(s.userId) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                            ) {
                                Text(text = "+ Adicionar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FeedCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Atividades", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FeedInk)
                if (activities.isEmpty()) {
                    Text(text = "Nenhuma atividade recente.", fontSize = 13.sp, color = FeedMuted)
                } else {
                    activities.forEach { a ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "🟠", fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildString {
                                        append(a.actorName)
                                        append(" ")
                                        append(a.description)
                                    },
                                    fontSize = 13.sp,
                                    color = FeedInk,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(text = formatPostTime(a.createdAt), fontSize = 11.sp, color = FeedMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherInfo?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0EA5E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (weather == null) {
                Text(text = "--°F", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = "Clima indisponível", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "☁️", fontSize = 40.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${weather.tempF.toInt()}°F",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Text(text = weather.condition, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Umidade: ${weather.humidity}%   ·   Vento: ${weather.windKph.toInt()} km/h",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
                if (weather.nextDays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        weather.nextDays.take(4).forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = formatDayName(day.date), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = "${day.maxTempF.toInt()}°", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
