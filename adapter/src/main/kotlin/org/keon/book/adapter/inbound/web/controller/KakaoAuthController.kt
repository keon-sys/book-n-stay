package org.keon.book.adapter.inbound.web.controller

import jakarta.servlet.http.HttpServletResponse
import org.keon.book.adapter.auth.JwtAuthTokenService
import org.keon.book.adapter.config.Properties
import org.keon.book.application.port.inbound.KakaoAccessTokenReadUseCase
import org.keon.book.application.port.inbound.KakaoSessionCreateUseCase
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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

    @PostMapping("/api/v1/auth/kakao/session", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createSession(
        @RequestBody request: KakaoSessionRequest,
        response: HttpServletResponse,
    ): ResponseEntity<KakaoSessionResponse> {
        val sessionResult = KakaoSessionCreateUseCase.Command(request.accessToken)
            .run { kakaoSessionCreateUseCase(this) }
        val token = tokenService.createToken(sessionResult.id)
        response.addHeader("Set-Cookie", buildCookie(token).toString())
        return ResponseEntity
            .created(URI("/"))
            .body(KakaoSessionResponse(
                id = sessionResult.id,
                nickname = sessionResult.nickname,
            ))
    }

    @PostMapping("/api/v1/auth/kakao/token", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun exchangeCode(
        @RequestBody request: KakaoCodeRequest
    ): ResponseEntity<KakaoAccessTokenReadUseCase.Response> {
        val token = KakaoAccessTokenReadUseCase.Query(request.authorizationCode, securityKakaoProperty.redirectUri)
            .run(kakaoAccessTokenReadUseCase::invoke)
        return ResponseEntity.ok(token)
    }

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
        val token = KakaoAccessTokenReadUseCase.Query(authorizationCode, securityKakaoProperty.redirectUri)
            .run(kakaoAccessTokenReadUseCase::invoke)
        val authResult = KakaoSessionCreateUseCase.Command(token.accessToken)
            .run(kakaoSessionCreateUseCase::invoke)
        val jwt = tokenService.createToken(authResult.id)
        response.addHeader("Set-Cookie", buildCookie(jwt).toString())
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

    data class KakaoSessionResponse(
        val id: String,
        val nickname: String?,
    )
}
