package br.com.espectra.data

/**
 * Espelha personal-information.ts, hobbies.ts, education-entry.ts e
 * employment-entry.ts do web.
 *
 * Campos nuláveis: a API pode devolver null e o Gson injeta null mesmo em
 * propriedades Kotlin não-nulas — daí o NPE no isBlank. Quem usa trata
 * null como vazio.
 */
data class PersonalInformation(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val birthday: String? = null,
    val website: String? = null,
    val phone: String? = null,
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val about: String? = null,
    val gender: String? = null,
    val religiousBeliefs: String? = null,
    val birthplace: String? = null,
    val occupation: String? = null,
    val status: String? = null,
    val politicalIncline: String? = null,
    val facebook: String? = null,
    val twitter: String? = null,
    val rss: String? = null,
    val dribbble: String? = null,
    val spotify: String? = null,
)

data class Hobbies(
    val hobbies: String? = null,
    val favouriteTvShows: String? = null,
    val favouriteMovies: String? = null,
    val favouriteGames: String? = null,
    val favouriteMusicBands: String? = null,
    val favouriteBooks: String? = null,
    val favouriteWriters: String? = null,
    val otherInterests: String? = null,
)

data class EducationEntry(
    val title: String? = null,
    val period: String? = null,
    val description: String? = null,
)

data class EmploymentEntry(
    val title: String? = null,
    val period: String? = null,
    val description: String? = null,
)

/**
 * Espelha ProfileService do web: PUT profile/avatar|cover com o corpo
 * `{ imageData: number[] }` (bytes sem sinal, como Array.from(Uint8Array)).
 */
data class ImageUploadRequest(
    val imageData: List<Int>,
)
