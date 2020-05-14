package com.kontakt1.tmonitor.asktweb.api.v1.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * Обработчик подключений по вебсокету.
 */
@Component
class WebsocketHandler : TextWebSocketHandler(), WebSocketHandler {
    val sockets = mutableListOf<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        super.afterConnectionEstablished(session)
        sockets.add(session)
        println("Socket Connected: $session")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Получено сообщение $message")
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        println("Socket Closed: [${closeStatus.code}] ${closeStatus.reason}")
        sockets.remove(session)
        super.afterConnectionClosed(session, closeStatus)
    }
}


