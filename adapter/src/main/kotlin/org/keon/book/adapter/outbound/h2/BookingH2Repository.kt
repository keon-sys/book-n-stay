package org.keon.book.adapter.outbound.h2

import jakarta.persistence.*
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingsReadRepository
import org.keon.book.application.port.outbound.UserBookingsReadRepository
import org.keon.book.application.type.EpochSecond
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Entity
@Table(name = "bookings")
data class BookingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column(name = "start_time", nullable = false)
    val from: Long, // epochSecond

    @Column(name = "end_time", nullable = false)
    val to: Long, // epochSecond

    @Column(name = "account_id", nullable = false)
    val accountId: String,

    @Column(name = "nickname", nullable = false)
    val nickname: String,
)

@Repository
@Profile("dev")
interface BookingJpaRepository : JpaRepository<BookingEntity, Long> {
    fun findByFromBetween(start: Long, end: Long): List<BookingEntity>
    fun findByAccountId(accountId: String): List<BookingEntity>
    fun deleteByIdAndAccountId(id: Long, accountId: String)
}

@Component
@Profile("dev")
class BookingH2Repository(
    private val jpaRepository: BookingJpaRepository,
) : BookingsReadRepository, UserBookingsReadRepository, BookingCreateRepository, BookingDeleteRepository {

    override fun invoke(request: BookingsReadRepository.Request): BookingsReadRepository.Result {
        // request.date는 특정 날짜의 시작 시각 (00:00:00 UTC)
        val startOfDay = request.date.value
        val endOfDay = (request.date + 86400).value // +1 day in seconds

        val entities = jpaRepository.findByFromBetween(startOfDay, endOfDay)
        val bookings = entities.map { entity ->
            BookingsReadRepository.BookingData(
                id = entity.id,
                from = EpochSecond(entity.from),
                to = EpochSecond(entity.to),
                accountId = entity.accountId,
                nickname = entity.nickname,
            )
        }
        return BookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingCreateRepository.Request): BookingCreateRepository.Result {
        val entity = BookingEntity(
            id = null,
            from = request.from.value,
            to = request.to.value,
            accountId = request.accountId,
            nickname = request.nickname,
        )
        val saved = jpaRepository.save(entity)
        return BookingCreateRepository.Result(
            id = saved.id!!,
            from = EpochSecond(saved.from),
            to = EpochSecond(saved.to),
            accountId = saved.accountId,
            nickname = saved.nickname,
        )
    }

    override fun invoke(request: UserBookingsReadRepository.Request): UserBookingsReadRepository.Result {
        val entities = jpaRepository.findByAccountId(request.accountId)
        val bookings = entities.map { entity ->
            UserBookingsReadRepository.BookingData(
                id = entity.id,
                from = EpochSecond(entity.from),
                to = EpochSecond(entity.to),
                accountId = entity.accountId,
                nickname = entity.nickname,
            )
        }
        return UserBookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingDeleteRepository.Request) {
        jpaRepository.deleteByIdAndAccountId(
            id = request.bookingId,
            accountId = request.accountId,
        )
    }
}
