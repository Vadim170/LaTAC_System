package com.kontakt1.tmonitor.asyncTasks

import com.kontakt1.tmonitor.settings.Settings
import java.sql.*

/**
 * Класс устонавливает подключение к базе данный системы АСКТ-01.
 * @author Makarov V.G.
 */
class Connect {
    companion object {
        suspend fun connectMySQL(settings: Settings, numberConnectAttempts: Int = 9): Connection? {
            //Class.forName("com.mysql.cj.jdbc.Driver") // Для mysql-connector-java-8.0.18.jar.  Не работает на старых версиях android
            Class.forName("com.mysql.jdbc.Driver")
            DriverManager.setLoginTimeout(7) // .. секунд ожидаем
            return getConnection(settings, numberConnectAttempts)
        }

        private suspend fun getConnection(settings: Settings, maxReconnects: Int): Connection? {
            val url = with(settings) {
                "jdbc:mysql://$address:$port/$databaseName" +
                        "?autoReconnect=true" +
                        "&verifyServerCertificate=false" +
                        "&useSSL=true&maxReconnects=$maxReconnects" +
                        "&useLegacyDatetimeCode=true"/* +
                    "&serverTimezone=UTC"*/
                // useSSL=false&verifyServerCertificate=false&useLegacyDatetimeCode=false&serverTimezone=UTC
                // ${if(databaseName != "") "/$databaseName" else ""}
            }
            return DriverManager.getConnection(url, settings.login, settings.password)
        }
    }
    interface EventListenerForInterface {
        fun onPostExecuteConnect(isSuccess : Boolean)
        fun onPreExecuteConnect()
    }
}
