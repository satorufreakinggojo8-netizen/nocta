package com.nocta.app.data.remote

import com.nocta.app.BuildConfig

/**
 * Whether this build actually has a coach backend.
 *
 * The repository ships with a placeholder base URL. Pretending the coach is
 * available would mean showing endless "try again" failures, so the app asks
 * this first and shows a truthful "not configured" state instead.
 */
object BackendConfig {

    val baseUrl: String = BuildConfig.API_BASE_URL

    val isConfigured: Boolean get() = isConfigured(baseUrl)

    fun isConfigured(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return false
        return !trimmed.contains(PLACEHOLDER_HOST)
    }

    private const val PLACEHOLDER_HOST = "api.nocta.example.com"
}
