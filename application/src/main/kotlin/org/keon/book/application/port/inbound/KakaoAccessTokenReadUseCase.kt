package org.keon.book.application.port.inbound

interface KakaoAccessTokenReadUseCase {
    operator fun invoke(query: Query): Response

    data class Query(
        val code: String,
        val redirectUri: String,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String?,
    )
}