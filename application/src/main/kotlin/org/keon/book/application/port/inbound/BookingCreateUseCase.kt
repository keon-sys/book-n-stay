package org.keon.book.application.port.inbound

import java.time.ZonedDateTime

interface BookingCreateUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val from: ZonedDateTime,
        val to: ZonedDateTime,
        val accountId: String,
    )

    data class Response(
        val from: ZonedDateTime,
        val to: ZonedDateTime,
        val accountId: String,
    )
}
