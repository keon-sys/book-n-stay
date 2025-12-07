package org.keon.book.adapter.outbound.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.keon.book.adapter.config.Properties
import org.keon.book.adapter.exception.KakaoAuthenticationException
import org.keon.book.application.port.outbound.KakaoUserReadRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoUserReadRepositoryAdapter(
    builder: RestClient.Builder,
    kakaoAccountIdProperty: Properties.KakaoAccountIdProperty,
) : KakaoUserReadRepository {

    private val userClient = builder
        .baseUrl(kakaoAccountIdProperty.kakaoBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
        .build()

    override fun invoke(request: KakaoUserReadRepository.Request): KakaoUserReadRepository.Result {
        val response = try {
            userClient.get()
                .uri(USER_INFO_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${request.accessToken}")
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

        val kakaoId = response?.id
            ?: throw KakaoAuthenticationException("Kakao response did not contain an id.")
        return KakaoUserReadRepository.Result(
            id = kakaoId.toString(),
            nickname = response.kakaoAccount?.profile?.nickname,
        )
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
