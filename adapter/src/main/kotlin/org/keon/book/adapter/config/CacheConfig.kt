package org.keon.book.adapter.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.keon.book.adapter.cache.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig(
    private val cacheProperty: Properties.CacheProperty,
) {

    @Bean
    fun cacheManager(): CacheManager =
        CaffeineCacheManager().apply {
            isAllowNullValues = false
            setCacheNames(listOf(CacheNames.KAKAO_USER_BY_ACCESS_TOKEN))
            setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(cacheProperty.kakaoUser.ttl)
                    .maximumSize(cacheProperty.kakaoUser.maximumSize),
            )
        }
}
