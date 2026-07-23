package com.zachvlat.footballscores.data.model

data class LineupsResponse(
    val Eid: String,
    val Lu: List<TeamLineup>?,
    val Subs: Map<String, List<Substitution>>?
)

data class TeamLineup(
    val Tnb: Int,
    val Ps: List<Player>,
    val Fo: List<Int>?
)

data class Player(
    val Aid: String?,
    val Pid: String,
    val Fn: String,
    val Ln: String,
    val Pnt: String?,
    val imageUrl: String?,
    val Pos: Int,
    val Pon: String,
    val Snu: Int,
    val PosA: Int,
    val Fp: String?,
    val Mo: Int?,
    val PosS: Int?
) {
    fun getFullName(): String = "$Fn $Ln"

    fun getPlayerImageUrl(): String? {
        return imageUrl?.let {
            "https://img.aicdn.com/headshots/$it"
        }
    }

    fun isStarting(): Boolean = Fp != null

    fun isBench(): Boolean = PosS != null

    fun isCoach(): Boolean = Pon == "COACH"
}

data class Substitution(
    val Min: Int,
    val Nm: Int,
    val Aid: String?,
    val ID: String?,
    val Fn: String?,
    val Ln: String?,
    val Pnt: String?,
    val Pnum: Int?,
    val Pn: String?,
    val PnumO: Int?,
    val IDo: String?,
    val AIDo: String?,
    val IT: Int,
    val Sor: Int?
) {
    fun getPlayerInName(): String {
        return if (!Pn.isNullOrEmpty()) Pn
        else if (!Fn.isNullOrEmpty() && !Ln.isNullOrEmpty()) "$Fn $Ln"
        else "Unknown Player"
    }
}
