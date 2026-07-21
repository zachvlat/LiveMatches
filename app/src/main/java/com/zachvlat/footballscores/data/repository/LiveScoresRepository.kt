package com.zachvlat.footballscores.data.repository

import android.util.Log
import com.zachvlat.footballscores.data.api.LiveScoresApi
import com.zachvlat.footballscores.data.api.MatchDetailApi
import com.zachvlat.footballscores.data.api.LineupsApi
import com.zachvlat.footballscores.data.api.BasketballApi
import com.zachvlat.footballscores.data.api.CricketApi
import com.zachvlat.footballscores.data.api.HockeyApi
import com.zachvlat.footballscores.data.model.LiveScoresResponse
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import com.zachvlat.footballscores.data.model.LineupsResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LiveScoresRepository {
    
    private val api: LiveScoresApi
    private val basketballApi: BasketballApi
    private val cricketApi: CricketApi
    private val hockeyApi: HockeyApi
    private val matchDetailApi: MatchDetailApi
    private val lineupsApi: LineupsApi
    
    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://prod-cdn-mev-api.livescore.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val matchDetailRetrofit = Retrofit.Builder()
            .baseUrl("https://prod-cdn-public-api.livescore.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(LiveScoresApi::class.java)
        basketballApi = retrofit.create(BasketballApi::class.java)
        cricketApi = retrofit.create(CricketApi::class.java)
        hockeyApi = retrofit.create(HockeyApi::class.java)
        matchDetailApi = matchDetailRetrofit.create(MatchDetailApi::class.java)
        lineupsApi = matchDetailRetrofit.create(LineupsApi::class.java)
    }
    
    suspend fun getTodayLiveScores(): Result<LiveScoresResponse> {
        return try {
            val url = LiveScoresApi.getTodayUrl()
            Log.d("LiveScoresRepository", "Fetching URL: $url")
            val response = api.getLiveScores(url)
            val normalizedResponse = response.normalizeEvents()
            Result.success(normalizedResponse)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching live scores", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLiveScoresForDate(dateString: String): Result<LiveScoresResponse> {
        return try {
            val url = LiveScoresApi.getUrlForDateString(dateString)
            Log.d("LiveScoresRepository", "Fetching URL: $url")
            val response = api.getLiveScores(url)
            val normalizedResponse = response.normalizeEvents()
            Result.success(normalizedResponse)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching live scores for date: $dateString", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTodayBasketballScores(): Result<LiveScoresResponse> {
        return try {
            val url = BasketballApi.getTodayUrl()
            Log.d("LiveScoresRepository", "Fetching basketball URL: $url")
            val response = basketballApi.getBasketballScores(url)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching basketball scores", e)
            Result.failure(e)
        }
    }
    
    suspend fun getBasketballScoresForDate(dateString: String): Result<LiveScoresResponse> {
        return try {
            val url = BasketballApi.getUrlForDateString(dateString)
            Log.d("LiveScoresRepository", "Fetching basketball URL: $url")
            val response = basketballApi.getBasketballScores(url)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching basketball scores for date: $dateString", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTodayCricketScores(): Result<LiveScoresResponse> {
        return try {
            val url = CricketApi.getTodayUrl()
            Log.d("LiveScoresRepository", "Fetching cricket URL: $url")
            val response = cricketApi.getCricketScores(url)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching cricket scores", e)
            Result.failure(e)
        }
    }
    
    suspend fun getCricketScoresForDate(dateString: String): Result<LiveScoresResponse> {
        return try {
            val url = CricketApi.getUrlForDateString(dateString)
            Log.d("LiveScoresRepository", "Fetching cricket URL: $url")
            val response = cricketApi.getCricketScores(url)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching cricket scores for date: $dateString", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTodayHockeyScores(): Result<LiveScoresResponse> {
        return try {
            val url = HockeyApi.getTodayUrl()
            Log.d("LiveScoresRepository", "Fetching hockey URL: $url")
            val response = hockeyApi.getHockeyScores(url)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching hockey scores", e)
            Result.failure(e)
        }
    }
    
    suspend fun getHockeyScoresForDate(dateString: String): Result<LiveScoresResponse> {
        return try {
            val url = HockeyApi.getUrlForDateString(dateString)
            Log.d("LiveScoresRepository", "Fetching hockey URL: $url")
            val response = hockeyApi.getHockeyScores(url)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching hockey scores for date: $dateString", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMatchDetails(matchId: String): Result<MatchDetailResponse> {
        return try {
            Log.d("LiveScoresRepository", "Fetching match details for ID: $matchId")
            val response = matchDetailApi.getMatchDetails(matchId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching match details for ID: $matchId", e)
            Result.failure(e)
        }
    }

    suspend fun getLineups(matchId: String): Result<LineupsResponse> {
        return try {
            Log.d("LiveScoresRepository", "Fetching lineups for match ID: $matchId")
            val response = lineupsApi.getLineups(matchId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("LiveScoresRepository", "Error fetching lineups for match ID: $matchId", e)
            Result.failure(e)
        }
    }
}