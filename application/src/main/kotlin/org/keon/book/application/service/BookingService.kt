package org.keon.book.application.service

import org.keon.book.application.exception.BookingCapacityExceededException
import org.keon.book.application.exception.DuplicateUserBookingException
import org.keon.book.application.exception.PastBookingDeletionException
import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingsReadUseCase
import org.keon.book.application.port.inbound.UserBookingsReadUseCase
import org.keon.book.application.port.outbound.*
import org.keon.book.application.type.EpochSecond
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class BookingService(
    private val bookingsReadRepository: BookingsReadRepository,
    private val userBookingsReadRepository: UserBookingsReadRepository,
    private val bookingCreateRepository: BookingCreateRepository,
    private val bookingDeleteRepository: BookingDeleteRepository,
    private val kakaoUserReadRepository: KakaoUserReadRepository,
) : BookingsReadUseCase, UserBookingsReadUseCase, BookingCreateUseCase, BookingDeleteUseCase {

    companion object {
        private const val MAX_CAPACITY_PER_DAY = 8
        private val SEOUL_ZONE = ZoneId.of("Asia/Seoul")
    }

    override fun invoke(query: BookingsReadUseCase.Query): BookingsReadUseCase.Response {
        val result = bookingsReadRepository(BookingsReadRepository.Request(
            year = query.year,
            month = query.month
        ))
        val bookings = result.bookings.map { booking ->
            BookingsReadUseCase.BookingInfo(
                date = booking.date,
                nickname = booking.nickname,
            )
        }
        return BookingsReadUseCase.Response(bookings)
    }

    override fun invoke(query: UserBookingsReadUseCase.Query): UserBookingsReadUseCase.Response {
        val result = userBookingsReadRepository(UserBookingsReadRepository.Request(query.accountId))

        val todayStart = java.time.ZonedDateTime.now(SEOUL_ZONE)
            .toLocalDate()
            .atStartOfDay(SEOUL_ZONE)
            .toEpochSecond()

        val bookings = result.bookings
            .filter { it.date.value >= todayStart }
            .sortedBy { it.date.value }
            .map { booking ->
                UserBookingsReadUseCase.BookingInfo(
                    bookingId = booking.id,
                    date = booking.date,
                    nickname = booking.nickname,
                )
            }

        return UserBookingsReadUseCase.Response(bookings)
    }

    override fun invoke(command: BookingCreateUseCase.Command): BookingCreateUseCase.Response {
        val dates = splitIntoDates(command.from, command.to)

        val userInfo = kakaoUserReadRepository(
            KakaoUserReadRepository.Request.AccountId(command.accountId)
        )
        val nickname = userInfo.nickname ?: throw IllegalStateException("User nickname not found")

        val datesByMonth = dates.groupBy { date ->
            val dateTime = date.toZonedDateTime().withZoneSameInstant(SEOUL_ZONE)
            dateTime.year to dateTime.monthValue
        }

        for ((yearMonth, datesInMonth) in datesByMonth) {
            val (year, month) = yearMonth
            val existingBookings = bookingsReadRepository(
                BookingsReadRepository.Request(year = year, month = month)
            )

            for (date in datesInMonth) {
                val hasSameUserBooking = existingBookings.bookings.any {
                    it.accountId == command.accountId && it.date.value == date.value
                }
                if (hasSameUserBooking) {
                    throw DuplicateUserBookingException(date = date.value)
                }

                val currentCount = existingBookings.bookings.count { it.date.value == date.value }
                if (currentCount >= MAX_CAPACITY_PER_DAY) {
                    throw BookingCapacityExceededException(
                        date = date.value,
                        currentCount = currentCount,
                        maxCapacity = MAX_CAPACITY_PER_DAY,
                    )
                }
            }
        }

        val createdBookings = dates.map { date ->
            val result = bookingCreateRepository(BookingCreateRepository.Request(
                date = date,
                accountId = command.accountId,
                nickname = nickname,
            ))

            BookingCreateUseCase.BookingInfo(
                bookingId = result.id,
                date = result.date,
                nickname = result.nickname,
            )
        }

        return BookingCreateUseCase.Response(bookings = createdBookings)
    }

    override fun invoke(command: BookingDeleteUseCase.Command): BookingDeleteUseCase.Response {
        val userBookings = userBookingsReadRepository(UserBookingsReadRepository.Request(command.accountId))
        val booking = userBookings.bookings.find { it.id == command.bookingId }
            ?: throw IllegalArgumentException("해당 예약을 찾을 수 없습니다. (예약 ID: ${command.bookingId})")

        val todayStart = java.time.ZonedDateTime.now(SEOUL_ZONE)
            .toLocalDate()
            .atStartOfDay(SEOUL_ZONE)
            .toEpochSecond()

        if (booking.date.value < todayStart) {
            throw PastBookingDeletionException(
                bookingId = command.bookingId,
                date = booking.date.value,
            )
        }

        bookingDeleteRepository(BookingDeleteRepository.Request(
            bookingId = command.bookingId,
            accountId = command.accountId,
        ))
        return BookingDeleteUseCase.Response(
            bookingId = command.bookingId,
            accountId = command.accountId,
        )
    }

    private fun splitIntoDates(from: EpochSecond, to: EpochSecond): List<EpochSecond> {
        val fromDate = from.toZonedDateTime().withZoneSameInstant(SEOUL_ZONE).toLocalDate()
        val toDate = to.toZonedDateTime().withZoneSameInstant(SEOUL_ZONE).toLocalDate()

        val daysBetween = ChronoUnit.DAYS.between(fromDate, toDate).toInt()

        return (0 until daysBetween).map { dayOffset ->
            val date = fromDate.plusDays(dayOffset.toLong())
            val dateTime = date.atStartOfDay(SEOUL_ZONE)
            EpochSecond(dateTime.toEpochSecond())
        }
    }
}
