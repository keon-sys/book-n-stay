package org.keon.book.application.port.inbound

import org.keon.book.application.port.outbound.dto.KakaoAccessToken
import org.keon.book.application.port.outbound.dto.KakaoUser

interface AuthUseCase {
    fun createSession(accessToken: String): AuthResult
    fun exchangeAuthorizationCode(code: String, redirectUri: String): KakaoAccessToken

    data class AuthResult(
        val user: KakaoUser,
    )
}
