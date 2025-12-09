package org.keon.book.application.port.inbound

import org.keon.book.application.type.EpochSecond

interface UserBookingsReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val accountId: String,
    )

    data class Response(
        val bookings: List<BookingInfo>,
    )

    data class BookingInfo(
        val bookingId: Long?,
        val date: EpochSecond,
        val nickname: String,
    )
}
