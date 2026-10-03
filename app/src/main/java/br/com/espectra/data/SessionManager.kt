package br.com.espectra.data

import android.content.Context
import android.content.SharedPreferences

/** Sessão local — mesmas chaves do app web (auth_token, user_id, ...). */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(response: LoginResponse) {
        prefs.edit()
            .putString(KEY_TOKEN, response.token)
            .putString(KEY_USER_ID, response.userId)
            .putString(KEY_USER_NAME, "${response.name} ${response.lastName}".trim())
            .putString(KEY_USER_EMAIL, response.email)
            .apply()
        response.userAvatarUrl?.let {
            prefs.edit().putString(KEY_USER_AVATAR, API_ORIGIN + it).apply()
        }
        response.userCoverUrl?.let {
            prefs.edit().putString(KEY_USER_COVER, API_ORIGIN + it).apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasSession(): Boolean = prefs.getString(KEY_TOKEN, null) != null

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getUserCover(): String? = prefs.getString(KEY_USER_COVER, null)

    companion object {
        private const val PREFS_NAME = "espectra_session"
        private const val API_ORIGIN = "https://agfapp.com"
        const val KEY_TOKEN = "auth_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_AVATAR = "user_avatar"
        const val KEY_USER_COVER = "user_cover"
    }
}
