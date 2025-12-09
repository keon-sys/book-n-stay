package org.keon.book.application.exception

class DuplicateUserBookingException(
    val date: Long,
) : RuntimeException("이미 해당 날짜에 예약이 존재합니다. (날짜: $date)")
