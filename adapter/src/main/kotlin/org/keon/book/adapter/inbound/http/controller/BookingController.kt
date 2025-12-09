package org.keon.book.adapter.inbound.http.controller

import org.keon.book.application.exception.BookingCapacityExceededException
import org.keon.book.application.exception.DuplicateUserBookingException
import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingsReadUseCase
import org.keon.book.application.type.EpochSecond
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class BookingController(
    private val bookingsReadUseCase: BookingsReadUseCase,
    private val bookingCreateUseCase: BookingCreateUseCase,
    private val bookingDeleteUseCase: BookingDeleteUseCase,
) {

    companion object {
        private const val HEADER_ACCOUNT_ID = "X-Kakao-Account-Id"
    }

    @GetMapping("/api/v1/bookings")
    fun getBookings(
        @RequestParam("year") year: Int,
        @RequestParam("month") month: Int,
    ): BookingsReadUseCase.Response =
        bookingsReadUseCase(BookingsReadUseCase.Query(year = year, month = month))

    @PostMapping("/api/v1/booking")
    fun setBooking(
        @RequestHeader(HEADER_ACCOUNT_ID) accountId: String,
        @RequestBody request: BookingCreateDto,
    ): ResponseEntity<BookingCreateUseCase.Response> {
        val response = bookingCreateUseCase(
            BookingCreateUseCase.Command(
                from = request.from,
                to = request.to,
                accountId = accountId,
            )
        )

        val firstBookingId = response.bookings.firstOrNull()?.bookingId
        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(firstBookingId)
            .toUri()

        return ResponseEntity.created(location).body(response)
    }

    @DeleteMapping("/api/v1/booking/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeBooking(
        @RequestHeader(HEADER_ACCOUNT_ID) accountId: String,
        @PathVariable id: Long,
    ) {
        bookingDeleteUseCase(
            BookingDeleteUseCase.Command(
                bookingId = id,
                accountId = accountId
            )
        )
    }

    data class BookingCreateDto(
        val from: EpochSecond,
        val to: EpochSecond,
    )

    @ExceptionHandler(DuplicateUserBookingException::class)
    fun handleDuplicateUserBookingException(e: DuplicateUserBookingException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(
                message = e.message ?: "이미 해당 날짜에 예약이 존재합니다.",
            ))
    }

    @ExceptionHandler(BookingCapacityExceededException::class)
    fun handleBookingCapacityExceededException(e: BookingCapacityExceededException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(
                message = e.message ?: "해당 날짜의 예약 인원이 초과되었습니다.",
            ))
    }

    data class ErrorResponse(
        val message: String,
    )
}
