package org.keon.book.application.service

import org.keon.book.application.port.inbound.BookingUseCase
import org.keon.book.application.port.outbound.BookingRepository
import org.keon.book.domain.booking.Booking
import org.springframework.stereotype.Service

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
) : BookingUseCase {

    override fun getBookings(query: BookingUseCase.BookingsQuery): BookingUseCase.BookingsResponse {
        val bookings = bookingRepository.findByDate(query.date)
        return BookingUseCase.BookingsResponse(bookings.map { it.accountId })
    }

    override fun setBooking(command: BookingUseCase.BookingCommand) {
        bookingRepository.save(Booking(date = command.date, accountId = command.accountId))
    }

    override fun removeBooking(command: BookingUseCase.BookingCommand) {
        bookingRepository.delete(Booking(date = command.date, accountId = command.accountId))
    }
}
