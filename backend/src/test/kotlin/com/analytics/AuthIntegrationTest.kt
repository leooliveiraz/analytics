package com.analytics

import com.analytics.auth.dto.TokenResponse
import com.analytics.auth.dto.UserResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class AuthIntegrationTest : AbstractIntegrationTest() {

    @Test
    fun `registers authenticates and reads profile`() {
        val email = "user-${UUID.randomUUID()}@example.com"

        val register = rest.postForEntity(
            "/api/v1/auth/register",
            jsonEntity(mapOf("email" to email, "password" to "secret123", "name" to "Test")),
            TokenResponse::class.java,
        )
        assertEquals(HttpStatus.CREATED, register.statusCode)
        val accessToken = register.body?.accessToken
        assertNotNull(accessToken)

        val me = rest.exchange(
            "/api/v1/auth/me",
            HttpMethod.GET,
            bearerEntity(accessToken!!),
            UserResponse::class.java,
        )
        assertEquals(HttpStatus.OK, me.statusCode)
        assertEquals(email, me.body?.email)

        val login = rest.postForEntity(
            "/api/v1/auth/login",
            jsonEntity(mapOf("email" to email, "password" to "secret123")),
            TokenResponse::class.java,
        )
        assertEquals(HttpStatus.OK, login.statusCode)
        assertNotNull(login.body?.refreshToken)
    }

    @Test
    fun `rejects invalid credentials`() {
        val response = rest.postForEntity(
            "/api/v1/auth/login",
            jsonEntity(mapOf("email" to "nobody@example.com", "password" to "wrongpass")),
            String::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `rejects protected endpoint without token`() {
        val response = rest.exchange("/api/v1/projects", HttpMethod.GET, null, String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
