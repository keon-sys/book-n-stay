package org.keon.book.application.port.inbound

import org.keon.book.application.type.EpochSecond

interface BookingsReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val date: EpochSecond,
    )

    data class Response(
        val bookings: List<BookingInfo>,
    )

    data class BookingInfo(
        val bookingId: Long?,
        val from: EpochSecond,
        val to: EpochSecond,
        val nickname: String,
    )
}
