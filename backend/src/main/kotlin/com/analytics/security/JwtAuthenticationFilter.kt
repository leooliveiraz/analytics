package com.analytics.security

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith(BEARER_PREFIX) && SecurityContextHolder.getContext().authentication == null) {
            val token = header.substring(BEARER_PREFIX.length)
            try {
                val claims = jwtService.parse(token)
                if (claims[JwtService.CLAIM_TYPE] == JwtService.TYPE_ACCESS) {
                    val principal = UserPrincipal(
                        id = UUID.fromString(claims.subject),
                        email = claims[JwtService.CLAIM_EMAIL] as String,
                    )
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (_: JwtException) {
                SecurityContextHolder.clearContext()
            } catch (_: IllegalArgumentException) {
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
