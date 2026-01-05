package org.keon.book.application.port.outbound

interface GrantUpdateRepository {
    operator fun invoke(request: Request)

    data class Request(
        val accountId: String,
        val level: Int,
    )
}
