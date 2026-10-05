package br.com.espectra

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.espectra.data.ApiClient
import br.com.espectra.data.CredentialVault
import br.com.espectra.data.PinManager
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
import br.com.espectra.ui.pin.PinSetupScreen
import br.com.espectra.ui.pin.UnlockScreen
import br.com.espectra.ui.pin.UnlockViewModel
import br.com.espectra.ui.pin.UnlockViewModelFactory
import br.com.espectra.ui.profile.ProfileScreen
import br.com.espectra.ui.profile.ProfileViewModel
import br.com.espectra.ui.profile.ProfileViewModelFactory
import br.com.espectra.ui.register.RegisterScreen
import br.com.espectra.ui.register.RegisterViewModel
import br.com.espectra.ui.register.RegisterViewModelFactory
import br.com.espectra.ui.theme.EspectraTheme

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val HOME = "home"
    const val PROFILE = "profile/{userId}?userName={userName}"
    const val PIN_SETUP = "pin-setup"
    const val UNLOCK = "unlock"

    fun profile(userId: String, userName: String?): String =
        "profile/$userId?userName=${Uri.encode(userName.orEmpty())}"
}

class MainActivity : ComponentActivity() {

    private val session by lazy { SessionManager(applicationContext) }

    private val pinManager by lazy { PinManager(applicationContext) }

    private val vault by lazy { CredentialVault(applicationContext) }

    /** Credenciais do login recém-feito, aguardando oferta do PIN. */
    private var pendingCreds: Pair<String, String>? = null
    private var pendingUserName: String? = null

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

    private fun getProfileViewModel(userId: String?, fallbackName: String?): ProfileViewModel {
        return ViewModelProvider(this, ProfileViewModelFactory(userId, fallbackName, session))
            .get("profile_${userId ?: "me"}", ProfileViewModel::class.java)
    }

    private fun getUnlockViewModel(): UnlockViewModel {
        return ViewModelProvider(this, UnlockViewModelFactory(applicationContext, session, pinManager, vault))[UnlockViewModel::class.java]
    }

    private fun clearHomeViewModels() {
        feedViewModel = null
        friendsViewModel = null
        albumsViewModel = null
        calendarViewModel = null
    }

    /** Sinal (observado no setContent) de que o token morreu (401). */
    private val expireSignal = androidx.compose.runtime.mutableStateOf(0)

    private fun handleSessionExpired() {
        // Ignora 401s de chamadas já em voo após o logout.
        if (!session.hasSession()) return
        session.clear()
        clearHomeViewModels()
        expireSignal.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.tokenProvider = { session.getToken() }
        ApiClient.onUnauthorized = { handleSessionExpired() }
        enableEdgeToEdge()
        setContent {
            // Identidade do app é clara (creme/branco) — não segue o modo escuro do sistema.
            EspectraTheme(darkTheme = false, dynamicColor = false) {
                val navController = rememberNavController()
                val expired = expireSignal.value

                LaunchedEffect(expired) {
                    if (expired > 0) {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                        loginViewModel.notifySessionExpired()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = when {
                            session.hasSession() -> Routes.HOME
                            pinManager.isEnrolled() -> Routes.UNLOCK
                            else -> Routes.LOGIN
                        },
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                viewModel = loginViewModel,
                                onLoggedIn = { name ->
                                    val creds = loginViewModel.takeCredentials()
                                    if (pinManager.isEnrolled() && creds != null) {
                                        vault.save(creds.first, creds.second)
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    } else {
                                        pendingCreds = creds
                                        pendingUserName = name
                                        navController.navigate(Routes.PIN_SETUP) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    }
                                },
                                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                                onForgotPasswordClick = { navController.navigate(Routes.FORGOT) },
                            )
                        }
                        composable(Routes.REGISTER) {
                            RegisterScreen(
                                viewModel = registerViewModel,
                                onRegistered = { navController.popBackStack() },
                                onBackToLogin = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.FORGOT) {
                            ForgotPasswordScreen(
                                viewModel = forgotViewModel,
                                onBackToLogin = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.PIN_SETUP) {
                            PinSetupScreen(
                                userName = pendingUserName,
                                onComplete = { pin ->
                                    pinManager.enroll(pin)
                                    pendingCreds?.let { vault.save(it.first, it.second) }
                                    pendingCreds = null
                                    pendingUserName = null
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onSkip = {
                                    pendingCreds = null
                                    pendingUserName = null
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.UNLOCK) {
                            UnlockScreen(
                                viewModel = getUnlockViewModel(),
                                onUnlocked = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onUsePassword = {
                                    vault.clear()
                                    pinManager.clear()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.HOME) {
                            FeedScreen(
                                viewModel = getFeedViewModel(),
                                friendsViewModel = getFriendsViewModel(),
                                albumsViewModel = getAlbumsViewModel(),
                                calendarViewModel = getCalendarViewModel(),
                                userName = session.getUserName(),
                                onLogout = {
                                    session.clear()
                                    vault.clear()
                                    pinManager.clear()
                                    pendingCreds = null
                                    pendingUserName = null
                                    clearHomeViewModels()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onAuthorClick = { userId, userName ->
                                    navController.navigate(Routes.profile(userId, userName))
                                },
                            )
                        }
                        composable(
                            route = Routes.PROFILE,
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType },
                                navArgument("userName") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                            ),
                        ) { entry ->
                            val userId = entry.arguments?.getString("userId").orEmpty()
                            val userName = entry.arguments?.getString("userName")
                            ProfileScreen(
                                viewModel = getProfileViewModel(userId.ifBlank { null }, userName),
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
