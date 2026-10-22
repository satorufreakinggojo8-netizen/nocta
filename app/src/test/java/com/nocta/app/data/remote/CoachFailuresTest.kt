package com.nocta.app.data.remote

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CoachFailuresTest {

    @Test
    fun `offline and connect failures tell the user their data is safe`() {
        assertEquals(CoachFailures.OFFLINE, CoachFailures.describe(UnknownHostException("dns")))
        assertEquals(CoachFailures.OFFLINE, CoachFailures.describe(ConnectException("refused")))
        assertTrue(CoachFailures.describe(UnknownHostException("dns")).contains("records are still saved"))
    }

    @Test
    fun `timeouts are distinguished from being offline`() {
        assertEquals(CoachFailures.TIMEOUT, CoachFailures.describe(SocketTimeoutException()))
    }

    @Test
    fun `http failures carry the status code without leaking internals`() {
        val serverError = HttpException(Response.error<Any>(503, "".toResponseBody()))
        val message = CoachFailures.describe(serverError)
        assertTrue(message.contains("503"))
        assertTrue(message.contains("temporarily unavailable"))

        val missing = HttpException(Response.error<Any>(404, "".toResponseBody()))
        assertTrue(CoachFailures.describe(missing).contains("404"))
    }

    @Test
    fun `unknown failures fall back to a retryable message`() {
        assertEquals(
            "I couldn't reach the coach right now. Please try again in a moment.",
            CoachFailures.describe(IllegalStateException("boom"))
        )
    }

    @Test
    fun `placeholder base url is treated as unconfigured`() {
        assertFalse(BackendConfig.isConfigured("https://api.nocta.example.com/"))
        assertFalse(BackendConfig.isConfigured(""))
        assertFalse(BackendConfig.isConfigured("api.nocta.example.com"))
        assertTrue(BackendConfig.isConfigured("https://nocta-api.fly.dev/"))
        assertTrue(BackendConfig.isConfigured("http://10.0.2.2:3000/"))
    }
}
