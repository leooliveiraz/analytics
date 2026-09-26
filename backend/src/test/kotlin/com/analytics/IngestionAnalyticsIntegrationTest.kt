package com.analytics

import com.analytics.analytics.dto.BreakdownResponse
import com.analytics.analytics.dto.OverviewResponse
import com.analytics.auth.dto.TokenResponse
import com.analytics.ingestion.dto.IngestResponse
import com.analytics.project.dto.ProjectResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDate
import java.util.UUID

class IngestionAnalyticsIntegrationTest : AbstractIntegrationTest() {

    @Test
    fun `ingests events and serves analytics`() {
        val token = createUserToken()
        val project = rest.exchange(
            "/api/v1/projects",
            HttpMethod.POST,
            jsonEntity(mapOf("name" to "Site Teste", "timezone" to "UTC"), token),
            ProjectResponse::class.java,
        ).body!!

        val ingestBody = mapOf(
            "events" to listOf(
                mapOf(
                    "name" to "pageview",
                    "url" to "https://site.com/",
                    "visitorId" to "v1",
                    "sessionId" to UUID.randomUUID().toString(),
                ),
                mapOf(
                    "name" to "pageview",
                    "url" to "https://site.com/precos",
                    "visitorId" to "v1",
                    "sessionId" to UUID.randomUUID().toString(),
                ),
                mapOf(
                    "name" to "signup",
                    "url" to "https://site.com/precos",
                    "visitorId" to "v2",
                    "sessionId" to UUID.randomUUID().toString(),
                    "properties" to mapOf("plan" to "pro"),
                ),
            ),
        )
        val ingest = rest.postForEntity(
            "/api/v1/events",
            ingestEntity(project.publicKey, ingestBody),
            IngestResponse::class.java,
        )
        assertEquals(HttpStatus.ACCEPTED, ingest.statusCode)
        assertEquals(3, ingest.body?.accepted)

        val today = LocalDate.now().toString()
        val overview = get(
            "/api/v1/projects/${project.id}/overview?from=$today&to=$today",
            token,
            OverviewResponse::class.java,
        )
        assertEquals(2L, overview.visitors)
        assertEquals(2L, overview.pageviews)

        val breakdown = get(
            "/api/v1/projects/${project.id}/breakdown?from=$today&to=$today&dimension=path",
            token,
            BreakdownResponse::class.java,
        )
        assertTrue(breakdown.items.any { it.value == "/precos" })

        val events = rest.exchange(
            "/api/v1/projects/${project.id}/events?size=10",
            HttpMethod.GET,
            bearerEntity(token),
            String::class.java,
        )
        assertEquals(HttpStatus.OK, events.statusCode)
        assertTrue(events.body!!.contains("signup"))
    }

    @Test
    fun `rejects ingestion with invalid key`() {
        val response = rest.postForEntity(
            "/api/v1/events",
            ingestEntity("pk_invalid", mapOf("events" to listOf(mapOf("name" to "pageview")))),
            String::class.java,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    private fun createUserToken(): String {
        val email = "owner-${UUID.randomUUID()}@example.com"
        return rest.postForEntity(
            "/api/v1/auth/register",
            jsonEntity(mapOf("email" to email, "password" to "secret123")),
            TokenResponse::class.java,
        ).body!!.accessToken
    }

    private fun ingestEntity(apiKey: String, body: Any): HttpEntity<Any> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("X-Api-Key", apiKey)
        return HttpEntity(body, headers)
    }

    private fun <T> get(url: String, token: String, responseType: Class<T>): T {
        val response = rest.exchange(url, HttpMethod.GET, bearerEntity(token), responseType)
        assertEquals(HttpStatus.OK, response.statusCode)
        return response.body!!
    }
}
