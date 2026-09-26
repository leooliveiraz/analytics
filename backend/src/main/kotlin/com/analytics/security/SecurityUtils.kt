package com.analytics.security

import com.analytics.common.exception.UnauthorizedException
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object SecurityUtils {

    fun currentUser(): UserPrincipal {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return principal as? UserPrincipal ?: throw UnauthorizedException("Not authenticated")
    }

    fun currentUserId(): UUID = currentUser().id
}
