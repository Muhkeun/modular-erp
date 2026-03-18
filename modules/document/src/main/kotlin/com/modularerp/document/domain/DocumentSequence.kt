package com.modularerp.document.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "document_sequences",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "document_type", "period"])]
)
class DocumentSequence(
    @Column(nullable = false, length = 20)
    val documentType: String,

    @Column(nullable = false, length = 10)
    val prefix: String,

    @Column(nullable = false, length = 10)
    val period: String,

    @Column(nullable = false)
    var currentSeq: Long = 0,

    @Column(nullable = false)
    val padLength: Int = 5

) : TenantEntity() {

    fun next(): String {
        currentSeq++
        val seqStr = currentSeq.toString().padStart(padLength, '0')
        return "$prefix-$period-$seqStr"
    }
}
