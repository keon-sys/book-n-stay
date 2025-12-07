package org.keon.book.adapter.outbound.kakao

import org.keon.book.adapter.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoTokenCacheSaveRepository
import org.keon.book.application.port.outbound.KakaoTokenRefreshRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class KakaoUserReadRefreshClient(
    private val delegate: KakaoUserReadClient,
    private val tokenRefreshRepository: KakaoTokenRefreshRepository,
    private val tokenCacheSaveRepository: KakaoTokenCacheSaveRepository,
) : KakaoUserReadRepository {

    override fun invoke(request: KakaoUserReadRepository.Request): KakaoUserReadRepository.Result {
        return try {
            delegate.invoke(request)
        } catch (ex: KakaoAuthenticationException) {
            val refreshToken = request.refreshToken
            if (isTokenExpired(ex) && refreshToken != null) {
                val newToken = tokenRefreshRepository(KakaoTokenRefreshRepository.Request(refreshToken))
                try {
                    val newRequest = KakaoUserReadRepository.Request(
                        accessToken = newToken.accessToken,
                        refreshToken = newToken.refreshToken,
                    )
                    val user = delegate.invoke(newRequest)
                    tokenCacheSaveRepository(KakaoTokenCacheSaveRepository.Request(
                        accountId = user.accountId,
                        accessToken = newToken.accessToken,
                        refreshToken = newToken.refreshToken,
                        expiresIn = newToken.expiresIn,
                        refreshTokenExpiresIn = newToken.refreshTokenExpiresIn,
                    ))
                    user
                } catch (retryEx: Exception) {
                    throw KakaoAuthenticationException("Failed to fetch user info after token refresh.", retryEx)
                }
            } else {
                throw ex
            }
        }
    }

    private fun isTokenExpired(ex: KakaoAuthenticationException): Boolean {
        val message = ex.message ?: return false
        return message.contains("401") || message.contains("expired") || message.contains("invalid_token")
    }
}
