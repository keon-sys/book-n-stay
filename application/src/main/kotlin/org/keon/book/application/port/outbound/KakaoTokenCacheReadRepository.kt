package org.keon.book.application.port.outbound

interface KakaoTokenCacheReadRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val accountId: String,
    )

    data class Result(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}
