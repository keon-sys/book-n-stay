package org.keon.book.adapter.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.keon.book.application.cache.CacheNames
import org.keon.book.application.cache.CacheProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheProperties = CacheProperties.default()

        return CaffeineCacheManager().apply {
            isAllowNullValues = false
            setCacheNames(listOf(CacheNames.KAKAO_USER_BY_ACCESS_TOKEN))
            setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(cacheProperties.kakaoUser.ttl)
                    .maximumSize(cacheProperties.kakaoUser.maximumSize),
            )
        }
    }
}
