package org.keon.book.adapter.inbound.common.handler

import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(
    basePackages = [
        "org.keon.book.adapter.inbound.web"
    ]
)
class ViewControllerExceptionHandler {
}