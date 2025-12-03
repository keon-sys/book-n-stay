package org.keon.book.application.port.outbound.dto

data class KakaoUser(
    val id: String,
    val nickname: String?,
    val email: String?,
)
