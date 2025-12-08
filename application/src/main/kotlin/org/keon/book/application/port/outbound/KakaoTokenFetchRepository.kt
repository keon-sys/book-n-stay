package org.keon.book.application.port.outbound

interface KakaoTokenFetchRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val authorizationCode: String,
        val redirectUri: String,
    )

    data class Result(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}