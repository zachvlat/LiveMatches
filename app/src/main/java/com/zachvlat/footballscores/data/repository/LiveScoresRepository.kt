package com.zachvlat.footballscores.data.repository

import android.util.Log
import com.zachvlat.footballscores.BuildConfig
import com.zachvlat.footballscores.data.api.LineupsApi
import com.zachvlat.footballscores.data.api.MatchDetailApi
import com.zachvlat.footballscores.data.api.ScoresApi
import com.zachvlat.footballscores.data.api.apiOffsetHours
import com.zachvlat.footballscores.data.model.LineupsResponse
import com.zachvlat.footballscores.data.model.LiveScoresResponse
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LiveScoresRepository {

    private const val TAG = "LiveScoresRepository"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val scoresApi = retrofit("https://prod-cdn-mev-api.livescore.com/").create(ScoresApi::class.java)
    private val publicApi = retrofit("https://prod-cdn-public-api.livescore.com/")
    private val matchDetailApi = publicApi.create(MatchDetailApi::class.java)
    private val lineupsApi = publicApi.create(LineupsApi::class.java)

    suspend fun getScores(sport: String, dateString: String): Result<LiveScoresResponse> = try {
        Result.success(scoresApi.getScores(sport, dateString, apiOffsetHours()).normalizeEvents())
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching $sport scores for date: $dateString", e)
        Result.failure(e)
    }

    suspend fun getMatchDetails(sport: String, matchId: String): Result<MatchDetailResponse> = try {
        Result.success(matchDetailApi.getMatchDetails(sport, matchId))
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching match details for ID: $matchId", e)
        Result.failure(e)
    }

    suspend fun getLineups(sport: String, matchId: String): Result<LineupsResponse> = try {
        Result.success(lineupsApi.getLineups(sport, matchId))
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching lineups for match ID: $matchId", e)
        Result.failure(e)
    }
}
