package org.keon.book.adapter.outbound.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.keon.book.adapter.config.Properties
import org.keon.book.adapter.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoSessionReadRepository
import org.keon.book.application.port.outbound.KakaoSessionSaveRepository
import org.keon.book.application.port.outbound.KakaoTokenRefreshRepository
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoUserReadClient(
    builder: RestClient.Builder,
    kakaoAccountIdProperty: Properties.KakaoAccountIdProperty,
    private val kakaoTokenRefreshRepository: KakaoTokenRefreshRepository,
    private val kakaoSessionReadRepository: KakaoSessionReadRepository,
    private val kakaoSessionSaveRepository: KakaoSessionSaveRepository,
) : KakaoUserReadRepository {

    private val userClient = builder
        .baseUrl(kakaoAccountIdProperty.kakaoBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
        .build()

    override fun invoke(request: KakaoUserReadRepository.Request): KakaoUserReadRepository.Result {
        val (accessToken, refreshToken) = when (request) {
            is KakaoUserReadRepository.Request.KakaoToken -> request.accessToken to request.refreshToken
            is KakaoUserReadRepository.Request.AccountId ->
                kakaoSessionReadRepository(KakaoSessionReadRepository.Request(request.accountId))
                    .let { it.accessToken to it.refreshToken }
        }
        return try {
            fetchUserInfo(accessToken)
        } catch (ex: KakaoAuthenticationException) {
            val refreshToken = refreshToken
            if (isTokenExpired(ex) && refreshToken != null) {
                refreshTokenAndFetchUser(refreshToken)
            } else {
                throw ex
            }
        }
    }

    private fun fetchUserInfo(accessToken: String): KakaoUserReadRepository.Result {
        val response = try {
            userClient.get()
                .uri(USER_INFO_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, clientResponse ->
                    KakaoAuthenticationException(
                        "Kakao API responded with ${clientResponse.statusCode} when fetching user info.",
                    )
                }
                .body(KakaoUserEntity::class.java)
        } catch (ex: RestClientException) {
            throw KakaoAuthenticationException("Failed to call Kakao user info API.", ex)
        }

        val accountId = response?.id
            ?: throw KakaoAuthenticationException("Kakao response did not contain an id.")
        return KakaoUserReadRepository.Result(
            accountId = accountId.toString(),
            nickname = response.kakaoAccount?.profile?.nickname,
        )
    }

    private fun refreshTokenAndFetchUser(refreshToken: String): KakaoUserReadRepository.Result {
        val newToken = kakaoTokenRefreshRepository(KakaoTokenRefreshRepository.Request(refreshToken))
        return try {
            val user = fetchUserInfo(newToken.accessToken)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoUserEntity(
        @JsonProperty("id")
        val id: Long?,
        @JsonProperty("kakao_account")
        val kakaoAccount: KakaoAccount?,
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class KakaoAccount(
            @JsonProperty("email")
            val email: String?,
            @JsonProperty("profile")
            val profile: Profile?,
        ) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            data class Profile(
                @JsonProperty("nickname")
                val nickname: String?,
            )
        }
    }

    companion object {
        private const val USER_INFO_PATH = "/v2/user/me"
    }
}
