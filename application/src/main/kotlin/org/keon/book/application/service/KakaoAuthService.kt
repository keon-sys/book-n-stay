package org.keon.book.application.service

import org.keon.book.application.port.inbound.KakaoAccessTokenReadUseCase
import org.keon.book.application.port.inbound.KakaoSessionCreateUseCase
import org.keon.book.application.port.outbound.KakaoTokenCacheSaveRepository
import org.keon.book.application.port.outbound.KakaoTokenReadRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.stereotype.Service

@Service
class KakaoAuthService(
    private val kakaoUserReadRepository: KakaoUserReadRepository,
    private val kakaoTokenReadRepository: KakaoTokenReadRepository,
    private val kakaoTokenCacheSaveRepository: KakaoTokenCacheSaveRepository,
) : KakaoSessionCreateUseCase, KakaoAccessTokenReadUseCase {

    override fun invoke(command: KakaoSessionCreateUseCase.Command): KakaoSessionCreateUseCase.Response {
        val user = kakaoUserReadRepository(KakaoUserReadRepository.Request(
            accessToken = command.accessToken,
            refreshToken = null,
        ))
        kakaoTokenCacheSaveRepository(KakaoTokenCacheSaveRepository.Request(
            accountId = user.accountId,
            accessToken = command.accessToken,
            refreshToken = null,
            expiresIn = null,
            refreshTokenExpiresIn = null,
        ))
        return KakaoSessionCreateUseCase.Response(
            accountId = user.accountId,
            nickname = user.nickname,
        )
    }

    override fun invoke(query: KakaoAccessTokenReadUseCase.Query): KakaoAccessTokenReadUseCase.Response {
        val token = kakaoTokenReadRepository(KakaoTokenReadRepository.Request(
            authorizationCode = query.code,
            redirectUri = query.redirectUri,
        ))
        return KakaoAccessTokenReadUseCase.Response(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
        )
    }
}
