package com.monst.transfiranow.util

data class ParsedVCard(
    val fullName: String = "",
    val title: String = "",
    val phone: String = "",
    val email: String = "",
    val url: String = "",
    val note: String = ""
)

object VCardParser {
    fun parse(raw: String): ParsedVCard? {
        val cleaned = raw.trim()
        if (!cleaned.contains("BEGIN:VCARD", ignoreCase = true)) return null

        val unfolded = unfoldLines(cleaned)
        var fullName = ""
        var title = ""
        var phone = ""
        var email = ""
        var url = ""
        var note = ""

        for (line in unfolded) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            val keyPart = line.substring(0, idx).trim()
            val valuePart = unescape(line.substring(idx + 1).trim())

            val key = keyPart.substringBefore(';').uppercase()
            when {
                key == "FN" && fullName.isBlank() -> fullName = valuePart
                key == "TITLE" && title.isBlank() -> title = valuePart
                key == "TEL" && phone.isBlank() -> phone = valuePart
                key == "EMAIL" && email.isBlank() -> email = valuePart
                key == "URL" && url.isBlank() -> url = valuePart
                key == "NOTE" && note.isBlank() -> note = valuePart
            }
        }

        return ParsedVCard(
            fullName = fullName,
            title = title,
            phone = phone,
            email = email,
            url = url,
            note = note
        )
    }

    private fun unfoldLines(raw: String): List<String> {
        val lines = raw.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    result[result.lastIndex] = result.last() + line.trimStart()
                }
            } else if (line.isNotBlank()) {
                result.add(line.trim())
            }
        }
        return result
    }

    private fun unescape(value: String): String {
        return value
            .replace("\\n", "\n", ignoreCase = true)
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}

