package org.keon.book.application.port.outbound

interface KakaoTokenCacheDeleteRepository {
    operator fun invoke(request: Request)

    data class Request(
        val accountId: String,
    )
}
