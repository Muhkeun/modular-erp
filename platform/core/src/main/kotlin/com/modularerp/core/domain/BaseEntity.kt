package com.modularerp.core.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 기본 엔티티 — 모든 도메인 엔티티의 공통 상위 클래스.
 *
 * 제공 기능:
 * - 자동 채번 ID (GenerationType.IDENTITY)
 * - 감사 추적: 생성일시/수정일시, 생성자/수정자 (Spring Data Auditing)
 * - 논리 삭제(soft delete): active 플래그로 활성/비활성 관리
 * - equals/hashCode: ID 기반 동등성 비교
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    @CreatedBy
    @Column(updatable = false)
    var createdBy: String? = null
        protected set

    @LastModifiedBy
    var updatedBy: String? = null
        protected set

    @Column(nullable = false)
    var active: Boolean = true
        protected set

    fun deactivate() {
        this.active = false
    }

    fun activate() {
        this.active = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEntity) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
