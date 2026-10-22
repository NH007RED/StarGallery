package com.stargallery.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * TMDB size tokens. Valid profile sizes are w45, w185, h632 and original; valid poster sizes are
 * w92, w154, w185, w342, w500, w780 and original (see TMDB /configuration).
 */
enum class ImageQuality(val label: String, val profileSize: String, val posterSize: String, val heroSize: String) {
    LOW("Data saver", "w185", "w154", "h632"),
    MEDIUM("Balanced", "h632", "w342", "h632"),
    HIGH("High quality", "h632", "w500", "original")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: String = "en-US",
    val imageQuality: ImageQuality = ImageQuality.MEDIUM,
    /** How long a cached profile is considered fresh, in hours. */
    val cacheTtlHours: Int = 24 * 7
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val CACHE_TTL = intPreferencesKey("cache_ttl_hours")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.LANGUAGE] = code }
    suspend fun setImageQuality(q: ImageQuality) = context.dataStore.edit { it[Keys.IMAGE_QUALITY] = q.name }
    suspend fun setCacheTtlHours(hours: Int) = context.dataStore.edit { it[Keys.CACHE_TTL] = hours }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = this[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            language = this[Keys.LANGUAGE] ?: defaults.language,
            imageQuality = this[Keys.IMAGE_QUALITY]?.let { runCatching { ImageQuality.valueOf(it) }.getOrNull() }
                ?: defaults.imageQuality,
            cacheTtlHours = this[Keys.CACHE_TTL] ?: defaults.cacheTtlHours
        )
    }
}
