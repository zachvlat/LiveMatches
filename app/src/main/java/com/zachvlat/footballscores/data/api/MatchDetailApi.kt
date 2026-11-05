package com.zachvlat.footballscores.data.api

import com.zachvlat.footballscores.data.model.MatchDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MatchDetailApi {

    @GET("v1/api/app/scoreboard/{sport}/{matchId}?locale=en")
    suspend fun getMatchDetails(@Path("sport") sport: String, @Path("matchId") matchId: String): MatchDetailResponse
}
