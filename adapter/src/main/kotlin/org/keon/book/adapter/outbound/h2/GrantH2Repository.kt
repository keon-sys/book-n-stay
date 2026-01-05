package org.keon.book.adapter.outbound.h2

import org.keon.book.application.port.outbound.GrantCreateRepository
import org.keon.book.application.port.outbound.GrantReadRepository
import org.keon.book.application.port.outbound.GrantUpdateRepository
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.sql.Statement
import javax.sql.DataSource

data class GrantEntity(
    val id: Long,
    val accountId: String,
    val level: Int,
)

@Component
@Profile("dev")
class GrantH2Repository(
    dataSource: DataSource,
) : GrantReadRepository, GrantCreateRepository, GrantUpdateRepository {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    init {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS grants (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                account_id VARCHAR(255) NOT NULL UNIQUE,
                level INT NOT NULL
            )
            """.trimIndent()
        )
    }

    private val rowMapper = RowMapper { rs, _ ->
        GrantEntity(
            id = rs.getLong("id"),
            accountId = rs.getString("account_id"),
            level = rs.getInt("level"),
        )
    }

    override fun invoke(request: GrantReadRepository.Request): GrantReadRepository.Result? {
        val entities = jdbcTemplate.query(
            "SELECT * FROM grants WHERE account_id = ?",
            rowMapper,
            request.accountId
        )

        val entity = entities.firstOrNull() ?: return null

        return GrantReadRepository.Result(
            id = entity.id,
            accountId = entity.accountId,
            level = entity.level,
        )
    }

    override fun invoke(request: GrantCreateRepository.Request): GrantCreateRepository.Result {
        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(
                "INSERT INTO grants (account_id, level) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            ps.setString(1, request.accountId)
            ps.setInt(2, request.level)
            ps
        }, keyHolder)

        val generatedId = keyHolder.key!!.toLong()

        return GrantCreateRepository.Result(
            id = generatedId,
            accountId = request.accountId,
            level = request.level,
        )
    }

    override fun invoke(request: GrantUpdateRepository.Request) {
        jdbcTemplate.update(
            "UPDATE grants SET level = ? WHERE account_id = ?",
            request.level,
            request.accountId
        )
    }
}
