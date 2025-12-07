package org.keon.book.application.port.outbound

interface KakaoTokenRepository {
    fun save(request: SaveRequest)
    fun findByUserId(request: FindRequest): FindResult?
    fun delete(request: DeleteRequest)

    data class SaveRequest(
        val userId: String,
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )

    data class FindRequest(
        val userId: String,
    )

    data class FindResult(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int?,
        val refreshTokenExpiresIn: Int?,
    )

    data class DeleteRequest(
        val userId: String,
    )
}
