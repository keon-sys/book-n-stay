package org.keon.book.application.port.outbound

import java.time.ZonedDateTime

interface BookingCreateRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val from: ZonedDateTime,
        val to: ZonedDateTime,
        val accountId: String,
    )

    data class Result(
        val id: Long,
        val from: ZonedDateTime,
        val to: ZonedDateTime,
        val accountId: String,
    )
}
