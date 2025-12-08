package org.keon.book.adapter.outbound.inmemory

import org.keon.book.adapter.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoTokenCacheDeleteRepository
import org.keon.book.application.port.outbound.KakaoTokenCacheReadRepository
import org.keon.book.application.port.outbound.KakaoTokenCacheSaveRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class KakaoTokenCacheInMemory :
    KakaoTokenCacheSaveRepository,
    KakaoTokenCacheReadRepository,
    KakaoTokenCacheDeleteRepository {

    private val tokenStore = ConcurrentHashMap<String, KakaoTokenCacheEntity>()

    override fun invoke(request: KakaoTokenCacheSaveRepository.Request) {
        tokenStore[request.accountId] = KakaoTokenCacheEntity(
            accessToken = request.accessToken,
            refreshToken = request.refreshToken,
            expiresIn = request.expiresIn,
            refreshTokenExpiresIn = request.refreshTokenExpiresIn,
        )
    }

    override fun invoke(request: KakaoTokenCacheReadRepository.Request): KakaoTokenCacheReadRepository.Result {
        val entity = tokenStore[request.accountId]
            ?: throw KakaoAuthenticationException("Token not found for accountId: ${request.accountId}")

        return KakaoTokenCacheReadRepository.Result(
            accessToken = entity.accessToken,
            refreshToken = entity.refreshToken,
            expiresIn = entity.expiresIn,
            refreshTokenExpiresIn = entity.refreshTokenExpiresIn,
        )
    }

    override fun invoke(request: KakaoTokenCacheDeleteRepository.Request) {
        tokenStore.remove(request.accountId)
    }

    private data class KakaoTokenCacheEntity(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}
