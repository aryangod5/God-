package com.example.god

import android.content.Context

class AIProviderStorage(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "GOD_AI_PROVIDER",
            Context.MODE_PRIVATE
        )

    fun save(config: AIProviderConfig) {
        preferences.edit()
            .putString("provider_name", config.providerName)
            .putString("endpoint", config.endpoint)
            .putString("api_key", config.apiKey)
            .putString("model_name", config.modelName)
            .putBoolean("enabled", config.enabled)
            .apply()
    }

    fun load(): AIProviderConfig {

        return AIProviderConfig(
            providerName =
                preferences.getString("provider_name", "") ?: "",

            endpoint =
                preferences.getString("endpoint", "") ?: "",

            apiKey =
                preferences.getString("api_key", "") ?: "",

            modelName =
                preferences.getString("model_name", "") ?: "",

            enabled =
                preferences.getBoolean("enabled", false)
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun isConfigured(): Boolean {
        val config = load()

        return config.providerName.isNotBlank() &&
                config.endpoint.isNotBlank() &&
                config.apiKey.isNotBlank() &&
                config.modelName.isNotBlank()
    }
}
