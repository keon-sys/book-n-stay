package org.keon.book.domain.booking

import java.time.ZonedDateTime

data class Booking(
    val date: ZonedDateTime,
    val accountId: String,
)
