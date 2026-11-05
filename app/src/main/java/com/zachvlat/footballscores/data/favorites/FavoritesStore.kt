package com.zachvlat.footballscores.data.favorites

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StarredMatch(val sport: String, val eid: String, val esd: Long, val home: String, val away: String) {
    val key get() = "$sport|$eid"

    fun encode() = listOf("m2", sport, eid, esd.toString(), home, away).joinToString(SEP)

    companion object {
        private const val SEP = "\u001F"

        fun decode(entry: String): StarredMatch? {
            if (entry.startsWith("m2$SEP")) {
                val parts = entry.split(SEP)
                if (parts.size != 6) return null
                return parts[3].toLongOrNull()?.let { StarredMatch(parts[1], parts[2], it, parts[4], parts[5]) }
            }
            val parts = entry.split("|", limit = 3)
            if (parts.size != 3) return null
            return parts[2].toLongOrNull()?.let { StarredMatch(parts[0], parts[1], it, "", "") }
        }
    }
}

data class FavoriteTeam(val sport: String, val id: String, val name: String, val img: String?) {
    fun encode() = "v2|$sport|$id|${img.orEmpty()}|$name"

    companion object {
        fun decode(entry: String): FavoriteTeam? {
            if (entry.startsWith("v2|")) {
                val parts = entry.removePrefix("v2|").split("|", limit = 4)
                return if (parts.size == 4) FavoriteTeam(parts[0], parts[1], parts[3], parts[2].ifEmpty { null }) else null
            }
            val parts = entry.split("|", limit = 3)
            return if (parts.size == 3) FavoriteTeam(parts[0], parts[1], parts[2], null) else null
        }
    }
}

class FavoritesStore private constructor(private val prefs: SharedPreferences) {

    private val _matches = MutableStateFlow(load(KEY_MATCHES, StarredMatch::decode))
    val matches: StateFlow<Set<StarredMatch>> = _matches.asStateFlow()

    private val _teams = MutableStateFlow(load(KEY_TEAMS, FavoriteTeam::decode))
    val teams: StateFlow<Set<FavoriteTeam>> = _teams.asStateFlow()

    @Synchronized
    fun updateMatches(transform: (Set<StarredMatch>) -> Set<StarredMatch>) {
        _matches.value = transform(_matches.value)
        save(KEY_MATCHES, _matches.value.map { it.encode() })
    }

    @Synchronized
    fun toggleMatch(match: StarredMatch): Boolean {
        val existing = _matches.value.find { it.key == match.key }
        _matches.value = if (existing != null) _matches.value - existing else _matches.value + match
        save(KEY_MATCHES, _matches.value.map { it.encode() })
        return existing == null
    }

    @Synchronized
    fun toggleTeam(team: FavoriteTeam): Boolean {
        val existing = _teams.value.find { it.sport == team.sport && it.id == team.id }
        _teams.value = if (existing != null) _teams.value - existing else _teams.value + team
        save(KEY_TEAMS, _teams.value.map { it.encode() })
        return existing == null
    }

    @Synchronized
    fun fillMissingTeamLogos(sport: String, logos: Map<String, String>) {
        val updated = _teams.value.map { team ->
            if (team.sport == sport && team.img == null) logos[team.id]?.let { team.copy(img = it) } ?: team else team
        }.toSet()
        if (updated == _teams.value) return
        _teams.value = updated
        save(KEY_TEAMS, updated.map { it.encode() })
    }

    private fun <T> load(key: String, decode: (String) -> T?): Set<T> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull(decode).toSet()

    private fun save(key: String, entries: List<String>) {
        prefs.edit().putStringSet(key, entries.toSet()).apply()
    }

    companion object {
        private const val KEY_MATCHES = "starred_matches"
        private const val KEY_TEAMS = "favorite_teams"

        @Volatile
        private var instance: FavoritesStore? = null

        fun get(context: Context): FavoritesStore = instance ?: synchronized(this) {
            instance ?: FavoritesStore(
                context.applicationContext.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            ).also { instance = it }
        }
    }
}
