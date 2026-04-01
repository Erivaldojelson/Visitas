package com.monst.transfiranow.wallet

import com.monst.transfiranow.data.VisitingCard
import org.json.JSONArray
import org.json.JSONObject

object WalletPassBuilder {
    fun buildUnsignedPayload(
        issuerId: String,
        classSuffix: String,
        card: VisitingCard
    ): JSONObject {
        val classId = "$issuerId.$classSuffix"
        val objectId = "$issuerId.card_${card.id.replace("-", "_")}"

        val links = JSONArray().apply {
            listOfNotNull(
                link("Website", card.website),
                link("Instagram", normalizeUrl(card.instagram, "https://instagram.com/")),
                link("LinkedIn", normalizeUrl(card.linkedin, "https://linkedin.com/in/")),
                link("Phone", phoneUrl(card.phone)),
                link("Email", emailUrl(card.email))
            ).forEach { put(it) }
        }

        val genericClass = JSONObject()
            .put("id", classId)
            .put("classTemplateInfo", JSONObject())
            .put("reviewStatus", "UNDER_REVIEW")
            .put("cardTitle", localized("Visitas"))

        val genericObject = JSONObject()
            .put("id", objectId)
            .put("classId", classId)
            .put("hexBackgroundColor", card.passColor)
            .put("cardTitle", localized(card.name))
            .put("subheader", localized(card.role.ifBlank { "Cartão pessoal" }))
            .put("header", localized(card.phone.ifBlank { card.email.ifBlank { "Contato" } }))
            .put("textModulesData", JSONArray().apply {
                listOfNotNull(
                    textModule("Telefone", card.phone),
                    textModule("Email", card.email),
                    textModule("Instagram", card.instagram),
                    textModule("LinkedIn", card.linkedin),
                    textModule("URL", card.website),
                    textModule("Nota", card.note)
                ).forEach { put(it) }
            })
            .put("linksModuleData", JSONObject().put("uris", links))

        image(card.walletPhotoUrl)?.let { image ->
            genericObject.put("heroImage", image)
            genericObject.put("logo", image)
        }

        return JSONObject()
            .put("iss", "backend-signs-this")
            .put("aud", "google")
            .put("typ", "savetowallet")
            .put("payload", JSONObject()
                .put("genericClasses", JSONArray().put(genericClass))
                .put("genericObjects", JSONArray().put(genericObject)))
    }

    private fun localized(value: String) = JSONObject()
        .put("defaultValue", JSONObject()
            .put("language", "pt-BR")
            .put("value", value))

    private fun textModule(header: String, body: String): JSONObject? {
        if (body.isBlank()) return null
        return JSONObject().put("header", header).put("body", body)
    }

    private fun link(label: String, url: String): JSONObject? {
        if (url.isBlank()) return null
        return JSONObject()
            .put("description", localized(label))
            .put("uri", url)
    }

    private fun image(url: String): JSONObject? {
        if (url.isBlank()) return null
        return JSONObject().put(
            "sourceUri",
            JSONObject().put("uri", url)
        )
    }

    private fun normalizeUrl(handleOrUrl: String, prefix: String): String {
        if (handleOrUrl.isBlank()) return ""
        return if (handleOrUrl.startsWith("http")) handleOrUrl else prefix + handleOrUrl.trimStart('@')
    }

    private fun phoneUrl(value: String): String = if (value.isBlank()) "" else "tel:$value"
    private fun emailUrl(value: String): String = if (value.isBlank()) "" else "mailto:$value"
}
