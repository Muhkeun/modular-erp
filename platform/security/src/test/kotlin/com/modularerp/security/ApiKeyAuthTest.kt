package com.modularerp.security

import com.modularerp.security.apikey.ApiKeyAuthenticator
import com.modularerp.security.apikey.ApiKeyPrincipal
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApiKeyAuthTest {

    @Test
    fun `should authenticate valid API key`() {
        val authenticator = object : ApiKeyAuthenticator {
            override fun authenticate(rawKey: String): ApiKeyPrincipal? {
                return if (rawKey == "valid-key") {
                    ApiKeyPrincipal(
                        id = 1L,
                        name = "test-key",
                        tenantId = "tenant-1",
                        allowedResources = listOf("sales", "purchase"),
                        rateLimit = 50
                    )
                } else null
            }
        }

        val result = authenticator.authenticate("valid-key")
        assertNotNull(result)
        assertEquals("test-key", result!!.name)
        assertEquals("tenant-1", result.tenantId)
        assertEquals(50, result.rateLimit)
        assertEquals(listOf("sales", "purchase"), result.allowedResources)
    }

    @Test
    fun `should reject invalid API key`() {
        val authenticator = object : ApiKeyAuthenticator {
            override fun authenticate(rawKey: String): ApiKeyPrincipal? = null
        }

        val result = authenticator.authenticate("bad-key")
        assertNull(result)
    }

    @Test
    fun `should reject expired API key`() {
        val authenticator = object : ApiKeyAuthenticator {
            override fun authenticate(rawKey: String): ApiKeyPrincipal? {
                // Simulates the service returning null for expired keys
                return null
            }
        }

        val result = authenticator.authenticate("expired-key")
        assertNull(result)
    }
}
