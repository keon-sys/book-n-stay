package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.KakaoUserReadUseCase
import org.keon.book.application.port.inbound.MyBookingsReadUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class UserController(
    private val kakaoUserReadUseCase: KakaoUserReadUseCase,
    private val myBookingsReadUseCase: MyBookingsReadUseCase,
) {

    companion object {
        private const val HEADER_ACCOUNT_ID = "X-Kakao-Account-Id"
    }

    @GetMapping("/api/v1/user/me")
    fun getCurrentUser(
        @RequestHeader(HEADER_ACCOUNT_ID) accountId: String,
    ): KakaoUserReadUseCase.Response =
        kakaoUserReadUseCase(KakaoUserReadUseCase.Query(accountId))

    @GetMapping("/api/v1/user/me/bookings")
    fun getCurrentUserBookings(
        @RequestHeader(HEADER_ACCOUNT_ID) accountId: String,
    ): MyBookingsReadUseCase.Response =
        myBookingsReadUseCase(MyBookingsReadUseCase.Query(accountId = accountId))
}