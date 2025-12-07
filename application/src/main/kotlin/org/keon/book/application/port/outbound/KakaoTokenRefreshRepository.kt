package org.keon.book.application.port.outbound

interface KakaoTokenRefreshRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val refreshToken: String,
    )

    data class Result(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}
