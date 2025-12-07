package org.keon.book.adapter.inbound.http.controller

import jakarta.servlet.http.HttpServletResponse
import org.keon.book.adapter.auth.JwtAuthTokenService
import org.keon.book.adapter.config.Properties
import org.keon.book.application.port.inbound.KakaoAccessTokenReadUseCase
import org.keon.book.application.port.inbound.KakaoSessionCreateUseCase
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RestController
class KakaoAuthController(
    private val kakaoAccessTokenReadUseCase: KakaoAccessTokenReadUseCase,
    private val kakaoSessionCreateUseCase: KakaoSessionCreateUseCase,
    private val tokenService: JwtAuthTokenService,
    private val securityAuthProperty: Properties.SecurityAuthProperty,
    private val securityKakaoProperty: Properties.SecurityKakaoProperty,
) {

    @GetMapping("/api/v1/auth/kakao/config", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun config(): KakaoConfigResponse =
        KakaoConfigResponse(
            javascriptKey = securityKakaoProperty.javascriptKey,
            redirectUri = securityKakaoProperty.redirectUri,
        )

    @PostMapping("/api/v1/auth/kakao/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        response.addHeader("Set-Cookie", deleteCookie().toString())
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/auth/kakao/callback")
    fun handleCallback(
        @RequestParam("code") authorizationCode: String,
        @RequestParam(name = "state", required = false) state: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        // OAuth 2.0 Authorization Code Flow
        // 1. Exchange authorization code for access token (server-to-server)
        // 2. Get user info from Kakao API using access token
        // 3. Create JWT session token and set httpOnly cookie
        // 4. Redirect to original page (from state parameter)
        val jwt = KakaoAccessTokenReadUseCase.Query(authorizationCode, securityKakaoProperty.redirectUri)
            .run(kakaoAccessTokenReadUseCase::invoke)
            .let { KakaoSessionCreateUseCase.Command(it.accessToken) }
            .run(kakaoSessionCreateUseCase::invoke)
            .let { tokenService.createToken(it.accountId) }

        response.addHeader("Set-Cookie", buildCookie(jwt).toString())

        // state parameter contains the original URL to redirect after login
        val target = state
            ?.takeIf { it.isNotBlank() }
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
            ?: "/"

        return ResponseEntity.status(302)
            .location(URI.create(target))
            .build()
    }

    private fun buildCookie(token: String): ResponseCookie =
        ResponseCookie.from(securityAuthProperty.cookieName, token)
            .path(securityAuthProperty.cookiePath)
            .httpOnly(true)
            .secure(securityAuthProperty.cookieSecure)
            .sameSite(securityAuthProperty.cookieSameSite)
            .maxAge(securityAuthProperty.tokenValidity)
            .build()

    private fun deleteCookie(): ResponseCookie =
        ResponseCookie.from(securityAuthProperty.cookieName, "")
            .path(securityAuthProperty.cookiePath)
            .httpOnly(true)
            .secure(securityAuthProperty.cookieSecure)
            .sameSite(securityAuthProperty.cookieSameSite)
            .maxAge(0)
            .build()

    data class KakaoConfigResponse(
        val javascriptKey: String,
        val redirectUri: String,
    )
}
