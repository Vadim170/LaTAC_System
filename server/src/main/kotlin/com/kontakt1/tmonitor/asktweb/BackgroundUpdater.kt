package com.kontakt1.tmonitor.asktweb

import com.kontakt1.tmonitor.asktweb.api.v1.websocket.WebsocketHandler
import com.kontakt1.tmonitor.systems.System
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import javax.sql.DataSource
import kotlin.random.Random

/**
 * Выполняет фоновое обновление, рыссылает по вебсокетам сообщения для изменеия отображаемой информации.
 */
@Component
class BackgroundUpdater {
    @Autowired
    lateinit var websocketHandler : WebsocketHandler
    @Autowired
    lateinit var dataSource: DataSource
    @Autowired
    lateinit var system: System

    var serviceIsRunning = true

    fun sendBroadcastMessage(message : String){
        websocketHandler.sockets.forEach {
            it.sendMessage(TextMessage(message))
        }
    }

    fun launchBackgroundRefresh() {
        launchWebsocketRandomizer()
        GlobalScope.launch {
            while (serviceIsRunning && system.silabus.listSilo.isNotEmpty()) {
                val connection = dataSource.connection
                val isNeedNotification = connection.let { system.silabus.readAllStates(it) }
                onPostExecuteReadAllStates(isNeedNotification)
                delay(300_000) // Каждые 5 минут
            }
        }
    }

    private fun launchWebsocketRandomizer() {
        GlobalScope.launch {
            while (serviceIsRunning) {
                //val groupId = "0"
                val paramType = "level"
                //val rezId = "1"
                val indication = getRandLevelIndication()
                val listOfUpdates = listOf<String>(
                        getJsonIndicatorUpdate(paramType, 0, indication = indication),
                        getJsonIndicatorUpdate(paramType, 2, indication = getRandLevelIndication()),
                        getJsonIndicatorUpdate(paramType, 1, "001_01", getRandLevelIndication()),
                        getJsonIndicatorUpdate(paramType, 1, "001_02", getRandLevelIndication())
                )
                val message = listOfUpdates.toString()
                sendBroadcastMessage(message)
                delay(1_000) // Каждые 5 минут
            }
        }
    }

    private fun getRandLevelIndication() = "{\"level\": ${Random.nextFloat()}}"

    private fun getJsonIndicatorUpdate(paramType: String, groupId: Int, paramIdentifer: String? = null, indication: String): String {
        val identificatorOfParam = if(paramIdentifer.isNullOrEmpty()) "" else "\"paramIdentifer\": \"$paramIdentifer\","
        return "{" +
                "\"type\": \"$paramType\"," +
                "\"groupId\": \"$groupId\"," +
                identificatorOfParam +
                "\"indication\": $indication" +
                "}"
    }

    private fun onPostExecuteReadAllStates(isNeedNotification: Boolean) {
        // TODO Send notifications to all users if it need
    }
}