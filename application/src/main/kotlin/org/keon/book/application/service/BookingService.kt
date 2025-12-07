package org.keon.book.application.service

import org.keon.book.application.port.inbound.BookingUseCase
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingReadRepository
import org.springframework.stereotype.Service

@Service
class BookingService(
    private val bookingReadRepository: BookingReadRepository,
    private val bookingCreateRepository: BookingCreateRepository,
    private val bookingDeleteRepository: BookingDeleteRepository,
) : BookingUseCase {

    override fun getBookings(query: BookingUseCase.BookingsQuery): BookingUseCase.BookingsResponse {
        val result = bookingReadRepository(BookingReadRepository.Request(query.date))
        return BookingUseCase.BookingsResponse(result.bookings.map { it.accountId })
    }

    override fun setBooking(command: BookingUseCase.BookingCommand) {
        val startOfDay = command.date.toLocalDate().atStartOfDay(command.date.zone)
        val endOfDay = startOfDay.plusDays(1)
        bookingCreateRepository(BookingCreateRepository.Request(
            from = startOfDay,
            to = endOfDay,
            accountId = command.accountId,
        ))
    }

    override fun removeBooking(command: BookingUseCase.BookingCommand) {
        val startOfDay = command.date.toLocalDate().atStartOfDay(command.date.zone)
        val endOfDay = startOfDay.plusDays(1)
        bookingDeleteRepository(BookingDeleteRepository.Request(
            from = startOfDay,
            to = endOfDay,
            accountId = command.accountId,
        ))
    }
}
