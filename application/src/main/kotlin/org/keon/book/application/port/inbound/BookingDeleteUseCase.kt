package org.keon.book.application.port.inbound

import java.time.ZonedDateTime

interface BookingDeleteUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val date: ZonedDateTime,
        val accountId: String,
    )

    data class Response(
        val date: ZonedDateTime,
        val accountId: String,
    )
}
