package com.stargallery.app.data.remote

/**
 * Central TMDB configuration.
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ TODO: ADD YOUR OWN TMDB CREDENTIALS HERE (leave both empty to ship   │
 * │       without credentials; the app then shows an                     │
 * │       "API not configured" screen).                                  │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * Option A (recommended for TMDB v4 auth): paste your "API Read Access Token"
 *           into [API_READ_ACCESS_TOKEN]. It is sent as `Authorization: Bearer ...`.
 * Option B: paste your v3 "API Key" into [API_KEY]. It is sent as the `api_key`
 *           query parameter.
 * Option C (recommended for production): leave both empty and set
 *           [BACKEND_PROXY_URL] to your own server that forwards requests to
 *           TMDB and injects the credential server-side. See README.md.
 *
 * Get credentials at https://www.themoviedb.org/settings/api
 *
 * IMPORTANT
 *  - Never commit real credentials to a public repository.
 *  - Anything compiled into an APK can be extracted by a determined user.
 *  - Credentials are never logged by this app: there is no HTTP logging
 *    interceptor, and error messages never include request URLs.
 */
object TmdbConfig {

    /** TODO: paste your TMDB "API Read Access Token" (v4 auth) between the quotes. */
    val API_READ_ACCESS_TOKEN: String = ""

    /** TODO: or paste your TMDB v3 "API Key" between the quotes. */
    val API_KEY: String = "2e211dfda888f7cc55ce433d743f9bc3"

    /**
     * TODO (optional, production): URL of your own backend proxy, e.g.
     * "https://tmdb-proxy.example.com/3/". When set, no credential is attached
     * by the app; the proxy must add it. Must be HTTPS.
     */
    val BACKEND_PROXY_URL: String = ""

    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

    val usesProxy: Boolean get() = BACKEND_PROXY_URL.isNotBlank()

    val baseUrl: String
        get() = if (usesProxy) {
            BACKEND_PROXY_URL.trim().let { if (it.endsWith("/")) it else "$it/" }
        } else {
            TMDB_BASE_URL
        }

    val isConfigured: Boolean
        get() = usesProxy || API_READ_ACCESS_TOKEN.isNotBlank() || API_KEY.isNotBlank()
}
