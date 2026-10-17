package com.example.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elitemax_prefs")

data class ElitePreferencesState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val hasSeenWelcome: Boolean = false,
    val defaultPhotoQuality: Int = 85,
    val defaultVideoPreset: String = "Balanced",
    val preserveMetadata: Boolean = true,
    val defaultExportFolder: String = "Movies/EliteMax & Pictures/EliteMax"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val DEFAULT_PHOTO_QUALITY = intPreferencesKey("default_photo_quality")
        val DEFAULT_VIDEO_PRESET = stringPreferencesKey("default_video_preset")
        val PRESERVE_METADATA = booleanPreferencesKey("preserve_metadata")
        val DEFAULT_EXPORT_FOLDER = stringPreferencesKey("default_export_folder")
    }

    val preferencesFlow: Flow<ElitePreferencesState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.LIGHT.name
        val mode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.LIGHT)
        ElitePreferencesState(
            themeMode = mode,
            hasSeenWelcome = prefs[Keys.HAS_SEEN_WELCOME] ?: false,
            defaultPhotoQuality = prefs[Keys.DEFAULT_PHOTO_QUALITY] ?: 85,
            defaultVideoPreset = prefs[Keys.DEFAULT_VIDEO_PRESET] ?: "Balanced",
            preserveMetadata = prefs[Keys.PRESERVE_METADATA] ?: true,
            defaultExportFolder = prefs[Keys.DEFAULT_EXPORT_FOLDER] ?: "Device Gallery (EliteMax)"
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHasSeenWelcome(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_WELCOME] = seen }
    }

    suspend fun setDefaultPhotoQuality(quality: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_PHOTO_QUALITY] = quality.coerceIn(30, 100) }
    }

    suspend fun setDefaultVideoPreset(preset: String) {
        context.dataStore.edit { it[Keys.DEFAULT_VIDEO_PRESET] = preset }
    }

    suspend fun setPreserveMetadata(preserve: Boolean) {
        context.dataStore.edit { it[Keys.PRESERVE_METADATA] = preserve }
    }

    suspend fun setDefaultExportFolder(folder: String) {
        context.dataStore.edit { it[Keys.DEFAULT_EXPORT_FOLDER] = folder }
    }
}
