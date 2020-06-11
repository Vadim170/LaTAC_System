package com.kontakt1.tmonitor.settings

/**
 * Класс для хранения настроек приложения. Задаёт настройки
 * по умолчанию при создании объекта.
 */
data class Settings(
    // Параметр хранящий адресс для хранения настроек
    // Задаю через setSharedPreferences в конструкторе MainActivity
    var selectedSystem: String = "АСКТ-01", // Выбранная система
    var useRestServer: Boolean = false,
    var subscribeFCM: Boolean = false,
    var isServiceEnabled: Boolean = false, // По умолчанию сервис отключен
    var isAutofillOn: Boolean = false, // По умолчанию выключено, так как это небезопасно
    var isDisplayTimeByServerTimeZone: Boolean = false, // По умолчанию используем время на устройстве клиента
    var isEnabledDefaultDBName: Boolean = true,
    // Параметры пдключения, на которые влияет автозаполнение
    var databaseName: String = "tmonitor",
    var address: String = "",
    var port: Int = 3306,
    var login: String = "",
    var password: String = "",
    var fcmtopic: String = ""
)