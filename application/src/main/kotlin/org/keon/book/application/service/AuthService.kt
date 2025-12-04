package org.keon.book.application.service

import org.keon.book.application.port.inbound.AuthUseCase
import org.keon.book.application.port.outbound.KakaoUserRepository
import org.keon.book.application.port.outbound.dto.KakaoAccessToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoUserRepository: KakaoUserRepository,
) : AuthUseCase {

    override fun createSession(accessToken: String): AuthUseCase.AuthResult {
        val token = KakaoAccessToken(accessToken)
        val user = kakaoUserRepository.fetchUser(token)
        return AuthUseCase.AuthResult(user = user)
    }

    override fun exchangeAuthorizationCode(code: String, redirectUri: String): KakaoAccessToken =
        kakaoUserRepository.exchangeCodeForToken(code, redirectUri)
}
