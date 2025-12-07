package org.keon.book.adapter.outbound.inmemory

import org.keon.book.application.port.outbound.KakaoTokenCacheRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class KakaoTokenCacheInMemory : KakaoTokenCacheRepository {
    private val tokenStore = ConcurrentHashMap<String, KakaoTokenCacheRepository.FindResult>()

    override fun save(request: KakaoTokenCacheRepository.SaveRequest) {
        tokenStore[request.userId] = KakaoTokenCacheRepository.FindResult(
            accessToken = request.accessToken,
            refreshToken = request.refreshToken,
            expiresIn = request.expiresIn,
            refreshTokenExpiresIn = request.refreshTokenExpiresIn,
        )
    }

    override fun findByUserId(request: KakaoTokenCacheRepository.FindRequest): KakaoTokenCacheRepository.FindResult? {
        return tokenStore[request.userId]
    }

    override fun delete(request: KakaoTokenCacheRepository.DeleteRequest) {
        tokenStore.remove(request.userId)
    }
}
