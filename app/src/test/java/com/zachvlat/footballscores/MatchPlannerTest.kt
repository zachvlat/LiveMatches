package com.zachvlat.footballscores

import com.google.gson.Gson
import com.zachvlat.footballscores.data.api.esdToEpochMillis
import com.zachvlat.footballscores.data.favorites.FavoriteTeam
import com.zachvlat.footballscores.data.favorites.StarredMatch
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.notifications.MatchPlan
import com.zachvlat.footballscores.notifications.mergePlan
import com.zachvlat.footballscores.notifications.planStarredMatches
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchPlannerTest {

    private val offset = 2
    private val now = esdToEpochMillis(20261105120000, offset)!!

    private fun event(eid: String, home: String, homeId: String, away: String, awayId: String, eps: String, esid: Int, esd: Long) =
        Gson().fromJson(
            """{"Eid":"$eid","Eps":"$eps","Esid":$esid,"Esd":$esd,
               "T1":[{"ID":"$homeId","Nm":"$home"}],"T2":[{"ID":"$awayId","Nm":"$away"}]}""",
            Event::class.java
        )

    private fun starred(eid: String, esd: Long = 20261105210000) = StarredMatch("soccer", eid, esd, "A", "B")

    @Test
    fun favoriteTeamMatches_getStarred_butNotFinishedOnes() {
        val liverpool = FavoriteTeam("soccer", "92", "Liverpool", null)
        val fetched = mapOf(
            "soccer" to listOf(
                event("1", "Liverpool", "92", "Arsenal", "1", "NS", 1, 20261105210000),
                event("2", "Chelsea", "3", "Liverpool", "92", "FT", 6, 20261105100000),
                event("3", "Chelsea", "3", "Arsenal", "1", "NS", 1, 20261105210000)
            )
        )
        val plan = planStarredMatches(emptySet(), setOf(liverpool), fetched, now, offset)

        assertEquals(listOf("1"), plan.keep.map { it.eid })
        assertEquals("Liverpool", plan.keep.single().home)
        assertEquals(emptyList<StarredMatch>(), plan.remove)
    }

    @Test
    fun starredMatches_areRemovedWhenOverPostponedOrStale() {
        val fetched = mapOf(
            "soccer" to listOf(
                event("10", "A", "1", "B", "2", "FT", 6, 20261105100000),
                event("11", "A", "1", "B", "2", "Postp.", 5, 20261105210000),
                event("12", "A", "1", "B", "2", "Canc.", 56, 20261105210000),
                event("13", "Home", "1", "Away", "2", "NS", 1, 20261105223000)
            )
        )
        val staleFromTwoDaysAgo = starred("14", esd = 20261103200000)
        val nextWeek = starred("15", esd = 20261112200000)
        val all = setOf(starred("10"), starred("11"), starred("12"), starred("13"), staleFromTwoDaysAgo, nextWeek)

        val plan = planStarredMatches(all, emptySet(), fetched, now, offset)

        assertEquals(setOf("10", "11", "12", "14"), plan.remove.map { it.eid }.toSet())
        assertEquals(setOf("13", "15"), plan.keep.map { it.eid }.toSet())
        val refreshed = plan.keep.single { it.eid == "13" }
        assertEquals(20261105223000, refreshed.esd)
        assertEquals("Home", refreshed.home)
    }

    @Test
    fun merge_keepsWhatTheUserChangedDuringTheFetch() {
        val snapshot = setOf(starred("1"), starred("2"))
        val plan = MatchPlan(keep = listOf(starred("1"), starred("2"), starred("auto")), remove = emptyList())
        val current = setOf(starred("1"), starred("new"))

        val merged = mergePlan(current, snapshot, plan)

        assertEquals(setOf("1", "new", "auto"), merged.map { it.eid }.toSet())
    }
}
