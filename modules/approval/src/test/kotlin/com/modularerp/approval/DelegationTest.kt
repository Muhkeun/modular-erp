package com.modularerp.approval

import com.modularerp.approval.domain.ApprovalDelegation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DelegationTest {

    @Test
    fun `delegation is effective within date range`() {
        val delegation = ApprovalDelegation(
            fromUserId = "user-a",
            toUserId = "user-b",
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 31)
        ).apply { assignTenant("T001") }

        assertTrue(delegation.isEffective(LocalDate.of(2026, 3, 15)))
        assertTrue(delegation.isEffective(LocalDate.of(2026, 3, 1)))
        assertTrue(delegation.isEffective(LocalDate.of(2026, 3, 31)))
        assertFalse(delegation.isEffective(LocalDate.of(2026, 2, 28)))
        assertFalse(delegation.isEffective(LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun `deactivated delegation is not effective`() {
        val delegation = ApprovalDelegation(
            fromUserId = "user-a",
            toUserId = "user-b",
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 31)
        ).apply { assignTenant("T001") }

        delegation.deactivateDelegation()
        assertFalse(delegation.isEffective(LocalDate.of(2026, 3, 15)))
    }
}
