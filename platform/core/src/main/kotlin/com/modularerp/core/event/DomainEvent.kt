package com.modularerp.core.event

import java.time.Instant
import java.util.UUID

abstract class DomainEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: Instant = Instant.now(),
    val tenantId: String
)

interface DomainEventPublisher {
    fun publish(event: DomainEvent)
}
