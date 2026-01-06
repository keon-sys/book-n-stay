package org.keon.book.application.port.inbound

interface UserReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val accountId: String,
    )

    data class Response(
        val nickname: String,
        val grantLevel: Int,
    )
}