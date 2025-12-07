package org.keon.book.adapter.inbound.web.controller

import org.keon.book.application.port.inbound.UserUseCase
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class NicknameController(
    private val nicknameUseCase: UserUseCase,
) {

    @GetMapping("/api/v1/nickname", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getNickname(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
    ): UserUseCase.Response =
        nicknameUseCase(
            UserUseCase.Query(
                userId = accountId,
            ),
        )
}
