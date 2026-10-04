package br.com.espectra.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    const val BASE_URL = "https://agfapp.com/api/"
    const val API_ORIGIN = "https://agfapp.com"

    /** Preenchido no login (equivale ao jwt.interceptor.ts do web: Bearer). */
    @Volatile
    var tokenProvider: () -> String? = { null }

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    internal fun httpClient(): OkHttpClient = httpClient

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    val feedApi: FeedApi by lazy { retrofit.create(FeedApi::class.java) }

    val socialApi: SocialApi by lazy { retrofit.create(SocialApi::class.java) }

    val profileApi: ProfileApi by lazy { retrofit.create(ProfileApi::class.java) }

    val albumApi: AlbumApi by lazy { retrofit.create(AlbumApi::class.java) }

    val calendarApi: CalendarApi by lazy { retrofit.create(CalendarApi::class.java) }
}
