package org.keon.book.adapter.outbound.file

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.keon.book.application.port.outbound.GrantCreateRepository
import org.keon.book.application.port.outbound.GrantReadRepository
import org.keon.book.application.port.outbound.GrantUpdateRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class GrantFileEntity(
    val id: Long,
    val accountId: String,
    val level: Int,
)

@Component
@Profile("prod")
class GrantFileRepository(
    @Value("\${storage.grant.path}")
    private val storagePath: String,
    private val objectMapper: ObjectMapper,
) : GrantReadRepository, GrantCreateRepository, GrantUpdateRepository {

    private val lock = ReentrantReadWriteLock()
    private val idGenerator = AtomicLong(0L)
    private val storageFile: File = File(storagePath)

    init {
        storageFile.parentFile?.mkdirs()
        if (!storageFile.exists()) {
            storageFile.writeText("[]")
        } else {
            val grants = readGrants()
            val maxId = grants.maxOfOrNull { it.id } ?: 0L
            idGenerator.set(maxId)
        }
    }

    private fun readGrants(): List<GrantFileEntity> {
        return try {
            objectMapper.readValue<List<GrantFileEntity>>(storageFile)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeGrants(grants: List<GrantFileEntity>) {
        storageFile.writeText(objectMapper.writeValueAsString(grants))
    }

    override fun invoke(request: GrantReadRepository.Request): GrantReadRepository.Result? {
        return lock.read {
            val entity = readGrants().firstOrNull { it.accountId == request.accountId }
                ?: return@read null

            GrantReadRepository.Result(
                id = entity.id,
                accountId = entity.accountId,
                level = entity.level,
            )
        }
    }

    override fun invoke(request: GrantCreateRepository.Request): GrantCreateRepository.Result {
        return lock.write {
            val grants = readGrants().toMutableList()

            // Check for duplicate accountId (UNIQUE constraint)
            val existingGrant = grants.firstOrNull { it.accountId == request.accountId }
            if (existingGrant != null) {
                throw IllegalStateException("Grant with accountId '${request.accountId}' already exists")
            }

            val newId = idGenerator.incrementAndGet()
            val newGrant = GrantFileEntity(
                id = newId,
                accountId = request.accountId,
                level = request.level,
            )
            grants.add(newGrant)
            writeGrants(grants)

            GrantCreateRepository.Result(
                id = newGrant.id,
                accountId = newGrant.accountId,
                level = newGrant.level,
            )
        }
    }

    override fun invoke(request: GrantUpdateRepository.Request) {
        lock.write {
            val grants = readGrants().toMutableList()
            val existingIndex = grants.indexOfFirst { it.accountId == request.accountId }

            if (existingIndex != -1) {
                val existingGrant = grants[existingIndex]
                val updatedGrant = existingGrant.copy(level = request.level)
                grants[existingIndex] = updatedGrant
                writeGrants(grants)
            }
        }
    }
}
