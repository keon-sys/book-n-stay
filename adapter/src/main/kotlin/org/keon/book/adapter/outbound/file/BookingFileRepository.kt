package org.keon.book.adapter.outbound.file

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingsReadRepository
import org.keon.book.application.port.outbound.MyBookingsReadRepository
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
    val from: Long,
    val to: Long,
    val accountId: String,
    val nickname: String,
)

@Component
@Profile("prod")
class BookingFileRepository(
    @Value("\${booking.storage.path:./data/bookings.json}")
    private val storagePath: String,
    private val objectMapper: ObjectMapper,
) : BookingsReadRepository, MyBookingsReadRepository, BookingCreateRepository, BookingDeleteRepository {

    private val lock = ReentrantReadWriteLock()
    private val idGenerator = AtomicLong(0L)
    private val storageFile: File = File(storagePath)

    init {
        storageFile.parentFile?.mkdirs()
        if (!storageFile.exists()) {
            storageFile.writeText("[]")
        } else {
            // 기존 파일에서 최대 ID를 찾아 idGenerator 초기화
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
        val startOfDay = request.date.value
        val endOfDay = (request.date + 86400).value

        val bookings = lock.read {
            readBookings()
                .filter { it.from >= startOfDay && it.from < endOfDay }
                .map { entity ->
                    BookingsReadRepository.BookingData(
                        id = entity.id,
                        from = EpochSecond(entity.from),
                        to = EpochSecond(entity.to),
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
                from = request.from.value,
                to = request.to.value,
                accountId = request.accountId,
                nickname = request.nickname,
            )
            bookings.add(newEntity)
            writeBookings(bookings)

            BookingCreateRepository.Result(
                id = newEntity.id,
                from = EpochSecond(newEntity.from),
                to = EpochSecond(newEntity.to),
                accountId = newEntity.accountId,
                nickname = newEntity.nickname,
            )
        }
    }

    override fun invoke(request: MyBookingsReadRepository.Request): MyBookingsReadRepository.Result {
        val bookings = lock.read {
            readBookings()
                .filter { it.accountId == request.accountId }
                .map { entity ->
                    MyBookingsReadRepository.BookingData(
                        id = entity.id,
                        from = EpochSecond(entity.from),
                        to = EpochSecond(entity.to),
                        accountId = entity.accountId,
                        nickname = entity.nickname,
                    )
                }
        }

        return MyBookingsReadRepository.Result(bookings)
    }

    override fun invoke(request: BookingDeleteRepository.Request) {
        lock.write {
            val bookings = readBookings().toMutableList()
            bookings.removeIf { it.id == request.bookingId && it.accountId == request.accountId }
            writeBookings(bookings)
        }
    }
}
