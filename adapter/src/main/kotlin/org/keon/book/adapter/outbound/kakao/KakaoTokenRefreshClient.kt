package org.keon.book.adapter.outbound.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.keon.book.adapter.config.Properties
import org.keon.book.adapter.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoTokenRefreshRepository
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoTokenRefreshClient(
    builder: RestClient.Builder,
    kakaoAccountIdProperty: Properties.KakaoAccountIdProperty,
    private val securityKakaoProperty: Properties.SecurityKakaoProperty,
) : KakaoTokenRefreshRepository {

    private val authClient = builder
        .baseUrl(kakaoAccountIdProperty.kakaoAuthBaseUrl)
        .defaultHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
        .build()

    override fun invoke(request: KakaoTokenRefreshRepository.Request): KakaoTokenRefreshRepository.Result {
        if (securityKakaoProperty.restApiKey.isBlank()) {
            throw KakaoAuthenticationException("Kakao REST API key is not configured.")
        }

        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("client_id", securityKakaoProperty.restApiKey)
            add("refresh_token", request.refreshToken)
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
                        "Kakao auth API responded with ${clientResponse.statusCode} when refreshing token.",
                    )
                }
                .body(KakaoTokenResponse::class.java)
        } catch (ex: RestClientException) {
            throw KakaoAuthenticationException("Failed to refresh Kakao access token.", ex)
        }

        val accessToken = response?.accessToken
            ?: throw KakaoAuthenticationException("Kakao token response did not contain an access token.")
        return KakaoTokenRefreshRepository.Result(
            accessToken = accessToken,
            refreshToken = response.refreshToken,
            expiresIn = response.expiresIn,
            refreshTokenExpiresIn = response.refreshTokenExpiresIn,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String?,
        @JsonProperty("refresh_token")
        val refreshToken: String?,
        @JsonProperty("expires_in")
        val expiresIn: Int?,
        @JsonProperty("refresh_token_expires_in")
        val refreshTokenExpiresIn: Int?,
    )

    companion object {
        private const val TOKEN_PATH = "/oauth/token"
    }
}
