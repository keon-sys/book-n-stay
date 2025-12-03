package org.keon.book.application.port.outbound

import org.keon.book.domain.booking.Booking
import java.time.ZonedDateTime

interface BookingRepository {

    fun findByDate(date: ZonedDateTime): List<Booking>

    fun save(booking: Booking)

    fun delete(booking: Booking)
}
