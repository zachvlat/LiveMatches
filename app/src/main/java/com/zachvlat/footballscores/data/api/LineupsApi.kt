package com.zachvlat.footballscores.data.api

import com.zachvlat.footballscores.data.model.LineupsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface LineupsApi {

    @GET("v1/api/app/lineups/soccer/{matchId}?locale=en")
    suspend fun getLineups(@Path("matchId") matchId: String): LineupsResponse
}
