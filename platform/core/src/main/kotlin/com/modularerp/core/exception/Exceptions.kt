package com.modularerp.core.exception

open class BusinessException(
    val code: String,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class EntityNotFoundException(
    entityName: String,
    id: Any
) : BusinessException(
    code = "NOT_FOUND",
    message = "$entityName not found with id: $id"
)

class DuplicateEntityException(
    entityName: String,
    field: String,
    value: Any
) : BusinessException(
    code = "DUPLICATE",
    message = "$entityName already exists with $field: $value"
)

class UnauthorizedException(
    message: String = "Unauthorized access"
) : BusinessException(code = "UNAUTHORIZED", message = message)

class ForbiddenException(
    message: String = "Access denied"
) : BusinessException(code = "FORBIDDEN", message = message)
