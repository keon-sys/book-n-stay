package org.keon.book.application.port.inbound

interface NicknameUseCase {
    fun getNickname(query: NicknameQuery): NicknameResult

    data class NicknameQuery(
        val accessToken: String,
    )

    data class NicknameResult(
        val nickname: String,
    )
}