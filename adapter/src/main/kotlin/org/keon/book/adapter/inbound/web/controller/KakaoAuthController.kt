package org.keon.book.adapter.inbound.web.controller

import jakarta.servlet.http.HttpServletResponse
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.keon.book.adapter.auth.JwtAuthTokenService
import org.keon.book.adapter.config.Properties
import org.keon.book.application.port.inbound.AuthUseCase
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class KakaoAuthController(
    private val authUseCase: AuthUseCase,
    private val tokenService: JwtAuthTokenService,
    private val authTokenProperty: Properties.AuthTokenProperty,
    private val kakaoProperty: Properties.KakaoProperty,
) {

    @GetMapping("/api/v1/auth/kakao/config", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun config(): KakaoConfigResponse =
        KakaoConfigResponse(
            javascriptKey = kakaoProperty.javascriptKey,
            redirectUri = kakaoProperty.redirectUri,
        )

    @PostMapping("/api/v1/auth/kakao/session", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createSession(
        @RequestBody request: KakaoSessionRequest,
        response: HttpServletResponse,
    ): AuthUseCase.AuthResult {
        val authResult = authUseCase.createSession(request.accessToken)
        val token = tokenService.createToken(authResult.user.id)
        response.addHeader("Set-Cookie", buildCookie(token).toString())
        return authResult
    }

    @PostMapping("/api/v1/auth/kakao/token", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun exchangeCode(@RequestBody request: KakaoCodeRequest) =
        authUseCase.exchangeAuthorizationCode(request.authorizationCode, kakaoProperty.redirectUri)

    @GetMapping("/api/v1/auth/kakao/callback")
    fun handleCallback(
        @RequestParam("code") authorizationCode: String,
        @RequestParam(name = "state", required = false) state: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val token = authUseCase.exchangeAuthorizationCode(authorizationCode, kakaoProperty.redirectUri)
        val authResult = authUseCase.createSession(token.accessToken)
        val jwt = tokenService.createToken(authResult.user.id)
        response.addHeader("Set-Cookie", buildCookie(jwt).toString())

        val target = state?.takeIf { it.isNotBlank() }
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
            ?: "/"

        return ResponseEntity.status(302)
            .location(URI.create(target))
            .build()
    }

    private fun buildCookie(token: String): ResponseCookie =
        ResponseCookie.from(authTokenProperty.cookieName, token)
            .path(authTokenProperty.cookiePath)
            .httpOnly(true)
            .secure(authTokenProperty.cookieSecure)
            .sameSite(authTokenProperty.cookieSameSite)
            .maxAge(authTokenProperty.tokenValidity)
            .build()

    @PostMapping("/api/v1/auth/kakao/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        response.addHeader("Set-Cookie", deleteCookie().toString())
        return ResponseEntity.noContent().build()
    }

    private fun deleteCookie(): ResponseCookie =
        ResponseCookie.from(authTokenProperty.cookieName, "")
            .path(authTokenProperty.cookiePath)
            .httpOnly(true)
            .secure(authTokenProperty.cookieSecure)
            .sameSite(authTokenProperty.cookieSameSite)
            .maxAge(0)
            .build()

    data class KakaoSessionRequest(
        val accessToken: String,
    )

    data class KakaoCodeRequest(
        val authorizationCode: String,
    )

    data class KakaoConfigResponse(
        val javascriptKey: String,
        val redirectUri: String,
    )
}
