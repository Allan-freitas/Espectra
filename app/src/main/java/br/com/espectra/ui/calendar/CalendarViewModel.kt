package br.com.espectra.ui.calendar

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.BirthdayMonth
import br.com.espectra.data.CalendarEvent
import br.com.espectra.data.CreateCalendarEvent
import br.com.espectra.data.UpdateCalendarEvent
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

data class DayEvent(
    val title: String,
    val description: String,
    val time: String,
    val isBirthday: Boolean,
    val completed: Boolean = false,
    val eventId: String? = null,
)

data class CalendarUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val selectedDay: Int = LocalDate.now().dayOfMonth,
    val events: List<CalendarEvent> = emptyList(),
    val birthdayMonths: List<BirthdayMonth> = emptyList(),
    val loading: Boolean = false,
    val showCreate: Boolean = false,
    val formTitle: String = "",
    val formTime: String = "",
    val formDescription: String = "",
    val formType: String = "Private Event",
    val formCategory: String = "Personal",
    val creating: Boolean = false,
    val deleteTarget: CalendarEvent? = null,
    val deleting: Boolean = false,
    val message: String? = null,
)

val EVENT_TYPES = listOf("Private Event", "Public Event", "Personal Event")
val EVENT_CATEGORIES = listOf("Work", "Personal", "Health", "Family")

private val MONTH_NUMBERS = mapOf(
    "january" to 1, "february" to 2, "march" to 3, "april" to 4,
    "may" to 5, "june" to 6, "july" to 7, "august" to 8,
    "september" to 9, "october" to 10, "november" to 11, "december" to 12,
    "janeiro" to 1, "fevereiro" to 2, "março" to 3, "marco" to 3, "abril" to 4,
    "maio" to 5, "junho" to 6, "julho" to 7, "agosto" to 8,
    "setembro" to 9, "outubro" to 10, "novembro" to 11, "dezembro" to 12,
)

/** Espelha calendar.component (grade real, CRUD de eventos, aniversários). */
class CalendarViewModel : ViewModel() {

    var uiState = mutableStateOf(CalendarUiState())
        private set

    init {
        load()
    }

    private fun update(transform: (CalendarUiState) -> CalendarUiState) {
        uiState.value = transform(uiState.value)
    }

    fun consumeMessage() = update { it.copy(message = null) }

    fun selectedDateStr(): String {
        val s = uiState.value
        return "%04d-%02d-%02d".format(s.year, s.month, s.selectedDay)
    }

    fun load() {
        viewModelScope.launch {
            update { it.copy(loading = true) }
            val events = runCatching { ApiClient.calendarApi.getAll() }.getOrDefault(emptyList())
            val birthdays = runCatching { ApiClient.socialApi.getBirthdayMonths() }.getOrDefault(emptyList())
            update { it.copy(events = events, birthdayMonths = birthdays, loading = false) }
        }
    }

    fun prevMonth() {
        update {
            var y = it.year
            var m = it.month - 1
            if (m < 1) { m = 12; y-- }
            it.copy(year = y, month = m, selectedDay = 1)
        }
    }

    fun nextMonth() {
        update {
            var y = it.year
            var m = it.month + 1
            if (m > 12) { m = 1; y++ }
            it.copy(year = y, month = m, selectedDay = 1)
        }
    }

    fun selectDay(day: Int) = update { it.copy(selectedDay = day) }

    fun hasEventOnDay(day: Int): Boolean {
        val s = uiState.value
        val dateStr = "%04d-%02d-%02d".format(s.year, s.month, day)
        if (s.events.any { it.date?.take(10) == dateStr }) return true
        return isBirthday(s.birthdayMonths, s.month, day)
    }

