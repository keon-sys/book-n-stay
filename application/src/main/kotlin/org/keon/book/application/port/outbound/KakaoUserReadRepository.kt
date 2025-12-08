package org.keon.book.application.port.outbound

interface KakaoUserReadRepository {
    operator fun invoke(request: Request): Result

    sealed class Request {
        data class KakaoToken(
            val accessToken: String,
            val refreshToken: String?,
        ): Request()

        data class AccountId(
            val accountId: String,
        ): Request()
    }




    data class Result(
        val accountId: String,
        val nickname: String?,
    )
}