package com.nocta.app.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns a transport failure into a message that tells the user what actually
 * happened, without leaking internals or pretending Vela answered.
 */
object CoachFailures {

    const val OFFLINE =
        "I can't reach the coach — this device looks offline. Your records are still saved; " +
            "reconnect and hit Retry."
    const val TIMEOUT =
        "The coach took too long to answer. Your records are safe — retry in a moment."
    const val NOT_CONFIGURED =
        "No coach backend is configured in this build, so I can't answer yet. " +
            "Your sleep data stays on this device. Add your server URL to enable Vela."
    const val EMPTY =
        "The coach returned an empty answer. Nothing was saved — please retry."

    fun describe(failure: Throwable): String = when (failure) {
        is UnknownHostException, is ConnectException -> OFFLINE
        is SocketTimeoutException -> TIMEOUT
        is HttpException -> when (failure.code()) {
            in 500..599 -> "The coach service is temporarily unavailable (HTTP ${failure.code()}). Retry shortly."
            401, 403 -> "This device isn't authorised with your coach server (HTTP ${failure.code()}). Sign-in needs to be set up before Vela can answer."
            404 -> "The coach endpoint wasn't found on your server (HTTP 404). The backend may not be deployed yet."
            else -> "The coach request was rejected (HTTP ${failure.code()}). Your records are unchanged."
        }
        is IllegalArgumentException -> "That message couldn't be sent. Try rephrasing it."
        is IOException -> "The connection dropped before the answer finished. Retry to continue."
        else -> "I couldn't reach the coach right now. Please try again in a moment."
    }
}
