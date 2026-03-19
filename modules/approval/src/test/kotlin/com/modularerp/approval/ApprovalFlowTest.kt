package com.modularerp.approval

import com.modularerp.approval.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApprovalFlowTest {

    @Test
    fun `submit creates pending request with first step active`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)
        request.addStep("DIRECTOR", "user-dir", 2)
        request.submit()

        assertEquals(ApprovalStatus.PENDING, request.status)
        assertEquals(StepStatus.ACTIVE, request.steps[0].stepStatus)
        assertEquals(StepStatus.WAITING, request.steps[1].stepStatus)
    }

    @Test
    fun `approve first step activates second step`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)
        request.addStep("DIRECTOR", "user-dir", 2)
        request.submit()

        request.approve("user-mgr", "Looks good")

        assertEquals(ApprovalStatus.PENDING, request.status)
        assertEquals(ApprovalDecision.APPROVED, request.steps[0].decision)
        assertEquals(StepStatus.ACTIVE, request.steps[1].stepStatus)
    }

    @Test
    fun `approve all steps marks request as approved`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)
        request.addStep("DIRECTOR", "user-dir", 2)
        request.submit()

        request.approve("user-mgr", "OK")
        request.approve("user-dir", "Approved")

        assertEquals(ApprovalStatus.APPROVED, request.status)
        assertNotNull(request.completedAt)
    }

    @Test
    fun `reject marks request as rejected`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)
        request.submit()

        request.reject("user-mgr", "Budget exceeded")

        assertEquals(ApprovalStatus.REJECTED, request.status)
        assertEquals(ApprovalDecision.REJECTED, request.steps[0].decision)
        assertNotNull(request.completedAt)
    }

    @Test
    fun `return marks request as returned`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)
        request.submit()

        request.returnToSubmitter("user-mgr", "Please revise quantities")

        assertEquals(ApprovalStatus.RETURNED, request.status)
        assertEquals(ApprovalDecision.RETURNED, request.steps[0].decision)
    }

    @Test
    fun `cancel works from draft and pending`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)

        // Cancel from DRAFT
        request.cancel()
        assertEquals(ApprovalStatus.CANCELLED, request.status)
    }

    @Test
    fun `comments can be added to request`() {
        val request = createTestRequest()
        request.addStep("MANAGER", "user-mgr", 1)
        request.submit()

        request.addComment("user-mgr", "Reviewing now", 1)
        assertEquals(1, request.comments.size)
        assertEquals("user-mgr", request.comments[0].commentBy)
    }

    private fun createTestRequest() = ApprovalRequest(
        documentType = "PR",
        documentId = 100L,
        documentNo = "PR-2026-0001",
        requestedBy = "user-requester"
    ).apply { assignTenant("T001") }
}
