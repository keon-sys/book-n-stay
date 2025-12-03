package org.keon.book.adapter.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationPropertiesScan
data class Properties(
    val appProperty: AppProperty,
    val authTokenProperty: AuthTokenProperty,
    val kakaoProperty: KakaoProperty,
    val kakaoAccountIdProperty: KakaoAccountIdProperty,
) {
    @ConfigurationProperties(prefix = "app")
    data class AppProperty(
        val phase: AppPhase,
    ) {
        fun isProd(): Boolean = phase == AppPhase.PROD

        enum class AppPhase {
            DEV,
            PROD,
            ;
        }
    }

    @ConfigurationProperties(prefix = "security.auth")
    data class AuthTokenProperty(
        val tokenSecret: String,
        val issuer: String,
        val tokenValidity: Duration,
        val cookieName: String,
        val cookiePath: String,
        val cookieSameSite: String,
        val cookieSecure: Boolean,
    )

    @ConfigurationProperties(prefix = "security.kakao")
    data class KakaoProperty(
        val javascriptKey: String,
        val redirectUri: String,
        val restApiKey: String,
        val clientSecret: String?,
    )

    @ConfigurationProperties(prefix = "security.kakao-account-id")
    data class KakaoAccountIdProperty(
        val kakaoBaseUrl: String,
        val kakaoAuthBaseUrl: String,
    )
}
