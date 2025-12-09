package org.keon.book.adapter.outbound.file

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingsReadRepository
import org.keon.book.application.port.outbound.UserBookingsReadRepository
import org.keon.book.application.type.EpochSecond
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class BookingFileEntity(
    val id: Long,
    val date: Long,
    val accountId: String,
    val nickname: String,
)

@Component
@Profile("prod")
class BookingFileRepository(
    @Value("\${storage.booking.path}")
    private val storagePath: String,
    private val objectMapper: ObjectMapper,
) : BookingsReadRepository, UserBookingsReadRepository, BookingCreateRepository, BookingDeleteRepository {

    private val lock = ReentrantReadWriteLock()
    private val idGenerator = AtomicLong(0L)
    private val storageFile: File = File(storagePath)

    init {
        storageFile.parentFile?.mkdirs()
        if (!storageFile.exists()) {
            storageFile.writeText("[]")
        } else {
            val bookings = readBookings()
            val maxId = bookings.maxOfOrNull { it.id } ?: 0L
            idGenerator.set(maxId)
        }
    }

    private fun readBookings(): List<BookingFileEntity> {
        return try {
            objectMapper.readValue<List<BookingFileEntity>>(storageFile)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeBookings(bookings: List<BookingFileEntity>) {
        storageFile.writeText(objectMapper.writeValueAsString(bookings))
    }

    override fun invoke(request: BookingsReadRepository.Request): BookingsReadRepository.Result {
        val startOfMonth = java.time.LocalDate.of(request.year, request.month, 1)
            .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
            .toEpochSecond()
        val endOfMonth = java.time.LocalDate.of(request.year, request.month, 1)
            .plusMonths(1)
            .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
            .toEpochSecond()

        val bookings = lock.read {
            readBookings()
                .filter { it.date >= startOfMonth && it.date < endOfMonth }
                .map { entity ->
                    BookingsReadRepository.BookingData(
                        id = entity.id,
                        date = EpochSecond(entity.date),
                        accountId = entity.accountId,
                        nickname = entity.nickname,
                    )
                }
        }

        return BookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingCreateRepository.Request): BookingCreateRepository.Result {
        return lock.write {
            val bookings = readBookings().toMutableList()
            val newId = idGenerator.incrementAndGet()
            val newEntity = BookingFileEntity(
                id = newId,
                date = request.date.value,
                accountId = request.accountId,
                nickname = request.nickname,
            )
            bookings.add(newEntity)
            writeBookings(bookings)

            BookingCreateRepository.Result(
                id = newEntity.id,
                date = EpochSecond(newEntity.date),
                accountId = newEntity.accountId,
                nickname = newEntity.nickname,
            )
        }
    }

    override fun invoke(request: UserBookingsReadRepository.Request): UserBookingsReadRepository.Result {
        val bookings = lock.read {
            readBookings()
                .filter { it.accountId == request.accountId }
                .map { entity ->
                    UserBookingsReadRepository.BookingData(
                        id = entity.id,
                        date = EpochSecond(entity.date),
                        accountId = entity.accountId,
                        nickname = entity.nickname,
                    )
                }
        }

        return UserBookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingDeleteRepository.Request) {
        lock.write {
            val bookings = readBookings().toMutableList()
            bookings.removeIf { it.id == request.bookingId && it.accountId == request.accountId }
            writeBookings(bookings)
        }
    }
}
