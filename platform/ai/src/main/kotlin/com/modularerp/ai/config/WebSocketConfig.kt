package com.modularerp.ai.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(prefix = "modular-erp.ai", name = ["enabled"], havingValue = "true")
class WebSocketConfig(
    private val aiProperties: AiProperties
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        if (aiProperties.enabled) {
            registry.addEndpoint("/ws/ai")
                .setAllowedOriginPatterns("*")
                .withSockJS()
        }
    }
}
