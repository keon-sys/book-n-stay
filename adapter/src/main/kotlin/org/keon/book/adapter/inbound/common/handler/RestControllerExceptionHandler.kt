package org.keon.book.adapter.inbound.common.handler

import org.springframework.web.bind.annotation.ControllerAdvice

@ControllerAdvice(
    basePackages = [
        "org.keon.book.adapter.inbound.view"
    ]
)
class RestControllerExceptionHandler {
}