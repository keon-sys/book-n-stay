package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingsReadUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime

@RestController
class BookingController(
    private val bookingsReadUseCase: BookingsReadUseCase,
    private val bookingCreateUseCase: BookingCreateUseCase,
    private val bookingDeleteUseCase: BookingDeleteUseCase,
) {

    @GetMapping("/api/v1/booking")
    fun getBookings(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: ZonedDateTime,
    ): BookingsReadUseCase.Response =
        bookingsReadUseCase(BookingsReadUseCase.Query(date = date))

    @PostMapping("/api/v1/booking")
    fun setBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingCreateDto,
    ): BookingCreateUseCase.Response =
        bookingCreateUseCase(BookingCreateUseCase.Command(
            from = request.from,
            to = request.to,
            accountId = accountId
        ))

    @DeleteMapping("/api/v1/booking")
    fun removeBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingDeleteDto,
    ): BookingDeleteUseCase.Response =
        bookingDeleteUseCase(BookingDeleteUseCase.Command(
            bookingId = request.bookingId,
            accountId = accountId
        ))

    data class BookingCreateDto(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val from: ZonedDateTime,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val to: ZonedDateTime,
    )

    data class BookingDeleteDto(
        val bookingId: Long,
    )
}
