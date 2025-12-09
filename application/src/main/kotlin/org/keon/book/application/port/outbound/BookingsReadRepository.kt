package org.keon.book.application.port.outbound

import org.keon.book.application.type.EpochSecond

interface BookingsReadRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val year: Int,
        val month: Int,
    )

    data class Result(
        val bookings: List<BookingData>,
    )

    data class BookingData(
        val id: Long?,
        val date: EpochSecond,
        val accountId: String,
        val nickname: String,
    )
}
