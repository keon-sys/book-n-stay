package org.keon.book.application.port.outbound

import java.time.ZonedDateTime

interface BookingRepository {

    fun fetchBookings()

    data class BookingsRequest(
        val date: ZonedDateTime,
    )

    data class BookingsResult(
        val accountIds: List<String>,
    )
}