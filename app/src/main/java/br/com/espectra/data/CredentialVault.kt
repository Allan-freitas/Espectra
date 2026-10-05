package br.com.espectra.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Cofre da senha para o PIN de acesso rápido: email+senha guardados
 * criptografados (Android Keystore). O PIN em si nunca descriptografa nada
 * sozinho — ele só libera esta leitura após o hash conferir (PinManager).
 */
class CredentialVault(context: Context) {

    private val appContext = context.applicationContext

    private fun prefs(): SharedPreferences? {
        return try {
            val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                alias,
                "espectra_vault",
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            null
        }
    }

    fun save(email: String, password: String) {
        try {
            prefs()?.edit()
                ?.putString(KEY_EMAIL, email)
                ?.putString(KEY_PASSWORD, password)
                ?.apply()
        } catch (e: Exception) {
            // cofre indisponível: o PIN simplesmente não será oferecido
        }
    }

    fun load(): Pair<String, String>? {
        return try {
            val p = prefs() ?: return null
            val email = p.getString(KEY_EMAIL, null)
            val password = p.getString(KEY_PASSWORD, null)
            if (email.isNullOrBlank() || password.isNullOrEmpty()) null else email to password
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        try {
            prefs()?.edit()?.clear()?.apply()
        } catch (e: Exception) {
        }
    }

    companion object {
        private const val KEY_EMAIL = "vault_email"
        private const val KEY_PASSWORD = "vault_password"
    }
}
