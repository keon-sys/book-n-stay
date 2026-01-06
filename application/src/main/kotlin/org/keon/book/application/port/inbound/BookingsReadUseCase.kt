package org.keon.book.application.port.inbound

import org.keon.book.application.type.EpochSecond

interface BookingsReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val year: Int,
        val month: Int,
        val accountId: String,
    )

    data class Response(
        val bookings: List<BookingInfo>,
    )

    data class BookingInfo(
        val date: EpochSecond,
        val nickname: String,
    )
}
