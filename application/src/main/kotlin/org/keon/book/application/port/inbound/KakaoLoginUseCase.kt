package org.keon.book.application.port.inbound

interface KakaoLoginUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val code: String,
        val redirectUri: String,
    )

    data class Response(
        val accountId: String,
    )
}