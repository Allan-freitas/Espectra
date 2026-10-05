package br.com.espectra.ui.pin

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.CredentialVault
import br.com.espectra.data.DeviceInfoProvider
import br.com.espectra.data.LoginRequest
import br.com.espectra.data.PinCheck
import br.com.espectra.data.PinManager
import br.com.espectra.data.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

data class UnlockUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val attemptsLeft: Int = PinManager.MAX_ATTEMPTS,
    val userName: String? = null,
)

/** Desbloqueio por PIN: confere o hash e refaz o login em silêncio. */
class UnlockViewModel(
    private val appContext: Context,
    private val session: SessionManager,
    private val pinManager: PinManager,
    private val vault: CredentialVault,
) : ViewModel() {

    var uiState = mutableStateOf(
        UnlockUiState(
            attemptsLeft = pinManager.attemptsLeft(),
            userName = session.getUserName(),
        ),
    )
        private set

    private fun update(transform: (UnlockUiState) -> UnlockUiState) {
        uiState.value = transform(uiState.value)
    }

    fun clearError() = update { it.copy(error = null) }

    /**
     * Retorna o nome logado em sucesso, ou null se deve cair no login completo
     * (PIN bloqueado ou credenciais inválidas).
     */
    fun unlock(pin: String, onResult: (loggedName: String?) -> Unit) {
        if (uiState.value.loading) return
        when (pinManager.check(pin)) {
            PinCheck.LOCKED -> {
                wipe()
                onResult(null)
                return
            }
            PinCheck.WRONG -> {
                update {
                    it.copy(error = "PIN incorreto.", attemptsLeft = pinManager.attemptsLeft())
                }
                return
            }
            PinCheck.OK -> Unit
        }
        val creds = vault.load()
        if (creds == null) {
            wipe()
            onResult(null)
            return
        }
        update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val deviceInfo = try {
                    DeviceInfoProvider.build(appContext)
                } catch (e: Exception) {
                    null
                }
                val response = ApiClient.authApi.login(LoginRequest(creds.first, creds.second, deviceInfo))
                session.saveSession(response)
                val name = "${response.name} ${response.lastName}".trim()
                update { it.copy(loading = false) }
                onResult(name)
            } catch (e: Exception) {
                // Senha trocada ou outro problema: volta ao login completo.
                update { it.copy(loading = false) }
                if (e is HttpException && e.code() != 401) {
                    update { it.copy(error = apiMessage(e) ?: "Falha ao entrar.") }
                    return@launch
                }
                wipe()
                onResult(null)
            }
        }
    }

    private fun wipe() {
        vault.clear()
        pinManager.clear()
    }

    private fun apiMessage(e: HttpException): String? {
        if (e is IOException) return "Sem conexão. Verifique sua internet."
        return try {
            val body = e.response()?.errorBody()?.string() ?: return null
            JSONObject(body).optString("message").takeIf { it.isNotBlank() }
        } catch (e2: Exception) {
            null
        }
    }
}

class UnlockViewModelFactory(
    private val appContext: Context,
    private val session: SessionManager,
    private val pinManager: PinManager,
    private val vault: CredentialVault,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UnlockViewModel(appContext.applicationContext, session, pinManager, vault) as T
    }
}
