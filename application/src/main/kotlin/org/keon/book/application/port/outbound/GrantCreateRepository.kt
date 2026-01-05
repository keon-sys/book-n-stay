package org.keon.book.application.port.outbound

interface GrantCreateRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val accountId: String,
        val level: Int,
    )

    data class Result(
        val id: Long,
        val accountId: String,
        val level: Int,
    )
}