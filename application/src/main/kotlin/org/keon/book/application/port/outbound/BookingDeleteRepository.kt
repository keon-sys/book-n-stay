package org.keon.book.application.port.outbound

import java.time.ZonedDateTime

interface BookingDeleteRepository {
    operator fun invoke(request: Request)

    data class Request(
        val from: ZonedDateTime,
        val to: ZonedDateTime,
        val accountId: String,
    )
}
