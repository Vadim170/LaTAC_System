package com.kontakt1.tmonitor.settings

import android.content.SharedPreferences

/**
 * Класс для работы с настройками приложения. Позволяет их считывать, сохранять в память устройства.
 * @param sharedPreferences дирректория в системе андроид для хранения данных.
 */
class SettingsController(
    private val sharedPreferences: SharedPreferences
) {
    var settingsData = Settings()

    init {
        if(sharedPreferences.all.isEmpty()) save() // Если настроек нет, то сохраним настройки по умолчанию
        settingsData = readData(sharedPreferences)
    }

    fun save() {
        sharedPreferences.edit().apply {
            settingsData.apply {
                putString(SELECTED_SYSTEM, selectedSystem)
                putBoolean(USE_REST_SERVER, useRestServer)
                putBoolean(IS_SERVICE_ENABLED, isServiceEnabled)
                putBoolean(IS_AUTOFILL_ON, isAutofillOn)
                putBoolean(IS_DISPLAY_TIME_BY_SERVER_TIME_ZONE, isDisplayTimeByServerTimeZone)
                putBoolean(IS_ENABLED_DEFAULT_DB_NAME, isEnabledDefaultDBName)

                if (!isAutofillOn) {
                    val defaultSettings =
                        Settings()
                    putString(SAVED_DATABASE_NAME, defaultSettings.databaseName)
                    putString(SAVED_LOGIN, defaultSettings.login)
                    putString(SAVED_PASS, defaultSettings.password)
                    putString(SAVED_ADDRESS, defaultSettings.address)
                    putInt(SAVED_PORT, defaultSettings.port)
                    putString(FCM_TOPIC, defaultSettings.fcmtopic)
                } else {
                    putString(SAVED_DATABASE_NAME, databaseName)
                    putString(SAVED_LOGIN, login)
                    putString(SAVED_PASS, password)
                    putString(SAVED_ADDRESS, address)
                    putInt(SAVED_PORT, port)
                    putString(FCM_TOPIC, fcmtopic)
                }
            }
        }.apply()
    }

    fun load() {
        settingsData = readData(sharedPreferences)
    }

    private fun readData(sharedPreferences: SharedPreferences): Settings {
        val default = Settings()
        with(sharedPreferences) {
            return Settings(
                selectedSystem = getString(SELECTED_SYSTEM, default.selectedSystem) ?: "",
                useRestServer = getBoolean(USE_REST_SERVER,default.useRestServer),
                isServiceEnabled = getBoolean(IS_SERVICE_ENABLED, default.isServiceEnabled),
                isAutofillOn = getBoolean(IS_AUTOFILL_ON, default.isAutofillOn),
                isEnabledDefaultDBName = getBoolean(
                    IS_ENABLED_DEFAULT_DB_NAME, default.isEnabledDefaultDBName
                ),
                databaseName = getString(SAVED_DATABASE_NAME, default.databaseName) ?: "",
                isDisplayTimeByServerTimeZone = getBoolean(
                    IS_DISPLAY_TIME_BY_SERVER_TIME_ZONE, default.isDisplayTimeByServerTimeZone
                ),
                login = getString(SAVED_LOGIN, default.login) ?: "",
                password = getString(SAVED_PASS, default.password) ?: "",
                address = getString(SAVED_ADDRESS, default.address) ?: "",
                port = getInt(SAVED_PORT, default.port),
                fcmtopic = getString(FCM_TOPIC, default.fcmtopic) ?: ""
            )
        }
    }

    companion object {
        private const val SELECTED_SYSTEM = "selectedSystem"
        private const val USE_REST_SERVER = "useRestServer"
        private const val IS_SERVICE_ENABLED = "service"
        private const val IS_AUTOFILL_ON = "autofill"
        private const val IS_DISPLAY_TIME_BY_SERVER_TIME_ZONE = "isDisplayTimeByServerTimeZone"
        private const val IS_ENABLED_DEFAULT_DB_NAME = "is_enabled_default_db_name"

        private const val SAVED_DATABASE_NAME = "saved_database_name"
        private const val SAVED_LOGIN = "saved_login"
        private const val SAVED_PASS = "saved_pass"
        private const val SAVED_ADDRESS = "saved_address"
        private const val SAVED_PORT = "saved_port"
        private const val FCM_TOPIC = ""
    }
}