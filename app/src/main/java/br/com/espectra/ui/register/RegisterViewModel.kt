package br.com.espectra.ui.register

import android.content.Context
import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.DeviceInfoProvider
import br.com.espectra.data.RegisterRequest
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

data class RegisterUiState(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val birthday: String = "",
    val gender: String = "",
    val nameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val genderError: String? = null,
    val loading: Boolean = false,
    val formError: String? = null,
    val registered: Boolean = false,
)

/** Mesmas regras do web (register.component.ts + API RegisterRequest). */
class RegisterViewModel(
    private val appContext: Context,
) : ViewModel() {

    var uiState = mutableStateOf(RegisterUiState())
        private set

    private fun update(transform: (RegisterUiState) -> RegisterUiState) {
        uiState.value = transform(uiState.value)
    }

    fun onNameChange(v: String) = update { it.copy(name = v, nameError = null, formError = null) }
    fun onLastNameChange(v: String) = update { it.copy(lastName = v, lastNameError = null, formError = null) }
    fun onEmailChange(v: String) = update { it.copy(email = v, emailError = null, formError = null) }
    fun onPasswordChange(v: String) = update { it.copy(password = v, passwordError = null, formError = null) }
    fun onConfirmPasswordChange(v: String) = update { it.copy(confirmPassword = v, confirmPasswordError = null, formError = null) }
    fun onBirthdayChange(v: String) = update { it.copy(birthday = v, formError = null) }
    fun onGenderChange(v: String) = update { it.copy(gender = v, genderError = null, formError = null) }

    fun register() {
        val s = uiState.value
        val name = s.name.trim()
        val lastName = s.lastName.trim()
        val email = s.email.trim()
        val password = s.password
        val confirmPassword = s.confirmPassword
        val birthday = s.birthday.trim().takeIf { it.isNotBlank() }
        val gender = s.gender.trim()

        val nameError = if (name.isEmpty()) "Name is required" else null
        val lastNameError = if (lastName.isEmpty()) "Last name is required" else null
        val emailError = when {
            email.isEmpty() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
        val passwordError = when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        val confirmPasswordError = when {
            confirmPassword.isEmpty() -> "Confirm password is required"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
        val genderError = if (gender.isEmpty()) "Gender is required" else null

        if (nameError != null || lastNameError != null || emailError != null ||
            passwordError != null || confirmPasswordError != null || genderError != null
        ) {
            update {
                it.copy(
                    nameError = nameError,
                    lastNameError = lastNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    genderError = genderError,
                )
            }
            return
        }

        update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                val deviceInfo = try {
                    DeviceInfoProvider.build(appContext)
                } catch (e: Exception) {
                    null
                }
                ApiClient.authApi.register(
                    RegisterRequest(
                        name = name,
                        lastName = lastName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        birthday = birthday,
                        gender = gender,
                        deviceInfo = deviceInfo,
                    ),
                )
                update { it.copy(loading = false, registered = true) }
            } catch (e: HttpException) {
                update { it.copy(loading = false, formError = apiMessage(e) ?: "Registration failed. Please try again.") }
            } catch (e: IOException) {
                update { it.copy(loading = false, formError = "Sem conexão. Verifique sua internet.") }
            } catch (e: Exception) {
                update { it.copy(loading = false, formError = "Algo deu errado. Tente de novo.") }
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

class RegisterViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RegisterViewModel(appContext.applicationContext) as T
    }
}
