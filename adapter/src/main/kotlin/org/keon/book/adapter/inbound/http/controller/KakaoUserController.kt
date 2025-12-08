package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.KakaoUserReadUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class KakaoUserController(
    private val kakaoUserReadUseCase: KakaoUserReadUseCase,
) {

    @GetMapping("/api/v1/users/me")
    fun getCurrentUser(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
    ): KakaoUserReadUseCase.Response =
        kakaoUserReadUseCase(KakaoUserReadUseCase.Query(accountId))
}