package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingReadUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime

@RestController
class BookingController(
    private val bookingReadUseCase: BookingReadUseCase,
    private val bookingCreateUseCase: BookingCreateUseCase,
    private val bookingDeleteUseCase: BookingDeleteUseCase,
) {

    @GetMapping("/api/v1/booking")
    fun getBookings(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: ZonedDateTime,
    ): BookingReadUseCase.Response =
        bookingReadUseCase(BookingReadUseCase.Query(date = date))

    @PostMapping("/api/v1/booking")
    fun setBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingDto,
    ): BookingCreateUseCase.Response =
        bookingCreateUseCase(BookingCreateUseCase.Command(date = request.date, accountId = accountId))

    @DeleteMapping("/api/v1/booking")
    fun removeBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingDto,
    ): BookingDeleteUseCase.Response =
        bookingDeleteUseCase(BookingDeleteUseCase.Command(date = request.date, accountId = accountId))

    data class BookingDto(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val date: ZonedDateTime,
    )
}
