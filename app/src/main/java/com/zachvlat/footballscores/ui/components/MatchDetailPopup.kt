package com.zachvlat.footballscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zachvlat.footballscores.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailPopup(
    matchDetail: MatchDetailResponse,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                MatchDetailHeader(matchDetail, onDismiss)
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MatchDetailScore(matchDetail)
                    }
                    
                    item {
                        MatchDetailVenue(matchDetail.Venue)
                    }
                    
                    matchDetail.`Incs-s`?.get("1")?.let { incidentGroups ->
                        val incidents = incidentGroups.flatMap { it.Incs ?: emptyList() }
                        if (incidents.isNotEmpty()) {
                            item {
                                MatchDetailIncidents(incidents)
                            }
                        }
                    }
                    
                    matchDetail.Media?.let { media ->
                        item {
                            MatchDetailMedia(media)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchDetailHeader(
    matchDetail: MatchDetailResponse,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = matchDetail.Stg.CompN ?: matchDetail.Stg.Snm,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${matchDetail.T1.first().Nm} vs ${matchDetail.T2.first().Nm}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close"
            )
        }
    }
    
    HorizontalDivider()
}

@Composable
private fun MatchDetailScore(matchDetail: MatchDetailResponse) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailTeamInfo(team = matchDetail.T1.first())
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!matchDetail.Trh1.isNullOrEmpty() && !matchDetail.Trh2.isNullOrEmpty()) {
                        Text(
                            text = "HT: ${matchDetail.Trh1}-${matchDetail.Trh2}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    val score = if (matchDetail.Tr1.isNullOrEmpty() || matchDetail.Tr2.isNullOrEmpty()) {
                        "vs"
                    } else {
                        "${matchDetail.Tr1} - ${matchDetail.Tr2}"
                    }
                    
                    Text(
                        text = score,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = matchDetail.Eps,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                DetailTeamInfo(team = matchDetail.T2.first())
            }
        }
    }
}

@Composable
private fun DetailTeamInfo(team: DetailTeam) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        val imageUrl = team.getTeamImageUrl()
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "${team.Nm} logo",
                modifier = Modifier.size(48.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = team.Abr,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = team.Nm,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun MatchDetailVenue(venue: Venue?) {
    venue?.let {
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Venue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it.Vnm,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                it.getVenueImageUrl()?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Venue image",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchDetailIncidents(incidents: List<Incident>) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Match Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            incidents.forEach { incident ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${incident.Min}'",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = incident.getIncidentType(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        color = when (incident.IT) {
                            36 -> Color(0xFF4CAF50)
                            63 -> MaterialTheme.colorScheme.primary
                            37 -> Color(0xFFFFC107)
                            38 -> Color(0xFFF44336)
                            45 -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Text(
                        text = incident.getPlayerName(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (incident != incidents.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchDetailMedia(media: MatchDetailMedia) {
    val tvChannels = media.`112` ?: emptyList()
    val highlights = media.`37` ?: emptyList()
    
    if (tvChannels.isNotEmpty() || highlights.isNotEmpty()) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (tvChannels.isNotEmpty()) {
                    Text(
                        text = "TV Channels:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    tvChannels.forEach { channel ->
                        Text(
                            text = "• ${channel.eventId}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }
                }
                
                if (highlights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Highlights Available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}