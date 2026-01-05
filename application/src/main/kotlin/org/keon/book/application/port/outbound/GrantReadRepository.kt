package org.keon.book.application.port.outbound

interface GrantReadRepository {
    operator fun invoke(request: Request): Result?

    data class Request(
        val accountId: String,
    )

    data class Result(
        val id: Long,
        val accountId: String,
        val level: Int,
    )
}