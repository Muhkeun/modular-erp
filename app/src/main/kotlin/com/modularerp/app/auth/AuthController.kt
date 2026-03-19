package com.modularerp.app.auth

import com.modularerp.core.exception.UnauthorizedException
import com.modularerp.security.domain.User
import com.modularerp.security.jwt.JwtProvider
import com.modularerp.security.repository.UserRepository
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

data class LoginRequest(
    @field:NotBlank val tenantId: String,
    @field:NotBlank val loginId: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
    val tenantId: String,
    val roles: List<String>,
    val locale: String,
    val ssoEnabled: Boolean = false,
    val ssoProviders: List<String> = emptyList()
)

data class RegisterRequest(
    @field:NotBlank val tenantId: String,
    @field:NotBlank val loginId: String,
    @field:NotBlank val password: String,
    @field:NotBlank val name: String,
    val email: String? = null,
    val locale: String = "ko"
)

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ApiResponse<LoginResponse> {
        val user = userRepository.findByTenantIdAndLoginId(req.tenantId, req.loginId)
            .orElseThrow { UnauthorizedException("Invalid credentials") }

        if (!passwordEncoder.matches(req.password, user.password)) {
            throw UnauthorizedException("Invalid credentials")
        }

        val token = jwtProvider.generateToken(
            userId = user.loginId,
            tenantId = user.tenantId,
            roles = user.roles.toList(),
            locale = user.locale
        )

        return ApiResponse.ok(LoginResponse(
            token = token,
            userId = user.loginId,
            name = user.name,
            tenantId = user.tenantId,
            roles = user.roles.toList(),
            locale = user.locale
        ))
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ApiResponse<LoginResponse> {
        val user = User(
            loginId = req.loginId,
            password = passwordEncoder.encode(req.password),
            name = req.name,
            email = req.email,
            locale = req.locale
        ).apply { assignTenant(req.tenantId) }

        val saved = userRepository.save(user)

        val token = jwtProvider.generateToken(
            userId = saved.loginId,
            tenantId = saved.tenantId,
            roles = saved.roles.toList(),
            locale = saved.locale
        )

        return ApiResponse.ok(LoginResponse(
            token = token,
            userId = saved.loginId,
            name = saved.name,
            tenantId = saved.tenantId,
            roles = saved.roles.toList(),
            locale = saved.locale
        ))
    }
}
