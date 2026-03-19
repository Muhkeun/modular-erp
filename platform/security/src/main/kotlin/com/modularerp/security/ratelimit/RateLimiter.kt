package com.modularerp.security.ratelimit

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Sliding window rate limiter using fixed-window approximation.
 * Each bucket tracks request counts per minute window.
 */
class RateLimiter {

    private data class WindowCounter(
        val windowStart: AtomicLong = AtomicLong(0),
        val count: AtomicLong = AtomicLong(0)
    )

    private val buckets = ConcurrentHashMap<String, WindowCounter>()

    /**
     * Try to consume one token for the given key.
     * @return remaining tokens, or -1 if limit exceeded
     */
    fun tryConsume(key: String, maxPerMinute: Int): RateLimitResult {
        val now = System.currentTimeMillis()
        val currentWindow = now / 60_000

        val counter = buckets.computeIfAbsent(key) { WindowCounter() }

        val windowStart = counter.windowStart.get()
        if (windowStart != currentWindow) {
            // New window — reset. Race condition is acceptable (slight over-count at boundary)
            if (counter.windowStart.compareAndSet(windowStart, currentWindow)) {
                counter.count.set(0)
            }
        }

        val currentCount = counter.count.incrementAndGet()
        val remaining = maxPerMinute - currentCount.toInt()

        return if (remaining >= 0) {
            RateLimitResult(allowed = true, remaining = remaining, retryAfterSeconds = 0)
        } else {
            counter.count.decrementAndGet()
            val secondsUntilReset = 60 - ((now % 60_000) / 1000).toInt()
            RateLimitResult(allowed = false, remaining = 0, retryAfterSeconds = secondsUntilReset)
        }
    }

    /**
     * Periodically clean up stale entries (windows older than 2 minutes).
     */
    fun cleanup() {
        val currentWindow = System.currentTimeMillis() / 60_000
        buckets.entries.removeIf { (_, v) -> currentWindow - v.windowStart.get() > 2 }
    }

    data class RateLimitResult(
        val allowed: Boolean,
        val remaining: Int,
        val retryAfterSeconds: Int
    )
}
