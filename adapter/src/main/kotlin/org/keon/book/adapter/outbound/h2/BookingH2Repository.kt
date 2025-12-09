package org.keon.book.adapter.outbound.h2

import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingsReadRepository
import org.keon.book.application.port.outbound.UserBookingsReadRepository
import org.keon.book.application.type.EpochSecond
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.sql.Statement
import javax.sql.DataSource

data class BookingEntity(
    val id: Long,
    val date: Long,
    val accountId: String,
    val nickname: String,
)

@Component
@Profile("dev")
class BookingH2Repository(
    dataSource: DataSource,
) : BookingsReadRepository, UserBookingsReadRepository, BookingCreateRepository, BookingDeleteRepository {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    init {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS bookings (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                date BIGINT NOT NULL,
                account_id VARCHAR(255) NOT NULL,
                nickname VARCHAR(255) NOT NULL
            )
            """.trimIndent()
        )
    }

    private val rowMapper = RowMapper { rs, _ ->
        BookingEntity(
            id = rs.getLong("id"),
            date = rs.getLong("date"),
            accountId = rs.getString("account_id"),
            nickname = rs.getString("nickname"),
        )
    }

    override fun invoke(request: BookingsReadRepository.Request): BookingsReadRepository.Result {
        val startOfMonth = java.time.LocalDate.of(request.year, request.month, 1)
            .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
            .toEpochSecond()
        val endOfMonth = startOfMonth.let { start ->
            java.time.LocalDate.of(request.year, request.month, 1)
                .plusMonths(1)
                .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
                .toEpochSecond()
        }

        val entities = jdbcTemplate.query(
            "SELECT * FROM bookings WHERE date >= ? AND date < ?",
            rowMapper,
            startOfMonth,
            endOfMonth
        )

        val bookings = entities.map { entity ->
            BookingsReadRepository.BookingData(
                id = entity.id,
                date = EpochSecond(entity.date),
                accountId = entity.accountId,
                nickname = entity.nickname,
            )
        }
        return BookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingCreateRepository.Request): BookingCreateRepository.Result {
        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(
                "INSERT INTO bookings (date, account_id, nickname) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setLong(1, request.date.value)
            ps.setString(2, request.accountId)
            ps.setString(3, request.nickname)
            ps
        }, keyHolder)

        val generatedId = keyHolder.key!!.toLong()

        return BookingCreateRepository.Result(
            id = generatedId,
            date = request.date,
            accountId = request.accountId,
            nickname = request.nickname,
        )
    }

    override fun invoke(request: UserBookingsReadRepository.Request): UserBookingsReadRepository.Result {
        val entities = jdbcTemplate.query(
            "SELECT * FROM bookings WHERE account_id = ?",
            rowMapper,
            request.accountId
        )

        val bookings = entities.map { entity ->
            UserBookingsReadRepository.BookingData(
                id = entity.id,
                date = EpochSecond(entity.date),
                accountId = entity.accountId,
                nickname = entity.nickname,
            )
        }
        return UserBookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingDeleteRepository.Request) {
        jdbcTemplate.update(
            "DELETE FROM bookings WHERE id = ? AND account_id = ?",
            request.bookingId,
            request.accountId
        )
    }
}