    fun dayEvents(): List<DayEvent> {
        val s = uiState.value
        val dateStr = selectedDateStr()
        val list = mutableListOf<DayEvent>()
        s.events.filter { it.date?.take(10) == dateStr }.forEach { e ->
            list += DayEvent(
                title = e.title.orEmpty().ifBlank { "Evento" },
                description = e.description.orEmpty(),
                time = e.time.orEmpty().ifBlank { "All day" },
                isBirthday = e.isBirthday,
                completed = e.completed,
                eventId = e.id,
            )
        }
        s.birthdayMonths.forEach { mg ->
            if (MONTH_NUMBERS[mg.month?.lowercase()] != s.month) return@forEach
            mg.friends.forEach { f ->
                if (extractDay(f.date) == s.selectedDay) {
                    list += DayEvent(
                        title = "Aniversário de ${(f.name ?: "?")}",
                        description = "",
                        time = "All day",
                        isBirthday = true,
                    )
                }
            }
        }
        return list
    }

    private fun isBirthday(months: List<BirthdayMonth>, month: Int, day: Int): Boolean {
        return months.any { mg ->
            MONTH_NUMBERS[mg.month?.lowercase()] == month &&
                mg.friends.any { extractDay(it.date) == day }
        }
    }

    private fun extractDay(dateStr: String?): Int? {
        if (dateStr.isNullOrBlank()) return null
        return Regex("\\d+").findAll(dateStr)
            .mapNotNull { it.value.toIntOrNull() }
            .firstOrNull { it in 1..31 }
    }

    // ---------- criar ----------

    fun openCreate() = update {
        it.copy(
            showCreate = true, formTitle = "", formTime = "",
            formDescription = "", formType = "Private Event", formCategory = "Personal",
        )
    }

    fun closeCreate() {
        if (uiState.value.creating) return
        update { it.copy(showCreate = false) }
    }

    fun onFormTitle(v: String) = update { it.copy(formTitle = v) }
    fun onFormTime(v: String) = update { it.copy(formTime = v) }
    fun onFormDescription(v: String) = update { it.copy(formDescription = v) }
    fun onFormType(v: String) = update { it.copy(formType = v) }
    fun onFormCategory(v: String) = update { it.copy(formCategory = v) }

    fun createEvent() {
        val s = uiState.value
        val title = s.formTitle.trim()
        if (title.isEmpty() || s.creating) return
        viewModelScope.launch {
            update { it.copy(creating = true) }
            try {
                ApiClient.calendarApi.create(
                    CreateCalendarEvent(
                        title = title,
                        date = selectedDateStr(),
                        time = s.formTime.trim().ifBlank { null },
                        description = s.formDescription.trim().ifBlank { null },
                        type = s.formType,
                        category = s.formCategory,
                    ),
                )
                update { it.copy(creating = false, showCreate = false, message = "Evento criado!") }
                load()
            } catch (e: Exception) {
                update { it.copy(creating = false, message = apiMessage(e) ?: "Erro ao criar evento.") }
            }
        }
    }

    // ---------- concluir / excluir ----------

    fun toggleComplete(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                val res = ApiClient.calendarApi.update(
                    event.id,
                    UpdateCalendarEvent(
                        title = event.title.orEmpty(),
                        date = event.date?.take(10) ?: selectedDateStr(),
                        time = event.time,
                        description = event.description,
                        type = event.type.orEmpty(),
                        category = event.category,
                        completed = !event.completed,
                    ),
                )
                if (!res.isSuccessful) throw HttpException(res)
                load()
            } catch (e: Exception) {
                update { it.copy(message = apiMessage(e) ?: "Falha ao atualizar.") }
            }
        }
    }

    fun askDelete(event: CalendarEvent) = update { it.copy(deleteTarget = event) }
    fun cancelDelete() {
        if (uiState.value.deleting) return
        update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val event = uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            update { it.copy(deleting = true) }
            try {
                val res = ApiClient.calendarApi.delete(event.id)
                if (!res.isSuccessful) throw HttpException(res)
                update { it.copy(deleting = false, deleteTarget = null, message = "Evento excluído.") }
                load()
            } catch (e: Exception) {
                update { it.copy(deleting = false, message = apiMessage(e) ?: "Erro ao excluir.") }
            }
        }
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

class CalendarViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalendarViewModel() as T
    }
}
