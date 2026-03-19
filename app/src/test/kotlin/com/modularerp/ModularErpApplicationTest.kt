package com.modularerp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [com.modularerp.app.ModularErpApplication::class])
@ActiveProfiles("test")
class ModularErpApplicationTest {

    @Test
    fun contextLoads() {
        // Verifies the Spring application context starts successfully
    }
}
