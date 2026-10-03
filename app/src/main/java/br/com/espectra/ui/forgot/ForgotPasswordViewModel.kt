package br.com.espectra.ui.forgot

import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.ForgotPasswordRequest
import kotlinx.coroutines.launch
import java.io.IOException

data class ForgotUiState(
    val email: String = "",
    val emailError: String? = null,
    val loading: Boolean = false,
    val sent: Boolean = false,
    val formError: String? = null,
)

/** Espelha forgot-password.component.ts: POST auth/forgot-password { email }. */
class ForgotPasswordViewModel : ViewModel() {

    var uiState = mutableStateOf(ForgotUiState())
        private set

    private fun update(transform: (ForgotUiState) -> ForgotUiState) {
        uiState.value = transform(uiState.value)
    }

    fun onEmailChange(v: String) = update { it.copy(email = v, emailError = null, formError = null) }

    fun submit() {
        val email = uiState.value.email.trim()
        val emailError = when {
            email.isEmpty() -> "Email é obrigatório"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Formato de email inválido"
            else -> null
        }
        if (emailError != null) {
            update { it.copy(emailError = emailError) }
            return
        }

        update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                // API sempre retorna mensagem genérica (anti-enumeração) — ver AuthService.
                ApiClient.authApi.forgotPassword(ForgotPasswordRequest(email))
                update { it.copy(loading = false, sent = true) }
            } catch (e: IOException) {
                update { it.copy(loading = false, formError = "Sem conexão. Verifique sua internet.") }
            } catch (e: Exception) {
                update { it.copy(loading = false, formError = "Algo deu errado. Tente de novo.") }
            }
        }
    }
}

class ForgotPasswordViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ForgotPasswordViewModel() as T
    }
}
