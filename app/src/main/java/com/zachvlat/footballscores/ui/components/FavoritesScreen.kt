package com.zachvlat.footballscores.ui.components

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zachvlat.footballscores.notifications.LiveTracker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zachvlat.footballscores.SportType
import com.zachvlat.footballscores.data.favorites.FavoriteTeam
import com.zachvlat.footballscores.data.favorites.FavoritesStore
import com.zachvlat.footballscores.data.model.teamImageUrl

@Composable
fun FavoritesScreen() {
    val favorites = FavoritesStore.get(LocalContext.current)
    val teams by favorites.teams.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Favorite teams",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        AlertsSetup()

        if (teams.isEmpty()) {
            Text(
                text = "No favorite teams yet.\nOpen a match and tap the star under a team.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            SportType.entries.forEach { sport ->
                val sportTeams = teams.filter { it.sport == sport.apiPath }.sortedBy { it.name.lowercase() }
                if (sportTeams.isEmpty()) return@forEach

                item(key = sport.name) {
                    Text(
                        text = sport.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(sportTeams, key = { "${it.sport}|${it.id}" }) { team ->
                    FavoriteTeamRow(team = team, onRemove = { favorites.toggleTeam(team) })
                }
            }
        }
    }
}

private data class AlertBlockers(
    val notificationsOff: Boolean,
    val exactAlarmsOff: Boolean,
    val batteryRestricted: Boolean
)

private fun alertBlockers(context: Context) = AlertBlockers(
    notificationsOff = !NotificationManagerCompat.from(context).areNotificationsEnabled(),
    exactAlarmsOff = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
    batteryRestricted = !context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
)

@Composable
private fun AlertsSetup() {
    val context = LocalContext.current
    var blockers by remember { mutableStateOf(alertBlockers(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = alertBlockers(context)
                if (now != blockers) LiveTracker.sync(context)
                blockers = now
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    if (blockers.notificationsOff) {
        SetupCard(
            text = "Notifications are off, so you won't get reminders or live alerts.",
            action = "Turn on",
            intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else appSettings
        )
    }
    if (blockers.exactAlarmsOff && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        SetupCard(
            text = "Allow exact alarms so live alerts start at kick-off. Without it, you'll need to tap a notification to start following a match.",
            action = "Allow",
            intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        )
    }
    if (blockers.batteryRestricted) {
        SetupCard(
            text = "Battery optimization can stop live alerts while your phone is locked. On the next screen, open Battery and choose Unrestricted.",
            action = "Open settings",
            intent = appSettings
        )
    }
}

@Composable
private fun SetupCard(text: String, action: String, intent: Intent) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { runCatching { context.startActivity(intent) } },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(action)
            }
        }
    }
}

@Composable
private fun FavoriteTeamRow(team: FavoriteTeam, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val logoUrl = teamImageUrl(team.img)
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = "${team.name} logo",
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = team.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = team.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StarButton(
                starred = true,
                onClick = onRemove,
                contentDescription = "Remove ${team.name} from favorites"
            )
        }
    }
}
