package org.keon.book.application.service

import org.keon.book.application.port.inbound.NicknameUseCase
import org.keon.book.application.port.outbound.KakaoUserRepository
import org.keon.book.application.port.outbound.dto.KakaoAccessToken
import org.springframework.stereotype.Service

@Service
class ViewService(
    private val kakaoUserRepository: KakaoUserRepository,
) : NicknameUseCase {
    override fun getNickname(query: NicknameUseCase.NicknameQuery): NicknameUseCase.NicknameResult {
        val nickname = query.accessToken
            .let(::KakaoAccessToken)
            .run(kakaoUserRepository::fetchUser)
            .nickname
        return NicknameUseCase.NicknameResult(nickname ?: "unknown")
    }

}