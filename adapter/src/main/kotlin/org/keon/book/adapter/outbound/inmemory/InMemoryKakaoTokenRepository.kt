package org.keon.book.adapter.outbound.inmemory

import org.keon.book.application.port.outbound.KakaoTokenRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryKakaoTokenRepository : KakaoTokenRepository {
    private val tokenStore = ConcurrentHashMap<String, KakaoTokenRepository.FindResult>()

    override fun save(request: KakaoTokenRepository.SaveRequest) {
        tokenStore[request.userId] = KakaoTokenRepository.FindResult(
            accessToken = request.accessToken,
            refreshToken = request.refreshToken,
            expiresIn = request.expiresIn,
            refreshTokenExpiresIn = request.refreshTokenExpiresIn,
        )
    }

    override fun findByUserId(request: KakaoTokenRepository.FindRequest): KakaoTokenRepository.FindResult? {
        return tokenStore[request.userId]
    }

    override fun delete(request: KakaoTokenRepository.DeleteRequest) {
        tokenStore.remove(request.userId)
    }
}
