package org.keon.book.application.service

import org.keon.book.application.port.inbound.AuthUseCase
import org.keon.book.application.port.outbound.KakaoUserClient
import org.keon.book.application.port.outbound.dto.KakaoAccessToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoUserClient: KakaoUserClient,
) : AuthUseCase {

    override fun createSession(accessToken: String): AuthUseCase.AuthResult {
        val token = KakaoAccessToken(accessToken)
        val user = kakaoUserClient.fetchUser(token)
        return AuthUseCase.AuthResult(user = user)
    }

    override fun exchangeAuthorizationCode(code: String, redirectUri: String): KakaoAccessToken =
        kakaoUserClient.exchangeCodeForToken(code, redirectUri)
}
