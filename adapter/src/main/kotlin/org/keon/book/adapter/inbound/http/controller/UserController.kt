package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.UserBookingsReadUseCase
import org.keon.book.application.port.inbound.UserReadUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class UserController(
    private val userReadUseCase: UserReadUseCase,
    private val userBookingsReadUseCase: UserBookingsReadUseCase,
) {

    companion object {
        private const val HEADER_ACCOUNT_ID = "X-Kakao-Account-Id"
    }

    @GetMapping("/api/v1/user/me")
    fun getCurrentUser(
        @RequestHeader(HEADER_ACCOUNT_ID) accountId: String,
    ): UserReadUseCase.Response =
        userReadUseCase(UserReadUseCase.Query(accountId = accountId))

    @GetMapping("/api/v1/user/me/bookings")
    fun getCurrentUserBookings(
        @RequestHeader(HEADER_ACCOUNT_ID) accountId: String,
    ): UserBookingsReadUseCase.Response =
        userBookingsReadUseCase(UserBookingsReadUseCase.Query(accountId = accountId))
}