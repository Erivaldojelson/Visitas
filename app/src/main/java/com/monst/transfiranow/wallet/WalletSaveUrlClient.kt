package com.monst.transfiranow.wallet

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class WalletSaveUrlClient {
    fun fetchSaveUrl(endpoint: String, payload: JSONObject): Result<String> {
        return runCatching {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            val body = BufferedReader(
                if (connection.responseCode in 200..299) {
                    connection.inputStream.reader()
                } else {
                    connection.errorStream?.reader() ?: throw IllegalStateException("Falha ao gerar link do Wallet.")
                }
            ).use { it.readText() }

            parseUrl(body)
        }
    }

    private fun parseUrl(body: String): String {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) return trimmed

        val json = JSONObject(trimmed)
        json.optString("url").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("jwt").takeIf { it.isNotBlank() }?.let { jwt ->
            return "https://pay.google.com/gp/v/save/$jwt"
        }
        json.optString("error").takeIf { it.isNotBlank() }?.let { message ->
            throw IllegalStateException(message)
        }

        throw IllegalStateException("Resposta do backend não contém o campo url.")
    }
}

