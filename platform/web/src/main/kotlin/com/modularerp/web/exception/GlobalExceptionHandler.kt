package com.modularerp.web.exception

import com.modularerp.core.exception.BusinessException
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.core.exception.UnauthorizedException
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.ErrorDetail
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(e: EntityNotFoundException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.code, e.message))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(e: UnauthorizedException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(e.code, e.message))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(e: ForbiddenException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.code, e.message))

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.code, e.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val details = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse(success = false, error = ErrorDetail("VALIDATION_ERROR", "Validation failed", details)))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unexpected error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}
