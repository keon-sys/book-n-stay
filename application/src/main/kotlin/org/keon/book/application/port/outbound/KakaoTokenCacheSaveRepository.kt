package org.keon.book.application.port.outbound

interface KakaoTokenCacheSaveRepository {
    operator fun invoke(request: Request)

    data class Request(
        val accountId: String,
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}
