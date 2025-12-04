package org.keon.book.adapter.outbound.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.keon.book.adapter.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoUserRepository
import org.keon.book.application.port.outbound.dto.KakaoAccessToken
import org.keon.book.application.port.outbound.dto.KakaoUser
import org.keon.book.adapter.config.Properties
import org.keon.book.adapter.cache.CacheNames
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoUserClient(
    builder: RestClient.Builder,
    kakaoAccountIdProperty: Properties.KakaoAccountIdProperty,
    private val securityKakaoProperty: Properties.SecurityKakaoProperty,
) : KakaoUserRepository {

    private val userClient = builder
        .baseUrl(kakaoAccountIdProperty.kakaoBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
        .build()

    private val authClient = builder
        .baseUrl(kakaoAccountIdProperty.kakaoAuthBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
        .build()

    @Cacheable(cacheNames = [CacheNames.KAKAO_USER_BY_ACCESS_TOKEN], key = "#accessToken.accessToken")
    override fun fetchUser(accessToken: KakaoAccessToken): KakaoUser {
        val response = try {
            userClient.get()
                .uri(USER_INFO_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken.accessToken}")
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, clientResponse ->
                    KakaoAuthenticationException(
                        "Kakao API responded with ${clientResponse.statusCode} when fetching user info.",
                    )
                }
                .body(KakaoUserResponse::class.java)
        } catch (ex: RestClientException) {
            throw KakaoAuthenticationException("Failed to call Kakao user info API.", ex)
        }

        val kakaoId = response?.id
            ?: throw KakaoAuthenticationException("Kakao response did not contain an id.")
        return KakaoUser(
            id = kakaoId.toString(),
            nickname = response.kakaoAccount?.profile?.nickname,
            email = response.kakaoAccount?.email,
        )
    }

    override fun exchangeCodeForToken(authorizationCode: String, redirectUri: String): KakaoAccessToken {
        if (securityKakaoProperty.restApiKey.isBlank()) {
            throw KakaoAuthenticationException("Kakao REST API key is not configured.")
        }

        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", securityKakaoProperty.restApiKey)
            add("redirect_uri", redirectUri)
            add("code", authorizationCode)
            securityKakaoProperty.clientSecret?.takeIf { it.isNotBlank() }?.let { add("client_secret", it) }
        }

        val response = try {
            authClient.post()
                .uri(TOKEN_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, clientResponse ->
                    KakaoAuthenticationException(
                        "Kakao auth API responded with ${clientResponse.statusCode} when exchanging code.",
                    )
                }
                .body(KakaoTokenResponse::class.java)
        } catch (ex: RestClientException) {
            throw KakaoAuthenticationException("Failed to exchange Kakao authorization code.", ex)
        }

        val accessToken = response?.accessToken
            ?: throw KakaoAuthenticationException("Kakao token response did not contain an access token.")
        return KakaoAccessToken(
            accessToken = accessToken,
            refreshToken = response.refreshToken,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoUserResponse(
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String?,
        @JsonProperty("refresh_token")
        val refreshToken: String?,
    )

    companion object {
        const val BEAN_NAME = "kakaoLoginClient"

        private const val USER_INFO_PATH = "/v2/user/me"
        private const val TOKEN_PATH = "/oauth/token"
    }
}
