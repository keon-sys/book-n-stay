package org.keon.book.adapter.inbound.web.controller

import org.keon.book.application.port.inbound.BookingUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BookingController(
    private val bookingUseCase: BookingUseCase,
) {

    @GetMapping
    fun getBookings(): BookingUseCase.BookingsResponse {
        TODO()
    }
}