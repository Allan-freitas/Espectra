package br.com.espectra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import br.com.espectra.data.SessionManager
import br.com.espectra.ui.forgot.ForgotPasswordScreen
import br.com.espectra.ui.forgot.ForgotPasswordViewModel
import br.com.espectra.ui.forgot.ForgotPasswordViewModelFactory
import br.com.espectra.ui.login.LoginScreen
import br.com.espectra.ui.login.LoginViewModel
import br.com.espectra.ui.login.LoginViewModelFactory
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EspectraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var loggedUser by remember { mutableStateOf(session.getUserName()) }
                    var screen by remember { mutableStateOf(AuthScreen.Login) }

                    if (loggedUser != null) {
                        LoggedInPlaceholder(
                            name = loggedUser!!,
                            onLogout = {
                                session.clear()
                                loggedUser = null
                                screen = AuthScreen.Login
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
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

// Placeholder até as próximas telas (feed, chat...) chegarem
@Composable
private fun LoggedInPlaceholder(
    name: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "♾️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Olá, $name!",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E293B),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Você está logado na Espectra.",
            fontSize = 15.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF97316),
                contentColor = Color.White,
            ),
        ) {
            Text(text = "Sair", fontWeight = FontWeight.Bold)
        }
    }
}
