package org.keon.book.domain.booking

import java.time.ZonedDateTime

data class Booking(
    val bookingId: String,
    val accountId: String,
    val nickname: String,
    val from: ZonedDateTime,
    val to: ZonedDateTime,
)
