package com.zachvlat.footballscores.data.api

import com.zachvlat.footballscores.data.model.LiveScoresResponse
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone

interface ScoresApi {

    @GET("v1/api/app/date/{sport}/{date}/{offsetHours}?locale=en")
    suspend fun getScores(
        @Path("sport") sport: String,
        @Path("date") date: String,
        @Path("offsetHours") offsetHours: Int
    ): LiveScoresResponse
}

fun apiOffsetHours(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3_600_000

fun esdToLocalTime(esd: Long, offsetHours: Int = apiOffsetHours()): String? {
    val text = esd.toString()
    if (text.length != 14) return null
    val parser = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = SimpleTimeZone(offsetHours * 3_600_000, "api")
    }
    val instant = runCatching { parser.parse(text) }.getOrNull() ?: return null
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(instant)
}
