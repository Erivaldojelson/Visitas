package com.monst.transfiranow.cards.model

data class CreateCardRequest(
    val name: String,
    val photo: String,
    val instagram: String? = null,
    val whatsapp: String? = null,
    val website: String? = null
)

data class CreateCardResponse(
    val id: String,
    val name: String,
    val photo: String,
    val instagram: String? = null,
    val whatsapp: String? = null,
    val website: String? = null,
    val qrCode: String
)

