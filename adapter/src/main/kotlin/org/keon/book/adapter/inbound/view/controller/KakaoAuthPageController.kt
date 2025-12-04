package org.keon.book.adapter.inbound.view.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class KakaoAuthPageController {

    @GetMapping("/auth/kakao", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun showAuthPage(): Resource = ClassPathResource("static/auth/kakao.html")
}
