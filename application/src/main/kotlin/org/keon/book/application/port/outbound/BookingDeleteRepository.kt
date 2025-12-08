package org.keon.book.application.port.outbound

interface BookingDeleteRepository {
    operator fun invoke(request: Request)

    data class Request(
        val bookingId: Long,
        val accountId: String,
    )
}
