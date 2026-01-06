package org.keon.book.application.exception

class KakaoAuthenticationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)