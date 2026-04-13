package com.monst.transfiranow.wallet

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class IssuedWalletPass(
    val url: String,
    val jwt: String
)

class WalletSaveUrlClient {
    fun fetchIssuedPass(endpoint: String, payload: JSONObject): Result<IssuedWalletPass> {
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
                    connection.errorStream?.reader() ?: throw IllegalStateException("Falha ao gerar passe do Wallet.")
                }
            ).use { it.readText() }

            parseIssuedPass(body)
        }
    }

    private fun parseIssuedPass(body: String): IssuedWalletPass {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) {
            val jwt = trimmed.substringAfterLast("/save/", missingDelimiterValue = "")
            if (jwt.isBlank()) throw IllegalStateException("Resposta do backend inválida para o Google Wallet.")
            return IssuedWalletPass(url = trimmed, jwt = jwt)
        }

        val json = JSONObject(trimmed)
        val url = json.optString("url").trim()
        val jwt = json.optString("jwt").trim().ifBlank {
            url.substringAfterLast("/save/", missingDelimiterValue = "")
        }

        if (url.isNotBlank() && jwt.isNotBlank()) {
            return IssuedWalletPass(url = url, jwt = jwt)
        }

        json.optString("error").takeIf { it.isNotBlank() }?.let { message ->
            throw IllegalStateException(message)
        }

        throw IllegalStateException("Resposta do backend não contém url/jwt do Google Wallet.")
    }
}

