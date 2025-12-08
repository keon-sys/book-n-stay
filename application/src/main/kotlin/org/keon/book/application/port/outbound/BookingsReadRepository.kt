package org.keon.book.application.port.outbound

import java.time.ZonedDateTime

interface BookingsReadRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val date: ZonedDateTime,
    )

    data class Result(
        val bookings: List<BookingData>,
    )

    data class BookingData(
        val id: Long?,
        val from: ZonedDateTime,
        val to: ZonedDateTime,
        val accountId: String,
    )
}
