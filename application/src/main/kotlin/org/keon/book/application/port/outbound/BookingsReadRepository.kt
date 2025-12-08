package org.keon.book.application.port.outbound

import org.keon.book.application.type.EpochSecond

interface BookingsReadRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val date: EpochSecond,
    )

    data class Result(
        val bookings: List<BookingData>,
    )

    data class BookingData(
        val id: Long?,
        val from: EpochSecond,
        val to: EpochSecond,
        val accountId: String,
        val nickname: String,
    )
}
