package com.rivo.ai.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "rivo_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(configId: Long, apiKey: String) {
        encryptedPrefs.edit().putString("api_key_$configId", apiKey).apply()
    }

    fun getApiKey(configId: Long): String? {
        return encryptedPrefs.getString("api_key_$configId", null)
    }

    fun deleteApiKey(configId: Long) {
        encryptedPrefs.edit().remove("api_key_$configId").apply()
    }

    fun saveTempApiKey(tempId: String, apiKey: String) {
        encryptedPrefs.edit().putString("temp_$tempId", apiKey).apply()
    }

    fun getTempApiKey(tempId: String): String? {
        return encryptedPrefs.getString("temp_$tempId", null)
    }

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}
