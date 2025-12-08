package org.keon.book.application.port.inbound

import org.keon.book.application.type.EpochSecond

interface BookingCreateUseCase {
    operator fun invoke(command: Command): Response

    data class Command(
        val from: EpochSecond,
        val to: EpochSecond,
        val accountId: String,
        val nickname: String,
    )

    data class Response(
        val id: Long,
        val from: EpochSecond,
        val to: EpochSecond,
        val accountId: String,
        val nickname: String,
    )
}
