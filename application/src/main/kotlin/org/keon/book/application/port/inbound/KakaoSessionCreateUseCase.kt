package org.keon.book.application.port.inbound

interface KakaoSessionCreateUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val accessToken: String,
    )

    data class Response(
        val id: String,
        val nickname: String?,
    )
}