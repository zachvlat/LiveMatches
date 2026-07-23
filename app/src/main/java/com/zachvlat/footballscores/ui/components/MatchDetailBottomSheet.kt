package com.zachvlat.footballscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zachvlat.footballscores.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailBottomSheet(
    matchDetail: MatchDetailResponse?,
    lineups: LineupsResponse?,
    event: Event?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MatchSheetHeader(matchDetail, event)
            }

            if (lineups != null) {
                lineups.Lu?.forEach { teamLineup ->
                    if (teamLineup.Tnb == 1 || teamLineup.Tnb == 2) {
                        item {
                            TeamLineupSection(teamLineup, event, teamLineup.Tnb)
                        }
                    }
                }

                lineups.Subs?.let { subs ->
                    if (subs.isNotEmpty()) {
                        item {
                            SubstitutionsSection(subs)
                        }
                    }
                }
            }

            val allIncidents = listOfNotNull(
                matchDetail?.`Incs-s`?.get("1"),
                matchDetail?.`Incs-s`?.get("2")
            ).flatten().sortedBy { it.Min }

            if (allIncidents.isNotEmpty()) {
                item {
                    IncidentsSection(allIncidents)
                }
            }
        }
    }
}

@Composable
private fun MatchSheetHeader(matchDetail: MatchDetailResponse?, event: Event?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        val team1Name = matchDetail?.T1?.firstOrNull()?.Nm ?: event?.T1?.firstOrNull()?.Nm ?: ""
        val team2Name = matchDetail?.T2?.firstOrNull()?.Nm ?: event?.T2?.firstOrNull()?.Nm ?: ""
        val score1 = matchDetail?.Tr1 ?: event?.Tr1
        val score2 = matchDetail?.Tr2 ?: event?.Tr2
        val status = matchDetail?.Eps ?: event?.Eps ?: ""

        Text(
            text = matchDetail?.Stg?.CompN ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamColumn(
                name = team1Name,
                imageUrl = matchDetail?.T1?.firstOrNull()?.getTeamImageUrl()
                    ?: event?.T1?.firstOrNull()?.getTeamImageUrl()
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val displayScore = if (score1 != null && score2 != null && score1.isNotEmpty() && score2.isNotEmpty()) {
                    "$score1 - $score2"
                } else {
                    "vs"
                }
                Text(
                    text = displayScore,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TeamColumn(
                name = team2Name,
                imageUrl = matchDetail?.T2?.firstOrNull()?.getTeamImageUrl()
                    ?: event?.T2?.firstOrNull()?.getTeamImageUrl()
            )
        }
    }
}

@Composable
private fun TeamColumn(name: String, imageUrl: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.size(56.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(3).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun TeamLineupSection(teamLineup: TeamLineup, event: Event?, teamNumber: Int) {
    val teamName = if (teamNumber == 1) {
        event?.T1?.firstOrNull()?.Nm ?: "Team 1"
    } else {
        event?.T2?.firstOrNull()?.Nm ?: "Team 2"
    }

    val formation = teamLineup.Fo?.joinToString("-") ?: ""
    val starters = teamLineup.Ps.filter { it.isStarting() }
    val coach = teamLineup.Ps.find { it.isCoach() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (formation.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = formation,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (coach != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Coach: ${coach.getFullName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val groupedStarters = starters.groupBy { it.Pos }.toSortedMap()
            groupedStarters.forEach { (position, players) ->
                val positionName = when (position) {
                    1 -> "Goalkeeper"
                    2 -> "Defenders"
                    3 -> "Midfielders"
                    4 -> "Forwards"
                    else -> "Other"
                }
                Text(
                    text = positionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${player.Snu}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = player.getFullName(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        player.Fp?.let { fp ->
                            Text(
                                text = fp.replace(":", ","),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubstitutionsSection(subs: Map<String, List<Substitution>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Substitutions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            subs.forEach { (teamKey, teamSubs) ->
                val outgoing = teamSubs.filter { it.IT == 4 }
                val incoming = teamSubs.filter { it.IT == 5 }

                outgoing.forEach { out ->
                    val inMatch = incoming.find { it.ID == out.IDo }
                    val playerOut = out.getPlayerInName()
                    val playerIn = inMatch?.getPlayerInName() ?: "Unknown"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = if (teamKey == "1") Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFF2196F3).copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "⇄",
                                    modifier = Modifier.size(12.dp),
                                    fontSize = 10.sp,
                                    color = if (teamKey == "1") Color(0xFF4CAF50) else Color(0xFF2196F3)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${out.Min}'",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = "$playerOut → $playerIn",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentsSection(incidentGroups: List<MatchIncidentGroup>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Match Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val sortedGroups = incidentGroups.sortedBy { it.Min }

            sortedGroups.forEachIndexed { index, group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${group.Min}'",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(40.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (group.isFlat()) {
                        val icon = when (group.IT) {
                            36 -> "⚽ "
                            37 -> "🟨 "
                            38 -> "🟥 "
                            39 -> "⚽ "
                            88 -> "❌ "
                            45 -> "🟥 "
                            else -> ""
                        }
                        val color = when (group.IT) {
                            36 -> Color(0xFF4CAF50)
                            37 -> Color(0xFFFFC107)
                            38 -> Color(0xFFF44336)
                            39 -> Color(0xFF4CAF50)
                            88 -> MaterialTheme.colorScheme.onSurface
                            45 -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = "${icon}${group.getFlatPlayerName()}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            color = color
                        )
                    } else {
                        val goals = group.Incs?.filter { it.IT == 36 } ?: emptyList()
                        val assists = group.Incs?.filter { it.IT == 63 } ?: emptyList()
                        val others = group.Incs?.filter { it.IT != 36 && it.IT != 63 } ?: emptyList()

                        val displayText = when {
                            goals.isNotEmpty() && assists.isNotEmpty() -> {
                                val scorer = goals.first().getPlayerName()
                                val assister = assists.first().getPlayerName()
                                "$scorer ($assister)"
                            }
                            goals.isNotEmpty() -> {
                                goals.first().getPlayerName()
                            }
                            others.isNotEmpty() -> {
                                others.first().getPlayerName()
                            }
                            else -> ""
                        }

                        val icon = when {
                            goals.isNotEmpty() -> "⚽ "
                            else -> ""
                        }
                        val color = when {
                            goals.isNotEmpty() -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Text(
                            text = "${icon}${displayText}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            color = color
                        )
                    }

                    if (group.Sc != null && group.Sc.size == 2) {
                        Text(
                            text = "${group.Sc[0]}-${group.Sc[1]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (index < sortedGroups.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
