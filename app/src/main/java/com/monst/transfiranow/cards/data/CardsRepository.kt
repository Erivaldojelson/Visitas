package com.monst.transfiranow.cards.data

import com.monst.transfiranow.cards.api.CardsApi
import com.monst.transfiranow.cards.model.CreateCardRequest
import com.monst.transfiranow.cards.model.CreateCardResponse

class CardsRepository(private val api: CardsApi) {
    suspend fun createCard(body: CreateCardRequest): CreateCardResponse = api.createCard(body)
}

