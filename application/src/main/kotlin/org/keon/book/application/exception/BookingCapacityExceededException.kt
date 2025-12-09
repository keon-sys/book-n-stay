package org.keon.book.application.exception

class BookingCapacityExceededException(
    val date: Long,
    val currentCount: Int,
    val maxCapacity: Int,
) : RuntimeException("해당 날짜의 예약 인원이 초과되었습니다. (날짜: $date, 현재: $currentCount, 최대: $maxCapacity)")
