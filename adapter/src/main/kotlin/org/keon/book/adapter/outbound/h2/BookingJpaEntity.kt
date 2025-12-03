package org.keon.book.adapter.outbound.h2

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "bookings")
class BookingJpaEntity(
    @Column(name = "booking_date", nullable = false)
    var bookingDate: ZonedDateTime,

    @Column(name = "account_id", nullable = false)
    var accountId: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
