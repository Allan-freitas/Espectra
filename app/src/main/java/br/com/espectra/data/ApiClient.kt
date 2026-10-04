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

    /**
     * Chamado uma vez por resposta 401 de rota autenticada (token expirado —
     * o JWT dura 60 min e a API não tem refresh). Rotas públicas de auth
     * (login/cadastro) nunca disparam.
     */
    @Volatile
    var onUnauthorized: () -> Unit = {}

    private val publicAuthPaths = setOf(
        "auth/login", "auth/register", "auth/forgot-password",
        "auth/reset-password", "auth/confirm-email",
    )

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider()
        val original = chain.request()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        val response = chain.proceed(request)
        // OkHttp 3.x (transitivo do Retrofit): url() e code() são métodos.
        val path = original.url().encodedPath().removePrefix("/api/").trimStart('/')
        if (response.code() == 401 && publicAuthPaths.none { path.equals(it, ignoreCase = true) }) {
            // Roda em thread de IO: avisa na thread principal.
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    onUnauthorized()
                } catch (e: Exception) {
                    // nunca quebrar a chamada por causa do listener
                }
            }
        }
        response
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
