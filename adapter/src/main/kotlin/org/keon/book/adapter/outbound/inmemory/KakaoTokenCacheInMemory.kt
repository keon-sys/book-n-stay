package org.keon.book.adapter.outbound.inmemory

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
        tokenStore[request.userId] = KakaoTokenCacheEntity(
            accessToken = request.accessToken,
            refreshToken = request.refreshToken,
            expiresIn = request.expiresIn,
            refreshTokenExpiresIn = request.refreshTokenExpiresIn,
        )
    }

    override fun invoke(request: KakaoTokenCacheReadRepository.Request): KakaoTokenCacheReadRepository.Result? {
        return tokenStore[request.userId]?.let { entity ->
            KakaoTokenCacheReadRepository.Result(
                accessToken = entity.accessToken,
                refreshToken = entity.refreshToken,
                expiresIn = entity.expiresIn,
                refreshTokenExpiresIn = entity.refreshTokenExpiresIn,
            )
        }
    }

    override fun invoke(request: KakaoTokenCacheDeleteRepository.Request) {
        tokenStore.remove(request.userId)
    }

    private data class KakaoTokenCacheEntity(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}
