package com.modularerp.web.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val meta: PageMeta? = null
) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data)
        fun <T> ok(data: T, meta: PageMeta) = ApiResponse(success = true, data = data, meta = meta)
        fun <T> error(code: String, message: String) = ApiResponse<T>(
            success = false,
            error = ErrorDetail(code, message)
        )
    }
}

data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
