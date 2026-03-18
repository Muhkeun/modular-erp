package com.modularerp.messaging

import com.modularerp.core.event.DomainEvent
import com.modularerp.core.event.DomainEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class InProcessEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
