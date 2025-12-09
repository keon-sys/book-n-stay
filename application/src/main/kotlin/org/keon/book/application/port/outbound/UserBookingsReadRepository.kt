package org.keon.book.application.port.outbound

import org.keon.book.application.type.EpochSecond

interface UserBookingsReadRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val accountId: String,
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
