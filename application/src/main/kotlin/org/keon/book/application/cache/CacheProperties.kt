package org.keon.book.application.cache

import java.time.Duration

data class CacheProperties(
    val kakaoUser: KakaoUserCacheProperties,
) {
    data class KakaoUserCacheProperties(
        val ttl: Duration,
        val maximumSize: Long,
    )

    companion object {
        fun default() = CacheProperties(
            kakaoUser = KakaoUserCacheProperties(
                ttl = Duration.ofMinutes(10),
                maximumSize = 1000,
            ),
        )
    }
}
