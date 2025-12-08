package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingsReadUseCase
import org.keon.book.application.port.inbound.MyBookingsReadUseCase
import org.keon.book.application.type.EpochSecond
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class BookingController(
    private val bookingsReadUseCase: BookingsReadUseCase,
    private val myBookingsReadUseCase: MyBookingsReadUseCase,
    private val bookingCreateUseCase: BookingCreateUseCase,
    private val bookingDeleteUseCase: BookingDeleteUseCase,
) {

    @GetMapping("/api/v1/bookings")
    fun getBookings(
        @RequestParam("date") date: EpochSecond,
    ): BookingsReadUseCase.Response =
        bookingsReadUseCase(BookingsReadUseCase.Query(date = date))

    @GetMapping("/api/v1/users/me/bookings")
    fun getCurrentUserBookings(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
    ): MyBookingsReadUseCase.Response =
        myBookingsReadUseCase(MyBookingsReadUseCase.Query(accountId = accountId))

    @PostMapping("/api/v1/bookings")
    fun setBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @RequestBody request: BookingCreateDto,
    ): ResponseEntity<BookingCreateUseCase.Response> {
        val response = bookingCreateUseCase(BookingCreateUseCase.Command(
            from = request.from,
            to = request.to,
            accountId = accountId,
            nickname = request.nickname,
        ))

        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.bookingId)
            .toUri()

        return ResponseEntity.created(location).body(response)
    }

    @DeleteMapping("/api/v1/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeBooking(
        @RequestHeader("X-Kakao-Account-Id") accountId: String,
        @PathVariable id: Long,
    ) {
        bookingDeleteUseCase(BookingDeleteUseCase.Command(
            bookingId = id,
            accountId = accountId
        ))
    }

    data class BookingCreateDto(
        val from: EpochSecond,
        val to: EpochSecond,
        val nickname: String,
    )
}
