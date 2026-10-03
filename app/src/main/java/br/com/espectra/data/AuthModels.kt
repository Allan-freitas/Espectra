package br.com.espectra.data

/**
 * Espelha a API .NET (JSON camelCase):
 * POST {apiUrl}/auth/login -> { userId, name, lastName, email, birthday,
 *   gender, userAvatarUrl, userCoverUrl, token }
 * Erros -> { message }
 *
 * DeviceInfo é opcional na API (LoginRequest.DeviceInfo / RegisterRequest.DeviceInfo),
 * mas o web sempre envia para auditoria (LoginHistory). O mobile faz o mesmo.
 */
data class DeviceInfo(
    val browser: String? = null,
    val browserVersion: String? = null,
    val os: String? = null,
    val osVersion: String? = null,
    val device: String? = null,
    val deviceType: String? = null,
    val orientation: String? = null,
    val userAgent: String? = null,
    val isDesktopMode: Boolean = false,
    val ipAddress: String? = null,
)

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceInfo: DeviceInfo? = null,
)

data class LoginResponse(
    val userId: String,
    val name: String,
    val lastName: String,
    val email: String,
    val birthday: String?,
    val gender: String,
    val userAvatarUrl: String?,
    val userCoverUrl: String?,
    val token: String,
)

data class RegisterRequest(
    val name: String,
    val lastName: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val birthday: String? = null,
    val gender: String,
    val deviceInfo: DeviceInfo? = null,
)

data class ForgotPasswordRequest(
    val email: String,
)

data class MessageResponse(
    val message: String?,
)
