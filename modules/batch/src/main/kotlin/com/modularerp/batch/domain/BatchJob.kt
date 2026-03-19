package com.modularerp.batch.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "batch_jobs")
class BatchJob(

    @Column(nullable = false, length = 50)
    var jobCode: String,

    @Column(nullable = false, length = 200)
    var jobName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var jobType: BatchJobType,

    @Column(length = 50)
    var cronExpression: String? = null,

    var enabled: Boolean = true,

    var lastRunAt: LocalDateTime? = null,

    var nextRunAt: LocalDateTime? = null,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity()

enum class BatchJobType {
    GL_POSTING, DEPRECIATION, MRP_RUN, STOCK_REVALUATION,
    EXCHANGE_RATE_UPDATE, DATA_IMPORT, DATA_EXPORT,
    REPORT_GENERATION, EMAIL_SENDING, CLEANUP
}
