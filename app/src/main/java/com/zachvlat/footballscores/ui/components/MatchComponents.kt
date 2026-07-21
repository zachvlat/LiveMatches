package com.zachvlat.footballscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.model.Stage
import com.zachvlat.footballscores.data.model.Team
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

@Composable
private fun parseColor(colorString: String): Color {
    return try {
        when {
            colorString.startsWith("#") -> {
                Color(android.graphics.Color.parseColor(colorString))
            }
            colorString.matches(Regex("^[0-9A-Fa-f]{6}$")) -> {
                Color(android.graphics.Color.parseColor("#$colorString"))
            }
            else -> {
                Color(android.graphics.Color.parseColor(colorString))
            }
        }
    } catch (e: IllegalArgumentException) {
        // Fallback to a default color if parsing fails
        MaterialTheme.colorScheme.primary
    }
}

@Composable
fun MatchCard(event: Event, onMatchClick: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team 1
            TeamInfo(team = event.T1.first(), alignment = Alignment.End)

            Spacer(modifier = Modifier.width(16.dp))

            // Score Section
            ScoreSection(event = event)

            Spacer(modifier = Modifier.width(16.dp))

            // Team 2
            TeamInfo(team = event.T2.first(), alignment = Alignment.Start)
        }
    }
}

@Composable
private fun TeamInfo(team: Team, alignment: Alignment.Horizontal) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Team Logo/Placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    color = team.Fc?.let { parseColor(it) }
                        ?: MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = team.getTeamImageUrl()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${team.Nm} logo",
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Text(
                    text = team.Abr,
                    color = team.Sc?.let { parseColor(it) }
                        ?: MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Team Name - centered with fixed width
        Text(
            text = team.Nm,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun ScoreSection(event: Event) {
    val isCricket = isCricketMatch(event)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Halftime Score if available
        if (!isCricket && !event.Trh1.isNullOrEmpty() && !event.Trh2.isNullOrEmpty()) {
            Text(
                text = "HT: ${event.Trh1}-${event.Trh2}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Main Score
        if (isCricket) {
            CricketScoreDisplay(event)
        } else {
            val displayScore = getDisplayScore(event)
            Text(
                text = displayScore,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadge(status = event.Eps ?: "NS", minutes = event.Eps ?: "NS", startTime = event.Esd, esid = event.Esid)
        }
    }
}

@Composable
private fun CricketScoreDisplay(event: Event) {
    val team1Runs = event.Tr1C1 ?: 0
    val team1Wickets = event.Tr1CW1 ?: 0
    val team1Overs = event.Tr1CO1 ?: 0.0
    val team2Runs = event.Tr2C1 ?: 0
    val team2Wickets = event.Tr2CW1 ?: 0
    val team2Overs = event.Tr2CO1 ?: 0.0
    
    val team1Name = event.T1.firstOrNull()?.Abr ?: "T1"
    val team2Name = event.T2.firstOrNull()?.Abr ?: "T2"
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Commentary if available
        if (!event.ECo.isNullOrEmpty()) {
            Text(
                text = event.ECo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                modifier = Modifier.width(140.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Team 2 score (usually the batting team)
        if (team2Runs > 0) {
            Text(
                text = "$team2Name: ${team2Runs}/${team2Wickets}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "(${formatOvers(team2Overs)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Team 1 score
        if (team1Runs > 0) {
            if (team2Runs > 0) Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$team1Name: ${team1Runs}/${team1Wickets}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "(${formatOvers(team1Overs)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (team1Runs == 0 && team2Runs == 0) {
            Text(
                text = "vs",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Status
        val safeStatus = event.Eps ?: "NS"
        StatusBadge(status = safeStatus, minutes = safeStatus, startTime = event.Esd, esid = event.Esid)
    }
}

private fun formatStartTime(timestamp: Long): String {
    return try {
        val timeStr = timestamp.toString()
        if (timeStr.length == 14) {
            val year = timeStr.substring(0, 4).toInt()
            val month = timeStr.substring(4, 6).toInt() - 1
            val day = timeStr.substring(6, 8).toInt()
            val hour = timeStr.substring(8, 10).toInt()
            val minute = timeStr.substring(10, 12).toInt()

            val cestCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
            cestCalendar.set(year, month, day, hour, minute, 0)
            cestCalendar.set(Calendar.MILLISECOND, 0)

            val localCalendar = Calendar.getInstance()
            localCalendar.timeInMillis = cestCalendar.timeInMillis

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeFormat.format(localCalendar.time)
        } else {
            timestamp.toString()
        }
    } catch (e: Exception) {
        timestamp.toString()
    }
}

private fun getDisplayScore(event: Event): String {
    val isCricket = event.Tr1C1 != null
    
    if (isCricket) {
        val team1Runs = event.Tr1C1 ?: 0
        val team1Wickets = event.Tr1CW1 ?: 0
        val team1Overs = event.Tr1CO1 ?: 0.0
        val team2Runs = event.Tr2C1 ?: 0
        val team2Wickets = event.Tr2CW1 ?: 0
        val team2Overs = event.Tr2CO1 ?: 0.0
        
        // Show both innings scores if available
        val team1Score = if (team1Runs > 0) "${team1Runs}/${team1Wickets} (${formatOvers(team1Overs)})" else null
        val team2Score = if (team2Runs > 0) "${team2Runs}/${team2Wickets} (${formatOvers(team2Overs)})" else null
        
        return when {
            team1Score != null && team2Score != null -> "$team1Score | $team2Score"
            team2Score != null -> "$team2Score"
            team1Score != null -> "$team1Score"
            else -> "vs"
        }
    }
    
    // Football/Basketball format
    val tr1 = event.Tr1
    val tr2 = event.Tr2
    val status = event.Eps ?: "NS"
    
    val team1Score = if (tr1 == null || tr1.equals("null", ignoreCase = true) || tr1.isEmpty()) "0" else tr1
    val team2Score = if (tr2 == null || tr2.equals("null", ignoreCase = true) || tr2.isEmpty()) "0" else tr2
    
    return if (status == "NS" && team1Score == "0" && team2Score == "0") {
        "vs"
    } else {
        "${team1Score} - ${team2Score}"
    }
}

private fun isCricketMatch(event: Event): Boolean = event.Tr1C1 != null

private fun formatOvers(overs: Double): String {
    if (overs == 0.0) return "0 ov"
    val wholeOvers = overs.toInt()
    val balls = ((overs - wholeOvers) * 10).toInt()
    return if (balls > 0) "$wholeOvers.$balls ov" else "$wholeOvers ov"
}

@Composable
private fun StatusBadge(status: String?, minutes: String?, startTime: Long?, esid: Int = 0) {
    val (statusText, color) = when {
        esid == 33 -> "LIVE" to Color.Green
        esid == 1 && status?.uppercase() == "NS" -> {
            val timeText = startTime?.let { formatStartTime(it) } ?: "NS"
            timeText to MaterialTheme.colorScheme.onSurface
        }
        else -> when (status?.uppercase() ?: "NS") {
            "FT" -> "FT" to Color.Gray
            "AET" -> "AET" to Color.Gray
            "AP" -> "AP" to Color.Gray
            "HT" -> "HT" to Color.Magenta
            "NS" -> {
                val timeText = startTime?.let { formatStartTime(it) } ?: "NS"
                timeText to MaterialTheme.colorScheme.onSurface
            }
            else -> {
                val liveMinutes = if (status?.contains("'") == true) {
                    status
                } else {
                    minutes?.let { 
                        if (it.endsWith("'")) it else "${it}'" 
                    } ?: status
                }
                liveMinutes to Color.Green
            }
        }
    }
    
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = statusText ?: "Unknown",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
    }
}

@Composable
fun ErrorMessage(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun CompetitionHeader(stage: Stage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Competition Badge
            val badgeUrl = stage.getCompetitionBadgeUrl()
            if (badgeUrl != null) {
                AsyncImage(
                    model = badgeUrl,
                    contentDescription = "${stage.CompN ?: stage.Snm} logo",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(32.dp + 12.dp))
            }

            // Competition Info
            Column(modifier = Modifier.weight(1f)) {
                 Text(
                     text = stage.CompN ?: stage.Snm,
                     style = MaterialTheme.typography.titleMedium,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )

                 if (!stage.Cnm.isNullOrEmpty() && stage.Cnm != stage.CompN) {
                     Text(
                         text = stage.Cnm,
                         style = MaterialTheme.typography.bodyMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                     )
                 }

                 Text(
                     text = "${stage.Events.size} matches",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                 )
            }
        }
    }
}

@Composable
fun DateHeader(timestamp: Long) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val dateText = dateFormat.format(Date(timestamp))
    
    Text(
        text = dateText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp)
    )
}