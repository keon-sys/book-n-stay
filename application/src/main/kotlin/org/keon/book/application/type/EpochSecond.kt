package org.keon.book.application.type

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@JvmInline
value class EpochSecond(val value: Long) {

    fun toZonedDateTime(): ZonedDateTime =
        Instant.ofEpochSecond(value).atZone(ZoneOffset.UTC)

    fun toSeoulZonedDateTime(): ZonedDateTime =
        Instant.ofEpochSecond(value).atZone(ZoneId.of("Asia/Seoul"))

    operator fun plus(seconds: Long): EpochSecond =
        EpochSecond(value + seconds)

    operator fun minus(seconds: Long): EpochSecond =
        EpochSecond(value - seconds)

    companion object {
        fun from(zonedDateTime: ZonedDateTime): EpochSecond =
            EpochSecond(zonedDateTime.toEpochSecond())

        fun now(): EpochSecond =
            EpochSecond(Instant.now().epochSecond)
    }
}
