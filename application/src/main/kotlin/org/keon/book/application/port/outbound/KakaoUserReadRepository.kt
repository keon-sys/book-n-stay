package org.keon.book.application.port.outbound

interface KakaoUserReadRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val accessToken: String,
    )

    data class Result(
        val accountId: String,
        val nickname: String?,
    )
}