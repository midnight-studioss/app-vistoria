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
    private val EMAIL_RECIPIENT = stringPreferencesKey("email_recipient")
    private val EMAIL_SENDER = stringPreferencesKey("email_sender")
    private val RESEND_API_KEY = stringPreferencesKey("resend_api_key")
    private val SEND_METHOD = stringPreferencesKey("send_method") // "manual", "resend", "webhook", "smtp"
    private val WEBHOOK_URL = stringPreferencesKey("webhook_url")
    private val SMTP_HOST = stringPreferencesKey("smtp_host")
    private val SMTP_PORT = stringPreferencesKey("smtp_port")
    private val SMTP_USERNAME = stringPreferencesKey("smtp_username")
    private val SMTP_PASSWORD = stringPreferencesKey("smtp_password")

    val userName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME]
        }

    val companyName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[COMPANY_NAME]
        }

    val emailRecipient: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[EMAIL_RECIPIENT]
        }

    val emailSender: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[EMAIL_SENDER]
        }

    val resendApiKey: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[RESEND_API_KEY]
        }

    val sendMethod: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SEND_METHOD] ?: "manual"
        }

    val webhookUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[WEBHOOK_URL]
        }

    val smtpHost: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SMTP_HOST] ?: "smtp.gmail.com"
        }

    val smtpPort: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SMTP_PORT] ?: "587"
        }

    val smtpUsername: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SMTP_USERNAME]
        }

    val smtpPassword: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SMTP_PASSWORD]
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

    suspend fun saveEmailRecipient(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL_RECIPIENT] = email
        }
    }

    suspend fun saveEmailSender(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL_SENDER] = email
        }
    }

    suspend fun saveResendApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[RESEND_API_KEY] = key
        }
    }

    suspend fun saveSendMethod(method: String) {
        context.dataStore.edit { preferences ->
            preferences[SEND_METHOD] = method
        }
    }

    suspend fun saveWebhookUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[WEBHOOK_URL] = url
        }
    }

    suspend fun saveSmtpHost(host: String) {
        context.dataStore.edit { preferences ->
            preferences[SMTP_HOST] = host
        }
    }

    suspend fun saveSmtpPort(port: String) {
        context.dataStore.edit { preferences ->
            preferences[SMTP_PORT] = port
        }
    }

    suspend fun saveSmtpUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[SMTP_USERNAME] = username
        }
    }

    suspend fun saveSmtpPassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[SMTP_PASSWORD] = password
        }
    }
}
