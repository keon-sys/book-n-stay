package org.keon.book.application.service

import org.keon.book.application.port.inbound.KakaoUserReadUseCase
import org.keon.book.application.port.outbound.KakaoSessionReadRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.stereotype.Service

@Service
class KakaoUserReadService(
    private val kakaoSessionReadRepository: KakaoSessionReadRepository,
    private val kakaoUserReadRepository: KakaoUserReadRepository,
) : KakaoUserReadUseCase {

    override fun invoke(query: KakaoUserReadUseCase.Query): KakaoUserReadUseCase.Response {
        val token = kakaoSessionReadRepository(
            KakaoSessionReadRepository.Request(accountId = query.accountId,)
        )

        val user = kakaoUserReadRepository(
            KakaoUserReadRepository.Request(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
            )
        )

        return KakaoUserReadUseCase.Response(
            accountId = user.accountId,
            nickname = user.nickname,
        )
    }
}
