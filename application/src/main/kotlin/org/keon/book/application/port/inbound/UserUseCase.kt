package org.keon.book.application.port.inbound

interface UserUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val userId: String,
    )

    data class Response(
        val nickname: String,
    )
}