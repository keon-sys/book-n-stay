package org.keon.book.adapter.inbound.common.handler

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class GlobalErrorController : ErrorController {

    private val logger = LoggerFactory.getLogger(javaClass)

    @RequestMapping("/error")
    fun handle(request: HttpServletRequest): ResponseEntity<*> {
        val statusCode = resolveStatus(request)
        val path = (request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) as? String)
            ?: request.requestURI

        return if (path.startsWith("/api")) {
            ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "status" to statusCode.value(),
                        "error" to statusCode.reasonPhrase,
                        "message" to resolveMessage(request, statusCode),
                        "path" to path,
                    ),
                )
        } else {
            renderErrorPage(statusCode)
        }
    }

    private fun renderErrorPage(status: HttpStatus): ResponseEntity<Resource> {
        val resource = errorPageResource(status)
        return if (resource.exists()) {
            ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(resource)
        } else {
            logger.warn("Missing error page for status {}", status.value())
            ResponseEntity.status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .body(null)
        }
    }

    private fun errorPageResource(status: HttpStatus): Resource {
        val pageName = when {
            status.is4xxClientError -> status.value().toString()
            else -> "500"
        }
        return ClassPathResource("static/error/$pageName.html")
    }

    private fun resolveStatus(request: HttpServletRequest): HttpStatus {
        val code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int
        return HttpStatus.resolve(code ?: 0) ?: HttpStatus.INTERNAL_SERVER_ERROR
    }

    private fun resolveMessage(request: HttpServletRequest, status: HttpStatus): String {
        val message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE) as? String
        if (!message.isNullOrBlank()) return message
        return when {
            status.is4xxClientError -> "요청이 올바르지 않습니다."
            status.is5xxServerError -> "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            else -> status.reasonPhrase
        }
    }
}
