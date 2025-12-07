package org.keon.book.application.service

import org.keon.book.application.port.inbound.KakaoUserReadUseCase
import org.keon.book.application.port.outbound.KakaoTokenCacheReadRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.stereotype.Service

@Service
class KakaoUserReadService(
    private val kakaoTokenCacheReadRepository: KakaoTokenCacheReadRepository,
    private val kakaoUserReadRepository: KakaoUserReadRepository,
) : KakaoUserReadUseCase {

    override fun invoke(query: KakaoUserReadUseCase.Query): KakaoUserReadUseCase.Response {
        val token = kakaoTokenCacheReadRepository(KakaoTokenCacheReadRepository.Request(
            accountId = query.accountId,
        )) ?: throw IllegalStateException("Token not found for accountId: ${query.accountId}")

        val user = kakaoUserReadRepository(KakaoUserReadRepository.Request(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
        ))

        return KakaoUserReadUseCase.Response(
            accountId = user.accountId,
            nickname = user.nickname,
        )
    }
}
