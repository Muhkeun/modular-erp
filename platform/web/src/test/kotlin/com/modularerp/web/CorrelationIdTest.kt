package com.modularerp.web

import com.modularerp.web.filter.CorrelationIdFilter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class CorrelationIdTest {

    private val filter = CorrelationIdFilter()

    @Test
    fun `should generate correlation ID when not provided`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        val correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME)
        assertNotNull(correlationId)
        assertTrue(correlationId!!.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `should use incoming correlation ID`() {
        val request = MockHttpServletRequest()
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "my-trace-123")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertEquals("my-trace-123", response.getHeader(CorrelationIdFilter.HEADER_NAME))
    }
}
