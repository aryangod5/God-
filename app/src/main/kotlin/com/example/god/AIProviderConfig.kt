package com.example.god

data class AIProviderConfig(
    val providerName: String = "",
    val endpoint: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val enabled: Boolean = false
)
