package org.keon.book.application.exception

import org.keon.book.application.type.EpochSecond
import java.time.format.DateTimeFormatter

class DuplicateUserBookingException(
    val date: Long,
) : RuntimeException("이미 해당 날짜에 예약이 존재합니다. (날짜: ${formatDate(date)})") {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        private fun formatDate(epochSecond: Long): String =
            EpochSecond(epochSecond).toSeoulZonedDateTime().format(DATE_FORMATTER)
    }
}
