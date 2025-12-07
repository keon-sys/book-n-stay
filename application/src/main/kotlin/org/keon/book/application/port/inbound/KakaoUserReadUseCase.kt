package org.keon.book.application.port.inbound

interface KakaoUserReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val accountId: String,
    )

    data class Response(
        val accountId: String,
        val nickname: String?,
    )
}