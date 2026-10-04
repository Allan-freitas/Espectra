package br.com.espectra.ui.login

import android.content.Context
import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.DeviceInfoProvider
import br.com.espectra.data.LoginRequest
import br.com.espectra.data.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val loading: Boolean = false,
    val loginError: String? = null,
    val loggedInName: String? = null,
)

/** Mesmas regras do web (landing.component.ts): required + email válido. */
class LoginViewModel(
    private val session: SessionManager,
    private val appContext: Context,
) : ViewModel() {

    var uiState = mutableStateOf(LoginUiState())
        private set

    private fun update(transform: (LoginUiState) -> LoginUiState) {
        uiState.value = transform(uiState.value)
    }

    fun onEmailChange(value: String) {
        update { it.copy(email = value, emailError = null, loginError = null) }
    }

    fun onPasswordChange(value: String) {
        update { it.copy(password = value, passwordError = null, loginError = null) }
    }

    fun dismissError() {
        update { it.copy(loginError = null) }
    }

    /** Chamado quando a API devolve 401 com o app em uso (JWT de 60 min expirado). */
    fun notifySessionExpired() {
        update { it.copy(loginError = "Sua sessão expirou. Entre novamente.") }
    }

    fun login() {
        val email = uiState.value.email.trim()
        val password = uiState.value.password

        val emailError = when {
            email.isEmpty() -> "Email é obrigatório"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Formato de email inválido"
            else -> null
        }
        val passwordError = if (password.isEmpty()) "Senha é obrigatória" else null

        if (emailError != null || passwordError != null) {
            update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        update { it.copy(loading = true, loginError = null) }
        viewModelScope.launch {
            try {
                val deviceInfo = try {
                    DeviceInfoProvider.build(appContext)
                } catch (e: Exception) {
                    null
                }
                val response = ApiClient.authApi.login(LoginRequest(email, password, deviceInfo))
                session.saveSession(response)
                update { it.copy(loading = false, loggedInName = "${response.name} ${response.lastName}".trim()) }
            } catch (e: HttpException) {
                update { it.copy(loading = false, loginError = apiMessage(e) ?: "Email ou senha inválidos.") }
            } catch (e: IOException) {
                update { it.copy(loading = false, loginError = "Sem conexão. Verifique sua internet.") }
            } catch (e: Exception) {
                update { it.copy(loading = false, loginError = "Algo deu errado. Tente de novo.") }
            }
        }
    }

    private fun apiMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string() ?: return null
            JSONObject(body).optString("message").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

/** ViewModelProvider direto (evita o artefato lifecycle-viewmodel-compose). */
class LoginViewModelFactory(
    private val session: SessionManager,
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(session, appContext.applicationContext) as T
    }
}
