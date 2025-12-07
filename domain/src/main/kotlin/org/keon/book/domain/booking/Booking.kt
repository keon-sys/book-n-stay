package org.keon.book.domain.booking

import java.time.ZonedDateTime

data class Booking(
    val id: Long?,
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    val accountId: String,
)
