package com.modularerp.admin.dto

import com.modularerp.admin.domain.*
import jakarta.validation.constraints.NotBlank
import java.time.Instant

// ── FieldPermission DTOs ──

data class FieldPermissionRequest(
    @field:NotBlank val fieldName: String,
    val accessLevel: FieldAccessLevel
)

data class FieldPermissionBatchRequest(
    @field:NotBlank val roleCode: String,
    @field:NotBlank val resource: String,
    val fields: List<FieldPermissionRequest>
)

data class FieldPermissionResponse(
    val id: Long,
    val roleCode: String,
    val resource: String,
    val fieldName: String,
    val accessLevel: FieldAccessLevel
) {
    companion object {
        fun from(e: FieldPermission) = FieldPermissionResponse(e.id, e.roleCode, e.resource, e.fieldName, e.accessLevel)
    }
}

// ── DataScope DTOs ──

data class DataScopeRequest(
    val scopeType: DataScopeType,
    val scopeValues: String? = null
)

data class DataScopeBatchRequest(
    @field:NotBlank val roleCode: String,
    @field:NotBlank val resource: String,
    val scopes: List<DataScopeRequest>
)

data class DataScopeResponse(
    val id: Long,
    val roleCode: String,
    val resource: String,
    val scopeType: DataScopeType,
    val scopeValues: String?,
    val valueList: List<String>
) {
    companion object {
        fun from(e: DataScope) = DataScopeResponse(e.id, e.roleCode, e.resource, e.scopeType, e.scopeValues, e.getValueList())
    }
}

// ── ApiKey DTOs ──

data class CreateApiKeyRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val allowedResources: String? = null,
    val rateLimit: Int? = null,
    val expiresAt: Instant? = null
)

data class UpdateApiKeyRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val allowedResources: String? = null,
    val rateLimit: Int? = null
)

data class ApiKeyResponse(
    val id: Long,
    val name: String,
    val keyPrefix: String,
    val description: String?,
    val allowedResources: String?,
    val rateLimit: Int?,
    val status: ApiKeyStatus,
    val expiresAt: Instant?,
    val lastUsedAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(e: ApiKey) = ApiKeyResponse(
            e.id, e.name, e.keyPrefix, e.description, e.allowedResources,
            e.rateLimit, e.status, e.expiresAt, e.lastUsedAt, e.createdAt
        )
    }
}

data class ApiKeyCreateResponse(
    val id: Long,
    val name: String,
    val rawKey: String,
    val keyPrefix: String,
    val expiresAt: Instant?
)

// ── Tenant DTOs ──

data class CreateTenantRequest(
    @field:NotBlank val tenantId: String,
    @field:NotBlank val name: String,
    val description: String? = null,
    val plan: TenantPlan = TenantPlan.FREE,
    val maxUsers: Int = 10,
    val maxStorageMb: Long = 1024
)

data class UpdateTenantRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val maxUsers: Int = 10,
    val maxStorageMb: Long = 1024
)

data class TenantResponse(
    val id: Long,
    val tenantId: String,
    val name: String,
    val description: String?,
    val plan: TenantPlan,
    val maxUsers: Int,
    val maxStorageMb: Long,
    val status: TenantStatus,
    val currentUsers: Long,
    val activatedAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(e: Tenant, userCount: Long) = TenantResponse(
            e.id, e.tenantId, e.name, e.description, e.plan,
            e.maxUsers, e.maxStorageMb, e.status, userCount,
            e.activatedAt, e.createdAt
        )
    }
}
