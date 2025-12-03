package org.keon.book.adapter.outbound.h2

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface BookingJpaRepository : JpaRepository<BookingJpaEntity, Long> {
    fun findByBookingDate(bookingDate: ZonedDateTime): List<BookingJpaEntity>
    fun deleteByBookingDateAndAccountId(bookingDate: ZonedDateTime, accountId: String)
}
