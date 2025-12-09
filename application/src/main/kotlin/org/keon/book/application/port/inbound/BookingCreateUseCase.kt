package org.keon.book.application.port.inbound

import org.keon.book.application.type.EpochSecond

interface BookingCreateUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val from: EpochSecond,
        val to: EpochSecond,
        val accountId: String,
    )

    data class Response(
        val bookings: List<BookingInfo>,
    )

    data class BookingInfo(
        val bookingId: Long,
        val date: EpochSecond,
        val nickname: String,
    )
}
