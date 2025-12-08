package org.keon.book.application.port.inbound

import java.time.ZonedDateTime

interface BookingsReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val date: ZonedDateTime,
    )

    data class Response(
        val userIds: List<String>,
    )
}
