package org.keon.book.application.service

import org.keon.book.application.port.inbound.KakaoAccessTokenReadUseCase
import org.keon.book.application.port.inbound.KakaoSessionCreateUseCase
import org.keon.book.application.port.outbound.KakaoAuthTokenReadRepository
import org.keon.book.application.port.outbound.KakaoTokenRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoUserReadRepository: KakaoUserReadRepository,
    private val kakaoAuthTokenReadRepository: KakaoAuthTokenReadRepository,
    private val kakaoTokenRepository: KakaoTokenRepository,
) : KakaoSessionCreateUseCase, KakaoAccessTokenReadUseCase {

    override fun invoke(command: KakaoSessionCreateUseCase.Command): KakaoSessionCreateUseCase.Response {
        val user = kakaoUserReadRepository(KakaoUserReadRepository.Request(
            accessToken = command.accessToken,
            refreshToken = null,
        ))
        kakaoTokenRepository.save(KakaoTokenRepository.SaveRequest(
            userId = user.id,
            accessToken = command.accessToken,
            refreshToken = null,
            expiresIn = null,
            refreshTokenExpiresIn = null,
        ))
        return KakaoSessionCreateUseCase.Response(
            id = user.id,
            nickname = user.nickname,
        )
    }

    override fun invoke(query: KakaoAccessTokenReadUseCase.Query): KakaoAccessTokenReadUseCase.Response {
        val token = kakaoAuthTokenReadRepository(KakaoAuthTokenReadRepository.Request(
            authorizationCode = query.code,
            redirectUri = query.redirectUri,
        ))
        return KakaoAccessTokenReadUseCase.Response(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
        )
    }
}
