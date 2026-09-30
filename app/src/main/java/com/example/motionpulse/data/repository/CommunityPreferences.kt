package com.example.motionpulse.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.communityDataStore: DataStore<Preferences> by preferencesDataStore(name = "community_settings")

class CommunityPreferences(private val context: Context) {

    companion object {
        private val KEY_HAS_SEEN_TIP = booleanPreferencesKey("has_seen_community_tip")
    }

    val hasSeenCommunityTipFlow: Flow<Boolean> = context.communityDataStore.data
        .map { preferences ->
            preferences[KEY_HAS_SEEN_TIP] ?: false
        }

    suspend fun setCommunityTipSeen(seen: Boolean = true) {
        context.communityDataStore.edit { preferences ->
            preferences[KEY_HAS_SEEN_TIP] = seen
        }
    }
}
