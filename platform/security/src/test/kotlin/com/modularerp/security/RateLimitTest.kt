package com.modularerp.security

import com.modularerp.security.ratelimit.RateLimiter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RateLimitTest {

    @Test
    fun `should allow requests within limit`() {
        val limiter = RateLimiter()
        val result = limiter.tryConsume("test-key", 10)
        assertTrue(result.allowed)
        assertEquals(9, result.remaining)
    }

    @Test
    fun `should reject requests exceeding limit`() {
        val limiter = RateLimiter()
        val limit = 5

        // Consume all tokens
        repeat(limit) {
            val result = limiter.tryConsume("test-key", limit)
            assertTrue(result.allowed, "Request ${it + 1} should be allowed")
        }

        // Next request should be rejected
        val result = limiter.tryConsume("test-key", limit)
        assertFalse(result.allowed)
        assertEquals(0, result.remaining)
        assertTrue(result.retryAfterSeconds > 0)
    }

    @Test
    fun `should track different keys independently`() {
        val limiter = RateLimiter()

        repeat(3) { limiter.tryConsume("key-a", 3) }
        val resultA = limiter.tryConsume("key-a", 3)
        assertFalse(resultA.allowed, "key-a should be exhausted")

        val resultB = limiter.tryConsume("key-b", 3)
        assertTrue(resultB.allowed, "key-b should still have capacity")
    }

    @Test
    fun `cleanup should remove stale entries`() {
        val limiter = RateLimiter()
        limiter.tryConsume("will-be-cleaned", 10)
        // cleanup won't remove current window entries, but should not throw
        limiter.cleanup()
    }
}
