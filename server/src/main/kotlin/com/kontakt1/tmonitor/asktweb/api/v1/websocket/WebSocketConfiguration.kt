package com.kontakt1.tmonitor.asktweb.api.v1.websocket

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * Задает бин в кажестве обработчика одключений по вебсокету.
 */
@Configuration
@EnableWebSocket
class WebSocketConfiguration : WebSocketConfigurer {
    @Autowired
    lateinit var websocketHandler : WebsocketHandler

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(websocketHandler, "/api/v1/mnemoscheme/websocket")
                .setAllowedOrigins("*") //        .withSockJS()
    }
}