package org.keon.book.application.service

import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingReadUseCase
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingReadRepository
import org.springframework.stereotype.Service

@Service
class BookingService(
    private val bookingReadRepository: BookingReadRepository,
    private val bookingCreateRepository: BookingCreateRepository,
    private val bookingDeleteRepository: BookingDeleteRepository,
) : BookingReadUseCase, BookingCreateUseCase, BookingDeleteUseCase {

    override fun invoke(query: BookingReadUseCase.Query): BookingReadUseCase.Response {
        val result = bookingReadRepository(BookingReadRepository.Request(query.date))
        return BookingReadUseCase.Response(result.bookings.map { it.accountId })
    }

    override fun invoke(command: BookingCreateUseCase.Command): BookingCreateUseCase.Response {
        val startOfDay = command.date.toLocalDate().atStartOfDay(command.date.zone)
        val endOfDay = startOfDay.plusDays(1)
        bookingCreateRepository(BookingCreateRepository.Request(
            from = startOfDay,
            to = endOfDay,
            accountId = command.accountId,
        ))
        return BookingCreateUseCase.Response(
            date = command.date,
            accountId = command.accountId,
        )
    }

    override fun invoke(command: BookingDeleteUseCase.Command): BookingDeleteUseCase.Response {
        val startOfDay = command.date.toLocalDate().atStartOfDay(command.date.zone)
        val endOfDay = startOfDay.plusDays(1)
        bookingDeleteRepository(BookingDeleteRepository.Request(
            from = startOfDay,
            to = endOfDay,
            accountId = command.accountId,
        ))
        return BookingDeleteUseCase.Response(
            date = command.date,
            accountId = command.accountId,
        )
    }
}
