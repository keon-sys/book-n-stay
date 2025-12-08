package org.keon.book.adapter.outbound.h2

import jakarta.persistence.*
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingsReadRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Component
class BookingH2Repository(
    private val jpaRepository: BookingJpaRepository,
) : BookingsReadRepository, BookingCreateRepository, BookingDeleteRepository {

    override fun invoke(request: BookingsReadRepository.Request): BookingsReadRepository.Result {
        val startOfDay = request.date.toLocalDate().atStartOfDay(request.date.zone)
        val endOfDay = startOfDay.plusDays(1)

        val entities = jpaRepository.findByFromBetween(startOfDay, endOfDay)
        val bookings = entities.map { entity ->
            BookingsReadRepository.BookingData(
                id = entity.id,
                from = entity.from,
                to = entity.to,
                accountId = entity.accountId,
            )
        }
        return BookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingCreateRepository.Request): BookingCreateRepository.Result {
        val entity = BookingEntity(
            id = null,
            from = request.from,
            to = request.to,
            accountId = request.accountId,
        )
        val saved = jpaRepository.save(entity)
        return BookingCreateRepository.Result(
            id = saved.id!!,
            from = saved.from,
            to = saved.to,
            accountId = saved.accountId,
        )
    }

    override fun invoke(request: BookingDeleteRepository.Request) {
        jpaRepository.deleteByIdAndAccountId(
            id = request.bookingId,
            accountId = request.accountId,
        )
    }

    @Entity
    @Table(name = "bookings")
    data class BookingEntity(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long?,

        @Column(name = "start_time", nullable = false)
        val from: ZonedDateTime,

        @Column(name = "end_time", nullable = false)
        val to: ZonedDateTime,

        @Column(name = "account_id", nullable = false)
        val accountId: String,
    )
}

@Repository
interface BookingJpaRepository : JpaRepository<BookingH2Repository.BookingEntity, Long> {
    fun findByFromBetween(start: ZonedDateTime, end: ZonedDateTime): List<BookingH2Repository.BookingEntity>
    fun deleteByIdAndAccountId(id: Long, accountId: String)
}
