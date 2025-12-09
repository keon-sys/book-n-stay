package org.keon.book.application.exception

class PastBookingDeletionException(
    val bookingId: Long,
    val date: Long,
) : RuntimeException("과거 예약은 삭제할 수 없습니다. (예약 ID: $bookingId, 날짜: $date)")
