package org.keon.book.application.port.inbound

import java.time.ZonedDateTime

interface BookingUseCase {
    fun getBookings(query: BookingsQuery): BookingsResponse

    fun setBooking(command: BookingCommand)

    fun removeBooking(command: BookingCommand)

    data class BookingsQuery(
        val date: ZonedDateTime,
    )

    data class BookingCommand(
        val date: ZonedDateTime,
        val accountId: String,
    )

    data class BookingsResponse(
        val accountIds: List<String>,
    )
}
