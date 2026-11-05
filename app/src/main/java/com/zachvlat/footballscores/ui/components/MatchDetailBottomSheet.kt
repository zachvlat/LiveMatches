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
                item {
                    LineupsSection(lineups, event)
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

        val periods = matchDetail?.periodScores().orEmpty()
        if (periods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEach { (label, score1, score2) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = score1, style = MaterialTheme.typography.bodyMedium)
                        Text(text = score2, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
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
private fun LineupsSection(lineups: LineupsResponse, event: Event?) {
    val team1Lineup = lineups.Lu?.find { it.Tnb == 1 }
    val team2Lineup = lineups.Lu?.find { it.Tnb == 2 }

    if (team1Lineup == null && team2Lineup == null) return

    val team1Name = event?.T1?.firstOrNull()?.Nm ?: "Team 1"
    val team2Name = event?.T2?.firstOrNull()?.Nm ?: "Team 2"

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
                text = "Starting Lineups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                StartingXIColumn(
                    teamLineup = team1Lineup,
                    teamName = team1Name,
                    modifier = Modifier.weight(1f)
                )
                StartingXIColumn(
                    teamLineup = team2Lineup,
                    teamName = team2Name,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StartingXIColumn(teamLineup: TeamLineup?, teamName: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val formation = teamLineup?.Fo?.joinToString("-")
        val starters = teamLineup?.Ps?.filter { it.isStarting() } ?: emptyList()
        val coach = teamLineup?.Ps?.find { it.isCoach() }

        Text(
            text = teamName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
        if (!formation.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (coach != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Coach: ${coach.getFullName()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

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
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
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

            subs.values.forEach { teamSubs ->
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

                    val (kind, name) = if (group.isFlat()) {
                        IncidentKind.of(group.IT) to group.getFlatPlayerName()
                    } else {
                        group.Incs.orEmpty().firstOrNull { it.IT != IncidentKind.ASSIST.code }
                            ?.let { IncidentKind.of(it.IT) to it.getPlayerName() } ?: (null to "")
                    }
                    val assist = group.Incs?.find { it.IT == IncidentKind.ASSIST.code }?.getPlayerName()
                    val color = when {
                        kind?.isGoal == true -> Color(0xFF4CAF50)
                        kind == IncidentKind.YELLOW_CARD -> Color(0xFFFFC107)
                        kind == IncidentKind.RED_CARD || kind == IncidentKind.SECOND_YELLOW -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = (kind?.let { "${it.icon} " } ?: "") + name + (kind?.suffix ?: "") +
                            (assist?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        color = color
                    )

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
