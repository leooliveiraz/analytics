package com.analytics.auth

import com.analytics.auth.dto.LoginRequest
import com.analytics.auth.dto.RefreshRequest
import com.analytics.auth.dto.RegisterRequest
import com.analytics.auth.dto.TokenResponse
import com.analytics.auth.dto.UserResponse
import com.analytics.security.SecurityUtils
import com.analytics.user.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): TokenResponse = authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse = authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenResponse = authService.refresh(request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout() {
        authService.logout(SecurityUtils.currentUserId())
    }

    @GetMapping("/me")
    fun me(): UserResponse {
        val user = userService.findById(SecurityUtils.currentUserId())
        return UserResponse(
            id = requireNotNull(user.id),
            email = user.email,
            name = user.name,
            createdAt = user.createdAt,
        )
    }
}
