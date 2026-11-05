package com.zachvlat.footballscores

import com.google.gson.Gson
import com.zachvlat.footballscores.data.model.IncidentKind
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import com.zachvlat.footballscores.notifications.LiveSnapshot
import com.zachvlat.footballscores.notifications.UpdateKind.*
import com.zachvlat.footballscores.notifications.diffSnapshots
import com.zachvlat.footballscores.notifications.flattenIncidents
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveDiffTest {

    private fun s(score1: Int, score2: Int, eps: String, esid: Int) = LiveSnapshot(score1, score2, eps, esid)

    @Test
    fun football_kickOffGoalsHalfTimeFullTime() {
        assertEquals(listOf(KICK_OFF), diffSnapshots("soccer", s(0, 0, "NS", 1), s(0, 0, "1'", 2)))
        assertEquals(listOf(GOAL, GOAL), diffSnapshots("soccer", s(0, 0, "20'", 2), s(1, 1, "21'", 2)))
        assertEquals(listOf(HALF_TIME), diffSnapshots("soccer", s(1, 1, "45+2'", 2), s(1, 1, "HT", 3)))
        assertEquals(emptyList<Any>(), diffSnapshots("soccer", s(1, 1, "HT", 3), s(1, 1, "HT", 3)))
        assertEquals(listOf(FULL_TIME), diffSnapshots("soccer", s(4, 1, "90+3'", 2), s(4, 1, "FT", 6)))
        assertEquals(emptyList<Any>(), diffSnapshots("soccer", s(4, 1, "FT", 6), s(4, 1, "FT", 6)))
    }

    @Test
    fun disallowedGoal_isNotAGoal() {
        assertEquals(emptyList<Any>(), diffSnapshots("soccer", s(1, 0, "30'", 2), s(0, 0, "31'", 2)))
    }

    @Test
    fun firstSight_onlyReportsAnEnding() {
        assertEquals(emptyList<Any>(), diffSnapshots("soccer", null, s(2, 0, "60'", 2)))
        assertEquals(listOf(FULL_TIME), diffSnapshots("soccer", null, s(2, 0, "FT", 6)))
        assertEquals(listOf(CALLED_OFF), diffSnapshots("soccer", null, s(0, 0, "Postp.", 5)))
    }

    @Test
    fun basketball_periodEnds_overtimeFinal_noGoalSpam() {
        assertEquals(listOf(PERIOD_END), diffSnapshots("basketball", s(25, 22, "1Q", 35), s(27, 22, "2Q", 0)))
        assertEquals(emptyList<Any>(), diffSnapshots("basketball", s(70, 66, "4Q", 43), s(80, 75, "4Q", 43)))
        assertEquals(listOf(FULL_TIME), diffSnapshots("basketball", s(104, 104, "OT", 0), s(112, 115, "OT", 36)))
    }

    @Test
    fun incidents_portoNapoli_flattenGroupedAndFlat() {
        val json = """{"Eid":"1882788","Incs-s":{"2":[
            {"Min":80,"Nm":1,"Sc":[3,1],"Sor":3,"Incs":[
              {"Min":80,"Nm":1,"PosA":2,"Aid":"17161","ID":"17161","Pn":"Jakub Kiwior","IT":36,"Sc":[3,1],"Sor":3},
              {"Min":80,"Nm":1,"PosA":5,"Aid":"42501","ID":"42501","Pn":"In-Beom Hwang","IT":63,"Sc":[3,1],"Sor":3}]},
            {"Min":89,"Nm":2,"PosA":2,"Aid":"9148","ID":"9148","Pn":"Rafa Marin","IT":45,"Sor":1},
            {"Min":90,"Nm":1,"PosA":5,"Aid":"96313","ID":"96313","Pn":"Samuel Aghehowa","IT":37,"Sc":[4,1],"Sor":0}]}}"""
        val incidents = flattenIncidents(Gson().fromJson(json, MatchDetailResponse::class.java))

        assertEquals(
            listOf(IncidentKind.GOAL, IncidentKind.ASSIST, IncidentKind.RED_CARD, IncidentKind.PENALTY_GOAL),
            incidents.map { it.kind }
        )
        assertEquals("Samuel Aghehowa", incidents.last().player)
        assertEquals(4, incidents.map { it.id }.toSet().size)
    }
}
