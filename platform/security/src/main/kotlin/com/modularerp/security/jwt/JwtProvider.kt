package com.modularerp.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${modular-erp.security.jwt.secret}") private val secret: String,
    @Value("\${modular-erp.security.jwt.expiration-ms:3600000}") private val expirationMs: Long
) {
    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun generateToken(userId: String, tenantId: String, roles: List<String>, locale: String = "ko"): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId)
            .claim("tenantId", tenantId)
            .claim("roles", roles)
            .claim("locale", locale)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(key)
            .compact()
    }

    fun validateAndParse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    fun getUserId(claims: Claims): String = claims.subject
    fun getTenantId(claims: Claims): String = claims["tenantId"] as String
    fun getLocale(claims: Claims): String = claims["locale"] as? String ?: "ko"

    @Suppress("UNCHECKED_CAST")
    fun getRoles(claims: Claims): List<String> = claims["roles"] as? List<String> ?: emptyList()
}
