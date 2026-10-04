package br.com.espectra.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espelha calendar-event.ts e birthday-month.ts. Campos nuláveis
 * (Gson injeta null em propriedades Kotlin não-nulas).
 */
data class CalendarEvent(
    val id: String,
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    val completed: Boolean = false,
    val isBirthday: Boolean = false,
    val friendName: String? = null,
    val createdAt: String? = null,
)

data class CreateCalendarEvent(
    val title: String,
    val date: String,
    val time: String? = null,
    val description: String? = null,
    val type: String = "",
    val category: String? = null,
)

data class UpdateCalendarEvent(
    val title: String,
    val date: String,
    val time: String? = null,
    val description: String? = null,
    val type: String = "",
    val category: String? = null,
    val completed: Boolean = false,
)

data class BirthdayFriend(
    val name: String? = null,
    val date: String? = null,
    val initials: String? = null,
)

data class BirthdayMonth(
    val month: String? = null,
    val friends: List<BirthdayFriend> = emptyList(),
)

/** Espelha CalendarService do web (base `CalendarEvents`). */
interface CalendarApi {
    @GET("CalendarEvents")
    suspend fun getAll(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<CalendarEvent>

    @POST("CalendarEvents")
    suspend fun create(@Body dto: CreateCalendarEvent): CalendarEvent

    @PUT("CalendarEvents/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body dto: UpdateCalendarEvent,
    ): retrofit2.Response<Unit>

    @DELETE("CalendarEvents/{id}")
    suspend fun delete(@Path("id") id: String): retrofit2.Response<Unit>
}
