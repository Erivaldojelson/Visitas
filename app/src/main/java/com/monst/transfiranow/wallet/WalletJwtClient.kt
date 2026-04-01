package com.monst.transfiranow.wallet

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class WalletJwtClient {
    fun fetchSignedJwt(endpoint: String, payload: JSONObject): Result<String> {
        return runCatching {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            val body = BufferedReader(
                if (connection.responseCode in 200..299) {
                    connection.inputStream.reader()
                } else {
                    connection.errorStream?.reader() ?: throw IllegalStateException("Falha ao assinar JWT.")
                }
            ).use { it.readText() }

            parseJwt(body)
        }
    }

    private fun parseJwt(body: String): String {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) return trimmed
        val json = JSONObject(trimmed)
        return json.optString("jwt").ifBlank {
            throw IllegalStateException("Resposta do backend não contém o campo jwt.")
        }
    }
}
