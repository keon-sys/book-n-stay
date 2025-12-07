package org.keon.book.application.service

import org.keon.book.application.port.inbound.UserUseCase
import org.keon.book.application.port.outbound.KakaoTokenRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.stereotype.Service

@Service
class ViewService(
    private val kakaoUserReadRepository: KakaoUserReadRepository,
    private val kakaoTokenRepository: KakaoTokenRepository,
) : UserUseCase {
    override fun invoke(query: UserUseCase.Query): UserUseCase.Response {
        val token = kakaoTokenRepository.findByUserId(KakaoTokenRepository.FindRequest(query.userId))
        val nickname = if (token != null) {
            kakaoUserReadRepository(KakaoUserReadRepository.Request(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
            )).nickname
        } else {
            null
        }
        return UserUseCase.Response(nickname ?: "unknown")
    }
}