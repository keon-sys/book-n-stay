package org.keon.book.adapter.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Profile("dev")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AccessLogFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)
        val startTime = System.currentTimeMillis()

        try {
            logRequest(wrappedRequest, startTime)
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logResponse(wrappedRequest, wrappedResponse, duration)
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun logRequest(request: ContentCachingRequestWrapper, timestamp: Long) {
        val logBuilder = StringBuilder("\n")
        logBuilder.append("========== REQUEST ==========\n")
        logBuilder.append("Timestamp  : ${formatTimestamp(timestamp)}\n")
        logBuilder.append("Method     : ${request.method}\n")
        logBuilder.append("URI        : ${request.requestURI}\n")

        val queryString = request.queryString
        if (!queryString.isNullOrBlank()) {
            logBuilder.append("Query      : $queryString\n")
        }

        logBuilder.append("Remote     : ${request.remoteAddr}\n")
        logBuilder.append("Headers    :\n")

        val headers = extractHeaders(request)
        headers.forEach { (name, value) ->
            logBuilder.append("  $name: $value\n")
        }

        val contentType = request.contentType
        if (contentType != null && !contentType.startsWith("multipart/")) {
            val body = getRequestBody(request)
            if (body.isNotBlank()) {
                logBuilder.append("Body       :\n")
                logBuilder.append("  $body\n")
            }
        }

        logBuilder.append("=============================")
        log.info(logBuilder.toString())
    }

    private fun logResponse(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        duration: Long,
    ) {
        val logBuilder = StringBuilder("\n")
        logBuilder.append("========== RESPONSE ==========\n")
        logBuilder.append("Method     : ${request.method}\n")
        logBuilder.append("URI        : ${request.requestURI}\n")
        logBuilder.append("Status     : ${response.status}\n")
        logBuilder.append("Duration   : ${duration}ms\n")

        logBuilder.append("Headers    :\n")
        response.headerNames.forEach { name ->
            val values = response.getHeaders(name)
            values.forEach { value ->
                logBuilder.append("  $name: $value\n")
            }
        }

        val contentType = response.contentType
        if (contentType != null && (contentType.contains("application/json") ||
                                     contentType.contains("application/xml") ||
                                     contentType.contains("text/"))) {
            val body = getResponseBody(response)
            if (body.isNotBlank()) {
                logBuilder.append("Body       :\n")
                logBuilder.append("  $body\n")
            }
        }

        logBuilder.append("==============================")
        log.info(logBuilder.toString())
    }

    private fun extractHeaders(request: HttpServletRequest): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val headerNames = request.headerNames

        val relevantHeaders = listOf(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            HttpHeaders.USER_AGENT,
            HttpHeaders.ORIGIN,
            HttpHeaders.REFERER,
            "X-Kakao-Account-Id",
            "Cookie"
        )

        while (headerNames.hasMoreElements()) {
            val name = headerNames.nextElement()
            if (relevantHeaders.any { it.equals(name, ignoreCase = true) }) {
                val value = if (name.equals("Cookie", ignoreCase = true) ||
                               name.equals(HttpHeaders.AUTHORIZATION, ignoreCase = true)) {
                    maskSensitiveValue(request.getHeader(name))
                } else {
                    request.getHeader(name)
                }
                headers[name] = value
            }
        }

        return headers
    }

    private fun maskSensitiveValue(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return if (value.length <= 10) {
            "***masked***"
        } else {
            "${value.take(10)}...***masked***"
        }
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        val content = request.contentAsByteArray
        if (content.isEmpty()) return ""

        return try {
            val body = String(content, Charsets.UTF_8)
            if (body.length > MAX_BODY_LENGTH) {
                "${body.take(MAX_BODY_LENGTH)}... (truncated)"
            } else {
                body
            }
        } catch (e: Exception) {
            "[Unable to parse body]"
        }
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String {
        val content = response.contentAsByteArray
        if (content.isEmpty()) return ""

        return try {
            val body = String(content, Charsets.UTF_8)
            if (body.length > MAX_BODY_LENGTH) {
                "${body.take(MAX_BODY_LENGTH)}... (truncated)"
            } else {
                body
            }
        } catch (e: Exception) {
            "[Unable to parse body]"
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    }

    companion object {
        private const val MAX_BODY_LENGTH = 1000
    }
}
