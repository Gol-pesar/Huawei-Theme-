package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "huawei_theme_prefs")

data class AppPreferences(
    val huaweiFolderUri: String? = null,
    val huaweiFolderDisplayName: String? = null,
    val isDarkTheme: Boolean = true,
    val customOnlineSources: List<String> = emptyList()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val HUAWEI_FOLDER_URI = stringPreferencesKey("huawei_folder_uri")
        val HUAWEI_FOLDER_NAME = stringPreferencesKey("huawei_folder_name")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val CUSTOM_ONLINE_SOURCES = stringSetPreferencesKey("custom_online_sources")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            huaweiFolderUri = prefs[Keys.HUAWEI_FOLDER_URI],
            huaweiFolderDisplayName = prefs[Keys.HUAWEI_FOLDER_NAME],
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            customOnlineSources = (prefs[Keys.CUSTOM_ONLINE_SOURCES] ?: emptySet()).sorted()
        )
    }

    suspend fun setHuaweiFolder(uri: String?, displayName: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.HUAWEI_FOLDER_URI)
                prefs.remove(Keys.HUAWEI_FOLDER_NAME)
            } else {
                prefs[Keys.HUAWEI_FOLDER_URI] = uri
                prefs[Keys.HUAWEI_FOLDER_NAME] = displayName ?: "Huawei/Themes"
            }
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = isDark
        }
    }

    suspend fun addCustomOnlineSource(source: String) {
        val trimmed = source.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_ONLINE_SOURCES] ?: emptySet()
            prefs[Keys.CUSTOM_ONLINE_SOURCES] = current + trimmed
        }
    }

    suspend fun removeCustomOnlineSource(source: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_ONLINE_SOURCES] ?: emptySet()
            prefs[Keys.CUSTOM_ONLINE_SOURCES] = current - source
        }
    }
}
