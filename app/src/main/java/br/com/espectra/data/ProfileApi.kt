package br.com.espectra.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espelha PersonalInformationService, HobbiesService, EducationService,
 * EmploymentService e os contadores do ProfileHeaderComponent.
 * Sem userId = perfil próprio (igual ao web).
 */
interface ProfileApi {
    @GET("personalInformation")
    suspend fun getPersonalInfo(@Query("userId") userId: String? = null): PersonalInformation

    @GET("HobbiesAndInterests")
    suspend fun getHobbies(@Query("userId") userId: String? = null): Hobbies

    @GET("education")
    suspend fun getEducation(@Query("userId") userId: String? = null): List<EducationEntry>

    @GET("employment")
    suspend fun getEmployment(@Query("userId") userId: String? = null): List<EmploymentEntry>

    @GET("posts/count")
    suspend fun getPostCount(): Int

    @GET("posts/user/{userId}/count")
    suspend fun getPostCountByUser(@Path("userId") userId: String): Int

    @PUT("profile/avatar")
    suspend fun uploadAvatar(@Body dto: ImageUploadRequest): retrofit2.Response<Unit>

    @PUT("profile/cover")
    suspend fun uploadCover(@Body dto: ImageUploadRequest): retrofit2.Response<Unit>
}
