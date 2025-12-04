package org.keon.book.application.port.outbound

import org.keon.book.application.port.outbound.dto.KakaoAccessToken
import org.keon.book.application.port.outbound.dto.KakaoUser

interface KakaoUserRepository {
    fun fetchUser(accessToken: KakaoAccessToken): KakaoUser
    fun exchangeCodeForToken(authorizationCode: String, redirectUri: String): KakaoAccessToken
}
