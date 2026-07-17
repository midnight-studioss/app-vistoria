package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_v2")

class UserPreferences(private val context: Context) {
    private val USER_NAME = stringPreferencesKey("user_name")
    private val COMPANY_NAME = stringPreferencesKey("company_name")

    val userName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME]
        }

    val companyName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[COMPANY_NAME]
        }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun saveCompanyName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[COMPANY_NAME] = name
        }
    }
}
