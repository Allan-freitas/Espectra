package br.com.espectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import br.com.espectra.data.ApiClient
import br.com.espectra.data.SessionManager
import br.com.espectra.ui.calendar.CalendarViewModel
import br.com.espectra.ui.calendar.CalendarViewModelFactory
import br.com.espectra.ui.feed.FeedScreen
import br.com.espectra.ui.feed.FeedViewModel
import br.com.espectra.ui.feed.FeedViewModelFactory
import br.com.espectra.ui.friends.FriendsViewModel
import br.com.espectra.ui.friends.FriendsViewModelFactory
import br.com.espectra.ui.photos.AlbumsViewModel
import br.com.espectra.ui.photos.AlbumsViewModelFactory
import br.com.espectra.ui.forgot.ForgotPasswordScreen
import br.com.espectra.ui.forgot.ForgotPasswordViewModel
import br.com.espectra.ui.forgot.ForgotPasswordViewModelFactory
import br.com.espectra.ui.login.LoginScreen
import br.com.espectra.ui.login.LoginViewModel
import br.com.espectra.ui.login.LoginViewModelFactory
import br.com.espectra.ui.profile.ProfileScreen
import br.com.espectra.ui.profile.ProfileViewModel
import br.com.espectra.ui.profile.ProfileViewModelFactory
import br.com.espectra.ui.register.RegisterScreen
import br.com.espectra.ui.register.RegisterViewModel
import br.com.espectra.ui.register.RegisterViewModelFactory
import br.com.espectra.ui.theme.EspectraTheme

private enum class AuthScreen { Login, Register, Forgot }

class MainActivity : ComponentActivity() {

    private val session by lazy { SessionManager(applicationContext) }

    private val loginViewModel by lazy {
        ViewModelProvider(this, LoginViewModelFactory(session, applicationContext))[LoginViewModel::class.java]
    }

    private val registerViewModel by lazy {
        ViewModelProvider(this, RegisterViewModelFactory(applicationContext))[RegisterViewModel::class.java]
    }

    private val forgotViewModel by lazy {
        ViewModelProvider(this, ForgotPasswordViewModelFactory())[ForgotPasswordViewModel::class.java]
    }

    private var feedViewModel: FeedViewModel? = null
    private var friendsViewModel: FriendsViewModel? = null
    private var albumsViewModel: AlbumsViewModel? = null
    private var calendarViewModel: CalendarViewModel? = null

    private fun getFeedViewModel(): FeedViewModel {
        val current = feedViewModel
        if (current != null) return current
        val created = ViewModelProvider(this, FeedViewModelFactory(session))[FeedViewModel::class.java]
        feedViewModel = created
        return created
    }

    private fun getFriendsViewModel(): FriendsViewModel {
        val current = friendsViewModel
        if (current != null) return current
        val created = ViewModelProvider(this, FriendsViewModelFactory())[FriendsViewModel::class.java]
        friendsViewModel = created
        return created
    }

    private fun getAlbumsViewModel(): AlbumsViewModel {
        val current = albumsViewModel
        if (current != null) return current
        val created = ViewModelProvider(this, AlbumsViewModelFactory())[AlbumsViewModel::class.java]
        albumsViewModel = created
        return created
    }

    private fun getCalendarViewModel(): CalendarViewModel {
        val current = calendarViewModel
        if (current != null) return current
        val created = ViewModelProvider(this, CalendarViewModelFactory())[CalendarViewModel::class.java]
        calendarViewModel = created
        return created
    }

    /** Sinal (observado no setContent) de que o token morreu (401). */
    private val expireSignal = mutableStateOf(0)

    private fun handleSessionExpired() {
        // Ignora 401s de chamadas já em voo após o logout.
        if (!session.hasSession()) return
        session.clear()
        feedViewModel = null
        friendsViewModel = null
        albumsViewModel = null
        calendarViewModel = null
        expireSignal.value++
    }

    private fun getProfileViewModel(userId: String?, fallbackName: String?): ProfileViewModel {
        return ViewModelProvider(this, ProfileViewModelFactory(userId, fallbackName, session))
            .get("profile_${userId ?: "me"}", ProfileViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.tokenProvider = { session.getToken() }
        ApiClient.onUnauthorized = { handleSessionExpired() }
        enableEdgeToEdge()
        setContent {
            // Identidade do app é clara (creme/branco) — não segue o modo escuro do sistema.
            EspectraTheme(darkTheme = false, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var loggedUser by remember { mutableStateOf(session.getUserName()) }
                    var screen by remember { mutableStateOf(AuthScreen.Login) }
                    var profile by remember { mutableStateOf<Pair<String, String?>?>(null) }
                    val expired = expireSignal.value

                    androidx.compose.runtime.LaunchedEffect(expired) {
                        if (expired > 0 && loggedUser != null) {
                            profile = null
                            loggedUser = null
                            screen = AuthScreen.Login
                            loginViewModel.notifySessionExpired()
                        }
                    }

                    if (loggedUser != null) {
                        val target = profile
                        if (target != null) {
                            ProfileScreen(
                                viewModel = getProfileViewModel(target.first, target.second),
                                onBack = { profile = null },
                                modifier = Modifier.padding(innerPadding),
                            )
                        } else {
                            FeedScreen(
                                viewModel = getFeedViewModel(),
                                friendsViewModel = getFriendsViewModel(),
                                albumsViewModel = getAlbumsViewModel(),
                                calendarViewModel = getCalendarViewModel(),
                                userName = loggedUser,
                                onLogout = {
                                    session.clear()
                                    feedViewModel = null
                                    friendsViewModel = null
                                    albumsViewModel = null
                                    calendarViewModel = null
                                    loggedUser = null
                                    screen = AuthScreen.Login
                                },
                                onAuthorClick = { userId, userName -> profile = userId to userName },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    } else {
                        when (screen) {
                            AuthScreen.Login -> LoginScreen(
                                viewModel = loginViewModel,
                                onLoggedIn = { loggedUser = it },
                                onRegisterClick = { screen = AuthScreen.Register },
                                onForgotPasswordClick = { screen = AuthScreen.Forgot },
                                modifier = Modifier.padding(innerPadding),
                            )
                            AuthScreen.Register -> RegisterScreen(
                                viewModel = registerViewModel,
                                onRegistered = { screen = AuthScreen.Login },
                                onBackToLogin = { screen = AuthScreen.Login },
                                modifier = Modifier.padding(innerPadding),
                            )
                            AuthScreen.Forgot -> ForgotPasswordScreen(
                                viewModel = forgotViewModel,
                                onBackToLogin = { screen = AuthScreen.Login },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}
