package org.keon.book.application.service

import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingsReadUseCase
import org.keon.book.application.port.outbound.BookingCreateRepository
import org.keon.book.application.port.outbound.BookingDeleteRepository
import org.keon.book.application.port.outbound.BookingsReadRepository
import org.springframework.stereotype.Service

@Service
class BookingService(
    private val bookingsReadRepository: BookingsReadRepository,
    private val bookingCreateRepository: BookingCreateRepository,
    private val bookingDeleteRepository: BookingDeleteRepository,
) : BookingsReadUseCase, BookingCreateUseCase, BookingDeleteUseCase {

    override fun invoke(query: BookingsReadUseCase.Query): BookingsReadUseCase.Response {
        val result = bookingsReadRepository(BookingsReadRepository.Request(query.date))
        return BookingsReadUseCase.Response(result.bookings.map { it.accountId })
    }

    override fun invoke(command: BookingCreateUseCase.Command): BookingCreateUseCase.Response {
        bookingCreateRepository(BookingCreateRepository.Request(
            from = command.from,
            to = command.to,
            accountId = command.accountId,
        ))
        return BookingCreateUseCase.Response(
            from = command.from,
            to = command.to,
            accountId = command.accountId,
        )
    }

    override fun invoke(command: BookingDeleteUseCase.Command): BookingDeleteUseCase.Response {
        bookingDeleteRepository(BookingDeleteRepository.Request(
            bookingId = command.bookingId,
            accountId = command.accountId,
        ))
        return BookingDeleteUseCase.Response(
            bookingId = command.bookingId,
            accountId = command.accountId,
        )
    }
}
