package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.KakaoUserReadUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class KakaoUserController(
    private val kakaoUserReadUseCase: KakaoUserReadUseCase,
) {

    @GetMapping("/api/v1/user/kakao/me")
    fun getKakaoUser(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
    ): KakaoUserResponse {
        val response = kakaoUserReadUseCase(KakaoUserReadUseCase.Query(accountId))
        return KakaoUserResponse(
            accountId = response.accountId,
            nickname = response.nickname,
        )
    }

    data class KakaoUserResponse(
        val accountId: String,
        val nickname: String?,
    )
}