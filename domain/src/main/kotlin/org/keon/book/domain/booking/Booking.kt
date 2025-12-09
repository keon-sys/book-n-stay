package org.keon.book.domain.booking

import java.time.ZonedDateTime

data class Booking(
    val bookingId: Long,
    val accountId: String,
    val nickname: String,
    val date: ZonedDateTime,
)
