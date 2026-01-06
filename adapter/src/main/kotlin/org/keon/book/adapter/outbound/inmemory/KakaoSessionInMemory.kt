package org.keon.book.adapter.outbound.inmemory

import org.keon.book.application.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoSessionDeleteRepository
import org.keon.book.application.port.outbound.KakaoSessionReadRepository
import org.keon.book.application.port.outbound.KakaoSessionSaveRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class KakaoSessionInMemory :
    KakaoSessionSaveRepository,
    KakaoSessionReadRepository,
    KakaoSessionDeleteRepository {

    private val tokenStore = ConcurrentHashMap<String, KakaoTokenCacheEntity>()

    override fun invoke(request: KakaoSessionSaveRepository.Request) {
        tokenStore[request.accountId] = KakaoTokenCacheEntity(
            accessToken = request.accessToken,
            refreshToken = request.refreshToken,
            expiresIn = request.expiresIn,
            refreshTokenExpiresIn = request.refreshTokenExpiresIn,
        )
    }

    override fun invoke(request: KakaoSessionReadRepository.Request): KakaoSessionReadRepository.Result {
        val entity = tokenStore[request.accountId]
            ?: throw KakaoAuthenticationException("Token not found for accountId: ${request.accountId}")

        return KakaoSessionReadRepository.Result(
            accessToken = entity.accessToken,
            refreshToken = entity.refreshToken,
            expiresIn = entity.expiresIn,
            refreshTokenExpiresIn = entity.refreshTokenExpiresIn,
        )
    }

    override fun invoke(request: KakaoSessionDeleteRepository.Request) {
        tokenStore.remove(request.accountId)
    }

    private data class KakaoTokenCacheEntity(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )
}
