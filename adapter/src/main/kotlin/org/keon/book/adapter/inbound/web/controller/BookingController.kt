package org.keon.book.adapter.inbound.web.controller

import org.keon.book.application.port.inbound.BookingUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime

@RestController
class BookingController(
    private val bookingUseCase: BookingUseCase,
) {

    @GetMapping("/api/booking")
    fun getBookings(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: ZonedDateTime,
    ): BookingUseCase.BookingsResponse =
        bookingUseCase.getBookings(BookingUseCase.BookingsQuery(date = date))

    @PostMapping("/api/booking")
    fun setBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingDto,
    ) {
        bookingUseCase.setBooking(
            BookingUseCase.BookingCommand(date = request.date, accountId = accountId),
        )
    }

    @DeleteMapping
    fun removeBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingDto,
    ) {
        bookingUseCase.removeBooking(
            BookingUseCase.BookingCommand(date = request.date, accountId = accountId),
        )
    }

    data class BookingDto(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val date: ZonedDateTime,
    )
}
