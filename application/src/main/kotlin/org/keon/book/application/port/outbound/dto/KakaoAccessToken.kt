package org.keon.book.application.port.outbound.dto

data class KakaoAccessToken(
    val accessToken: String,
    val refreshToken: String? = null,
)
