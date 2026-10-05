package br.com.espectra.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

enum class PinCheck { OK, WRONG, LOCKED }

/**
 * Guarda só o hash (SHA-256 + salt) do PIN, nunca o PIN. Após 5 erros,
 * bloqueia — quem chama deve apagar cofre+PIN e exigir login completo.
 */
class PinManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnrolled(): Boolean =
        !prefs.getString(KEY_HASH, null).isNullOrBlank()

    fun enroll(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, sha256(salt + pin))
            .putInt(KEY_ATTEMPTS, 0)
            .apply()
    }

    fun check(pin: String): PinCheck {
        if (!isEnrolled()) return PinCheck.LOCKED
        val salt = prefs.getString(KEY_SALT, null) ?: return PinCheck.LOCKED
        val ok = sha256(salt + pin) == prefs.getString(KEY_HASH, null)
        if (ok) {
            prefs.edit().putInt(KEY_ATTEMPTS, 0).apply()
            return PinCheck.OK
        }
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        prefs.edit().putInt(KEY_ATTEMPTS, attempts).apply()
        return if (attempts >= MAX_ATTEMPTS) PinCheck.LOCKED else PinCheck.WRONG
    }

    fun attemptsLeft(): Int = MAX_ATTEMPTS - prefs.getInt(KEY_ATTEMPTS, 0)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS = 5
        private const val PREFS_NAME = "espectra_pin"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_ATTEMPTS = "pin_attempts"
    }
}
