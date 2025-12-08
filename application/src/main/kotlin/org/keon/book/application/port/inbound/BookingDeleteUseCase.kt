package org.keon.book.application.port.inbound

interface BookingDeleteUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val bookingId: Long,
        val accountId: String,
    )

    data class Response(
        val bookingId: Long,
        val accountId: String,
    )
}
