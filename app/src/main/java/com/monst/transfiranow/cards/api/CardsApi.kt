package com.monst.transfiranow.cards.api

import com.monst.transfiranow.cards.model.CreateCardRequest
import com.monst.transfiranow.cards.model.CreateCardResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CardsApi {
    @POST("cards")
    suspend fun createCard(@Body body: CreateCardRequest): CreateCardResponse

    @GET("cards/{id}")
    suspend fun getCard(@Path("id") id: String): CreateCardResponse
}

