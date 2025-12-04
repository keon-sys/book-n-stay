package org.keon.book.adapter.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.keon.book.adapter.config.Properties
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Date
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.SecretKey
import org.springframework.stereotype.Component

@Component
class JwtAuthTokenService(
    private val authTokenProperty: Properties.AuthTokenProperty,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val signingKey: SecretKey by lazy {
        require(authTokenProperty.tokenSecret.isNotBlank()) {
            "security.auth.token-secret must not be blank"
        }
        val rawBytes = authTokenProperty.tokenSecret.toByteArray(StandardCharsets.UTF_8)
        val keyBytes = if (rawBytes.size < 32) {
            logger.warn("security.auth.token-secret is shorter than 256 bits; deriving a 256-bit key via SHA-256. Use a longer random secret in production.")
            MessageDigest.getInstance("SHA-256").digest(rawBytes)
        } else {
            rawBytes
        }
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
