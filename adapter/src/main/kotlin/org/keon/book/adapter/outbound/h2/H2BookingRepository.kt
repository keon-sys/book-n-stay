package org.keon.book.adapter.outbound.h2

import org.keon.book.application.port.outbound.BookingRepository
import org.keon.book.domain.booking.Booking
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class H2BookingRepository(
    private val bookingJpaRepository: BookingJpaRepository,
) : BookingRepository {

    override fun findByDate(date: ZonedDateTime): List<Booking> =
        bookingJpaRepository.findByBookingDate(date).map { it.toDomain() }

    override fun save(booking: Booking) {
        bookingJpaRepository.save(booking.toEntity())
    }

    override fun delete(booking: Booking) {
        bookingJpaRepository.deleteByBookingDateAndAccountId(booking.date, booking.accountId)
    }

    private fun BookingJpaEntity.toDomain(): Booking =
        Booking(date = bookingDate, accountId = accountId)

    private fun Booking.toEntity(): BookingJpaEntity =
        BookingJpaEntity(bookingDate = date, accountId = accountId)
}
