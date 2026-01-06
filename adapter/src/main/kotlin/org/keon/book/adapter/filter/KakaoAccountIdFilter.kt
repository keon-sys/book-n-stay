package org.keon.book.adapter.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.keon.book.adapter.auth.JwtAuthTokenService
import org.keon.book.adapter.config.Properties
import org.keon.book.application.exception.KakaoAuthenticationException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URLEncoder
import java.util.*

@Component
class KakaoAccountIdFilter(
    private val tokenService: JwtAuthTokenService,
    private val securityAuthProperty: Properties.SecurityAuthProperty,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/auth/") ||
            path.startsWith("/api/v1/auth/") ||
            path.startsWith("/component/") ||
            path.endsWith(".js") ||
            path.endsWith(".css") ||
            path.endsWith(".html") ||
            path.endsWith(".svg") ||
            path.endsWith(".png") ||
            path.endsWith(".jpg") ||
            path.endsWith(".ico")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val cookie = cookieToken(request)
            if (cookie != null) {
                val accountId = handleCookieToken(cookie)
                filterChain.doFilter(KakaoAccountHeaderRequestWrapper(request, accountId), response)
            } else {
                return handleMissingAuth(request, response)
            }
        } catch (ex: KakaoAuthenticationException) {
            unauthorized(request, response, ex.message ?: "Unauthorized")
        }
    }

    private fun handleCookieToken(token: String): String {
        val accountId = tokenService.parseAccountId(token)
        return accountId
    }

    private fun cookieToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == securityAuthProperty.cookieName }
            ?.value

    private fun bearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.startsWith("Bearer ", ignoreCase = true)) return null
        return header.removePrefix("Bearer").trim()
    }

    private fun unauthorized(request: HttpServletRequest, response: HttpServletResponse, message: String) {
        val path = request.requestURI
        if (path.startsWith("/api")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"status":401,"error":"Unauthorized","message":"$message","path":"$path"}""")
        } else {
            redirectToLogin(request, response)
        }
    }

    private fun handleMissingAuth(request: HttpServletRequest, response: HttpServletResponse) {
        val path = request.requestURI
        if (path.startsWith("/api")) {
            unauthorized(request, response, "Unauthorized")
        } else {
            redirectToLogin(request, response)
        }
    }

    private fun redirectToLogin(request: HttpServletRequest, response: HttpServletResponse) {
        val target = buildTarget(request)
        val location = "/auth/kakao?redirect=${URLEncoder.encode(target, Charsets.UTF_8)}"
        response.sendRedirect(location)
    }

    private fun buildTarget(request: HttpServletRequest): String {
        val query = request.queryString?.let { "?$it" } ?: ""
        return "${request.requestURI}$query"
    }

    private class KakaoAccountHeaderRequestWrapper(
        request: HttpServletRequest,
        private val accountId: String,
    ) : HttpServletRequestWrapper(request) {

        private val headerName = ACCOUNT_HEADER.lowercase(Locale.getDefault())

        override fun getHeader(name: String?): String? {
            name ?: return null
            return if (name.lowercase(Locale.getDefault()) == headerName) accountId else super.getHeader(name)
        }

        override fun getHeaders(name: String?): Enumeration<String> {
            name ?: return Collections.emptyEnumeration()
            return if (name.lowercase(Locale.getDefault()) == headerName) {
                Collections.enumeration(listOf(accountId))
            } else {
                super.getHeaders(name)
            }
        }

        override fun getHeaderNames(): Enumeration<String> {
            val names = mutableSetOf<String>()
            Collections.list(super.getHeaderNames())
                .filter { it.lowercase(Locale.getDefault()) != headerName }
                .forEach { names.add(it) }
            names.add(ACCOUNT_HEADER)
            return Collections.enumeration(names)
        }
    }

    companion object {
        private const val ACCOUNT_HEADER = "X-Kakao-Account-Id"
    }
}
