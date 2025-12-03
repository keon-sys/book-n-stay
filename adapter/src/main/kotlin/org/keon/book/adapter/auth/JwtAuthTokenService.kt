package org.keon.book.adapter.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.keon.book.adapter.config.Properties
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey
import org.springframework.stereotype.Component

@Component
class JwtAuthTokenService(
    private val authTokenProperty: Properties.AuthTokenProperty,
) {

    private val signingKey: SecretKey by lazy {
        val keyBytes = authTokenProperty.tokenSecret.toByteArray()
        require(keyBytes.size >= 32) { "security.auth.token-secret must be at least 256 bits" }
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun createToken(accountId: String): String {
        val now = Instant.now()
        val expiresAt = now.plus(authTokenProperty.tokenValidity)

        return Jwts.builder()
            .issuer(authTokenProperty.issuer)
            .subject(accountId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact()
    }

    fun parseAccountId(token: String): String =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
}
