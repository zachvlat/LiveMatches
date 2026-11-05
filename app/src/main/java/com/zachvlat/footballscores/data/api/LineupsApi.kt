package com.zachvlat.footballscores.data.api

import com.zachvlat.footballscores.data.model.LineupsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface LineupsApi {

    @GET("v1/api/app/lineups/{sport}/{matchId}?locale=en")
    suspend fun getLineups(@Path("sport") sport: String, @Path("matchId") matchId: String): LineupsResponse
}
