package org.keon.book.application.port.outbound

import org.keon.book.application.type.EpochSecond

interface BookingCreateRepository {
    operator fun invoke(request: Request): Result

    data class Request(
        val date: EpochSecond,
        val accountId: String,
        val nickname: String,
    )

    data class Result(
        val id: Long,
        val date: EpochSecond,
        val accountId: String,
        val nickname: String,
    )
}
