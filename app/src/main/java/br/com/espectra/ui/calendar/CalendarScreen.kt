package br.com.espectra.ui.calendar

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.CalendarEvent
import br.com.espectra.ui.feed.FeedBrand
import br.com.espectra.ui.feed.FeedBrandDark
import br.com.espectra.ui.feed.FeedCardBg
import br.com.espectra.ui.feed.FeedError
import br.com.espectra.ui.feed.FeedInk
import br.com.espectra.ui.feed.FeedMuted
import java.time.LocalDate
import java.time.YearMonth

private val WEEK_DAYS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
private val MONTHS_PT = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val today = LocalDate.now()
    val yearMonth = YearMonth.of(state.year, state.month)
    val firstOffset = yearMonth.atDay(1).dayOfWeek.value % 7 // domingo primeiro
    val cells = List(firstOffset) { null } + (1..yearMonth.lengthOfMonth()).toList()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                TextButton(onClick = viewModel::prevMonth) {
                    Text(text = "←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FeedBrandDark)
                }
                Text(
                    text = "${MONTHS_PT[state.month - 1].replaceFirstChar { it.uppercase() }} de ${state.year}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FeedInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::nextMonth) {
                    Text(text = "→", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FeedBrandDark)
                }
            }

            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = FeedCardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    WEEK_DAYS.forEach { wd ->
                                        Text(
                                            text = wd,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FeedMuted,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                cells.chunked(7).forEach { week ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        week.forEach { day ->
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp),
                                            ) {
                                                if (day != null) {
                                                    val isToday = state.year == today.year &&
                                                        state.month == today.monthValue && day == today.dayOfMonth
                                                    val isSelected = day == state.selectedDay
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(
                                                                when {
                                                                    isSelected -> FeedBrand
                                                                    isToday -> Color(0xFFFFEDD5)
                                                                    else -> Color.Transparent
                                                                },
                                                            )
                                                            .clickable { viewModel.selectDay(day) }
                                                            .padding(vertical = 4.dp)
                                                            .fillMaxWidth(),
                                                    ) {
                                                        Text(
                                                            text = "$day",
                                                            fontSize = 14.sp,
                                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) Color.White else FeedInk,
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .background(
                                                                    if (viewModel.hasEventOnDay(day)) {
                                                                        if (isSelected) Color.White else FeedBrand
                                                                    } else {
                                                                        Color.Transparent
                                                                    },
                                                                    CircleShape,
                                                                ),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Eventos · ${state.selectedDay} de ${MONTHS_PT[state.month - 1]}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FeedInk,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = viewModel::openCreate,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FeedBrand, contentColor = Color.White),
                            ) {
                                Text(text = "+ Novo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val dayEvents = viewModel.dayEvents()
                    if (dayEvents.isEmpty()) {
                        item {
                            Text(
                                text = "Nada agendado para este dia. 🗓️",
                                fontSize = 13.sp,
                                color = FeedMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    } else {
                        items(dayEvents, key = { it.title + it.time + it.isBirthday }) { ev ->
                            DayEventRow(
                                event = ev,
                                onToggle = {
                                    val src = state.events.firstOrNull { e -> e.id == ev.eventId }
                                    if (src != null) viewModel.toggleComplete(src)
                                },
                                onDelete = {
                                    val src = state.events.firstOrNull { e -> e.id == ev.eventId }
                                    if (src != null) viewModel.askDelete(src)
                                },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        if (state.showCreate) CreateEventDialog(viewModel = viewModel)

        state.deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                title = { Text(text = "Excluir evento", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Excluir \"${target.title.orEmpty()}\"?") },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDelete,
                        enabled = !state.deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = FeedError, contentColor = Color.White),
                    ) {
                        Text(text = if (state.deleting) "Excluindo..." else "Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDelete, enabled = !state.deleting) {
                        Text(text = "Cancelar")
                    }
                },
            )
        }
    }
}

@Composable
private fun DayEventRow(
    event: DayEvent,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Text(text = if (event.isBirthday) "🎂" else "📌", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeedInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (event.completed) TextDecoration.LineThrough else null,
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        fontSize = 13.sp,
                        color = FeedMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(text = "🕐 ${event.time}", fontSize = 12.sp, color = FeedMuted)
            }
            if (!event.isBirthday) {
                Text(
                    text = if (event.completed) "☑️" else "⬜",
                    fontSize = 20.sp,
                    modifier = Modifier.clickable(onClick = onToggle).padding(4.dp),
                )
                Text(
                    text = "🗑️",
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onDelete).padding(4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEventDialog(viewModel: CalendarViewModel) {
    val state by viewModel.uiState
    AlertDialog(
        onDismissRequest = viewModel::closeCreate,
        title = { Text(text = "Novo evento · ${state.selectedDay}/${state.month}/${state.year}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = state.formTitle,
                    onValueChange = viewModel::onFormTitle,
                    placeholder = { Text("Nome do evento") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedBrand,
                        focusedLabelColor = FeedBrandDark,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.formTime,
                    onValueChange = viewModel::onFormTime,
                    placeholder = { Text("Hora (ex. 09:00)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedBrand,
                        focusedLabelColor = FeedBrandDark,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.formDescription,
                    onValueChange = viewModel::onFormDescription,
                    placeholder = { Text("Descrição (opcional)") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedBrand,
                        focusedLabelColor = FeedBrandDark,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "Tipo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FeedMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EVENT_TYPES.forEach { t ->
                        FilterChip(
                            selected = state.formType == t,
                            onClick = { viewModel.onFormType(t) },
                            label = { Text(t, fontSize = 12.sp) },
                        )
                    }
                }
                Text(text = "Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FeedMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EVENT_CATEGORIES.forEach { c ->
                        FilterChip(
                            selected = state.formCategory == c,
                            onClick = { viewModel.onFormCategory(c) },
                            label = { Text(c, fontSize = 12.sp) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::createEvent,
                enabled = !state.creating && state.formTitle.trim().isNotEmpty(),
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
