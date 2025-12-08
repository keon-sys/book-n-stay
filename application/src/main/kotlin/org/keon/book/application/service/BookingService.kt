package org.keon.book.application.service

import org.keon.book.application.port.inbound.BookingCreateUseCase
import org.keon.book.application.port.inbound.BookingDeleteUseCase
import org.keon.book.application.port.inbound.BookingsReadUseCase
import org.keon.book.application.port.inbound.MyBookingsReadUseCase
import org.keon.book.application.port.outbound.*
import org.springframework.stereotype.Service

@Service
class BookingService(
    private val bookingsReadRepository: BookingsReadRepository,
    private val myBookingsReadRepository: MyBookingsReadRepository,
    private val bookingCreateRepository: BookingCreateRepository,
    private val bookingDeleteRepository: BookingDeleteRepository,
    private val kakaoUserReadRepository: KakaoUserReadRepository,
) : BookingsReadUseCase, MyBookingsReadUseCase, BookingCreateUseCase, BookingDeleteUseCase {

    override fun invoke(query: BookingsReadUseCase.Query): BookingsReadUseCase.Response {
        val result = bookingsReadRepository(BookingsReadRepository.Request(query.date))
        val bookings = result.bookings.map { booking ->
            BookingsReadUseCase.BookingInfo(
                bookingId = booking.id,
                from = booking.from,
                to = booking.to,
                nickname = booking.nickname,
            )
        }
        return BookingsReadUseCase.Response(bookings)
    }

    override fun invoke(query: MyBookingsReadUseCase.Query): MyBookingsReadUseCase.Response {
        val result = myBookingsReadRepository(MyBookingsReadRepository.Request(query.accountId))
        val bookings = result.bookings.map { booking ->
            MyBookingsReadUseCase.BookingInfo(
                bookingId = booking.id,
                from = booking.from,
                to = booking.to,
                nickname = booking.nickname,
            )
        }
        return MyBookingsReadUseCase.Response(bookings)
    }

    override fun invoke(command: BookingCreateUseCase.Command): BookingCreateUseCase.Response {
        val userInfo = kakaoUserReadRepository(
            KakaoUserReadRepository.Request.AccountId(command.accountId)
        )
        val nickname = userInfo.nickname ?: throw IllegalStateException("User nickname not found")

        val result = bookingCreateRepository(BookingCreateRepository.Request(
            from = command.from,
            to = command.to,
            accountId = command.accountId,
            nickname = nickname,
        ))
        return BookingCreateUseCase.Response(
            bookingId = result.id,
            from = result.from,
            to = result.to,
            nickname = result.nickname,
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
