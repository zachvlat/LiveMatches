package com.zachvlat.footballscores

import com.google.gson.Gson
import com.zachvlat.footballscores.data.api.apiOffsetHours
import com.zachvlat.footballscores.data.api.esdToLocalTime
import com.zachvlat.footballscores.data.model.IncidentKind
import com.zachvlat.footballscores.data.model.LineupsResponse
import com.zachvlat.footballscores.data.model.MatchIncidentGroup
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import com.zachvlat.footballscores.data.model.isLiveStatus
import com.zachvlat.footballscores.data.model.teamImageUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class LogicTest {

    @Test
    fun isLiveStatus_codes() {
        val cases = mapOf(
            "45'" to true, "45+2'" to true, "HT" to true, "2H" to true, "ht" to true,
            "2Q" to true, "1E" to true, "OT" to true, "LIVE" to true,
            "FT" to false, "NS" to false, "AET" to false, "Postp." to false, null to false
        )
        cases.forEach { (eps, live) -> assertEquals("Eps=$eps", live, isLiveStatus(0, eps)) }
        assertEquals(true, isLiveStatus(33, "FT"))
    }

    @Test
    fun kickoffTime_matchesThePhoneClockAllYear() {
        withZone("Europe/Athens") {
            assertEquals("14:30", esdToLocalTime(20261107143000, offsetHours = 2))
        }
        withZone("America/New_York") {
            assertEquals("07:30", esdToLocalTime(20261107073000, offsetHours = -5))
        }
        withZone("Asia/Kolkata") {
            assertEquals(5, apiOffsetHours())
            assertEquals("18:00", esdToLocalTime(20261107173000))
        }
        assertEquals(null, esdToLocalTime(123))
    }

    private fun withZone(id: String, block: () -> Unit) {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun incidentCodes_portoNapoli() {
        val json = """[
            {"Min":80,"Nm":1,"Sc":[3,1],"Sor":3,"Incs":[
              {"Min":80,"Nm":1,"PosA":2,"Aid":"17161","ID":"17161","Pn":"Jakub Kiwior","IT":36,"Sc":[3,1],"Sor":3},
              {"Min":80,"Nm":1,"PosA":5,"Aid":"42501","ID":"42501","Pn":"In-Beom Hwang","IT":63,"Sc":[3,1],"Sor":3}]},
            {"Min":89,"Nm":2,"PosA":2,"Aid":"9148","ID":"9148","Pn":"Rafa Marin","IT":45,"NoP":false,"Sor":1},
            {"Min":90,"Nm":1,"PosA":5,"Aid":"96313","ID":"96313","Pn":"Samuel Aghehowa","IT":37,"Sc":[4,1],"Sor":0}]"""
        val groups = Gson().fromJson(json, Array<MatchIncidentGroup>::class.java)

        assertEquals(IncidentKind.GOAL, IncidentKind.of(groups[0].Incs!!.first().IT))
        assertEquals(IncidentKind.RED_CARD, IncidentKind.of(groups[1].IT))
        val aghehowa = IncidentKind.of(groups[2].IT)!!
        assertEquals(IncidentKind.PENALTY_GOAL, aghehowa)
        assertEquals(true, aghehowa.isGoal)
        assertEquals(false, IncidentKind.MISSED_PENALTY.isGoal)
    }

    @Test
    fun teamImageUrl_everyImgFormatGetsTheHost() {
        val host = "https://storage.livescore.com/images/team/high/"
        assertEquals("${host}enet/8564.png", teamImageUrl("enet/8564.png"))
        assertEquals("${host}teambadge/marseille-2026.png", teamImageUrl("teambadge/marseille-2026.png"))
        assertEquals("${host}teambadge/afghanistan---cricket.png", teamImageUrl("teambadge/afghanistan---cricket.png"))
        assertEquals("https://x.com/a.png", teamImageUrl("https://x.com/a.png"))
        assertEquals(null, teamImageUrl(""))
        assertEquals(null, teamImageUrl(null))
    }

    @Test
    fun basketballDetails_parseWithFootballModels() {
        val scoreboard = """{"Eid":"1868337","Tr1":"120","Tr2":"113","Tr1OT":"16","Tr2OT":"9",
            "Tr1Q1":"24","Tr2Q1":"24","Tr1Q2":"18","Tr2Q2":"13","Tr1Q3":"28","Tr2Q3":"43","Tr1Q4":"34","Tr2Q4":"24",
            "T1":[{"Nm":"Pistons"}],"T2":[{"Nm":"Heat"}],"Eps":"OT"}"""
        val detail = Gson().fromJson(scoreboard, MatchDetailResponse::class.java)
        assertEquals(listOf("Q1", "Q2", "Q3", "Q4", "OT"), detail.periodScores().map { it.first })
        assertEquals(Triple("OT", "16", "9"), detail.periodScores().last())

        val lineups = """{"Eid":"1868337","Lu":[{"Tnb":1,"Ps":[
            {"Pid":"1","Fn":"Cade","Ln":"Cunningham","Pos":14,"Pon":"STARTER","Snu":2,"PosA":14},
            {"Pid":"2","Fn":"Dean","Ln":"Wade","Pos":5,"Pon":"Forward","Snu":45,"PosA":5,"PosS":4},
            {"Pid":"3","Fn":"Nick","Ln":"Nurse","Pos":10,"Pon":"COACH","PosA":10}]}]}"""
        val players = Gson().fromJson(lineups, LineupsResponse::class.java).Lu!!.single().Ps
        assertEquals(listOf("Cade Cunningham"), players.filter { it.isStarting() }.map { it.getFullName() })
        assertEquals("Nick Nurse", players.single { it.isCoach() }.getFullName())
    }

    @Test
    fun datePickerMillis_keepTheDayWestOfUtc() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val nov5UtcMidnight = 1793836800000L
            assertEquals("20261105", utcMillisToDateString(nov5UtcMidnight))
            assertEquals(nov5UtcMidnight, dateStringToUtcMillis("20261105"))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
