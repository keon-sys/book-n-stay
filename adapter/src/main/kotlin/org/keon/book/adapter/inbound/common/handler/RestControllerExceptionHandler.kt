package org.keon.book.adapter.inbound.common.handler

import org.keon.book.application.exception.KakaoAuthenticationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(
    basePackages = [
        "org.keon.book.adapter.inbound.http"
    ]
)
class RestControllerExceptionHandler {

    @ExceptionHandler(KakaoAuthenticationException::class)
    fun handleKakaoAuthenticationException(ex: KakaoAuthenticationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    status = HttpStatus.UNAUTHORIZED.value(),
                    error = HttpStatus.UNAUTHORIZED.reasonPhrase,
                    message = ex.message ?: "Authentication failed",
                    redirectUrl = "/auth/kakao"
                )
            )
    }

    data class ErrorResponse(
        val status: Int,
        val error: String,
        val message: String,
        val redirectUrl: String? = null
    )
}