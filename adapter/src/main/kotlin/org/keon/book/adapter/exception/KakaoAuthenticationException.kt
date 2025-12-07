package org.keon.book.adapter.exception

class KakaoAuthenticationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)