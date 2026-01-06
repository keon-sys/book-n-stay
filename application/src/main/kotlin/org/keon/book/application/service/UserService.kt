package org.keon.book.application.service

import org.keon.book.application.exception.KakaoAuthenticationException
import org.keon.book.application.port.inbound.KakaoLoginUseCase
import org.keon.book.application.port.inbound.UserReadUseCase
import org.keon.book.application.port.outbound.*
import org.keon.book.domain.User
import org.springframework.stereotype.Service

@Service
class UserService(
    private val kakaoUserReadRepository: KakaoUserReadRepository,
    private val kakaoTokenFetchRepository: KakaoTokenFetchRepository,
    private val kakaoTokenRefreshRepository: KakaoTokenRefreshRepository,
    private val kakaoSessionReadRepository: KakaoSessionReadRepository,
    private val kakaoSessionSaveRepository: KakaoSessionSaveRepository,
    private val grantReadRepository: GrantReadRepository,
    private val grantCreateRepository: GrantCreateRepository,
) : KakaoLoginUseCase, UserReadUseCase {

    override fun invoke(command: KakaoLoginUseCase.Command): KakaoLoginUseCase.Response {
        val token = kakaoTokenFetchRepository(
            KakaoTokenFetchRepository.Request(
                authorizationCode = command.code,
                redirectUri = command.redirectUri,
            )
        )

        val user = fetchKakaoUserOrRefreshIfTokenExpired(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
        )

        kakaoSessionSaveRepository(
            KakaoSessionSaveRepository.Request(
                accountId = user.accountId,
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                expiresIn = null,
                refreshTokenExpiresIn = null,
            )
        )

        return KakaoLoginUseCase.Response(
            accountId = user.accountId,
        )
    }

    override fun invoke(query: UserReadUseCase.Query): UserReadUseCase.Response {
        val user = readUser(query.accountId)
        return UserReadUseCase.Response(
            nickname = user.nickname,
            grantLevel = user.grantLevel,
        )
    }


    private fun readUser(accountId: String): User {
        val token = kakaoSessionReadRepository(
            KakaoSessionReadRepository.Request(accountId = accountId)
        )

        val kakaoUser = fetchKakaoUserOrRefreshIfTokenExpired(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
        )

        val grantLevel = fetchGrantLevelOrCreateIfAbsent(
            accountId = accountId,
        )

        return User(
            accountId = accountId,
            nickname = kakaoUser.nickname ?: throw RuntimeException("user nickname is null"),
            grantLevel = grantLevel,
        )
    }

    private fun fetchKakaoUserOrRefreshIfTokenExpired(
        accessToken: String,
        refreshToken: String?
    ): KakaoUserReadRepository.Result {
        return try {
            kakaoUserReadRepository(KakaoUserReadRepository.Request(accessToken = accessToken))
        } catch (ex: KakaoAuthenticationException) {
            if (isTokenExpired(ex) && refreshToken != null) {
                refreshTokenAndSaveSession(refreshToken)
            } else {
                throw ex
            }
        }
    }

    private fun refreshTokenAndSaveSession(refreshToken: String): KakaoUserReadRepository.Result {
        val newToken = kakaoTokenRefreshRepository(KakaoTokenRefreshRepository.Request(refreshToken))
        return try {
            val user = kakaoUserReadRepository(KakaoUserReadRepository.Request(newToken.accessToken))
            kakaoSessionSaveRepository(
                KakaoSessionSaveRepository.Request(
                    accountId = user.accountId,
                    accessToken = newToken.accessToken,
                    refreshToken = newToken.refreshToken,
                    expiresIn = newToken.expiresIn,
                    refreshTokenExpiresIn = newToken.refreshTokenExpiresIn,
                )
            )
            user
        } catch (retryEx: Exception) {
            throw KakaoAuthenticationException("Failed to fetch user info after token refresh.", retryEx)
        }
    }

    private fun isTokenExpired(ex: KakaoAuthenticationException): Boolean {
        val message = ex.message ?: return false
        return message.contains("401") || message.contains("expired") || message.contains("invalid_token")
    }

    private fun fetchGrantLevelOrCreateIfAbsent(accountId: String): Int {
        val grant = grantReadRepository(
            GrantReadRepository.Request(
                accountId = accountId,
            )
        )
        if (grant != null) {
            return grant.level
        }

        val newGrant = grantCreateRepository(
            GrantCreateRepository.Request(
                accountId = accountId,
                level = 1,
            )
        )
        return newGrant.level
    }
}
