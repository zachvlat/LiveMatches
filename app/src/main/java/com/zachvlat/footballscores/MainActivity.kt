package com.zachvlat.footballscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.zachvlat.footballscores.ui.components.*
import com.zachvlat.footballscores.ui.theme.FootballscoresTheme
import com.zachvlat.footballscores.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

enum class SportType(val displayName: String, val apiPath: String, val hasMatchDetails: Boolean = false) {
    SOCCER("Football", "soccer", hasMatchDetails = true),
    BASKETBALL("Basketball", "basketball", hasMatchDetails = true),
    CRICKET("Cricket", "cricket"),
    HOCKEY("Hockey", "hockey")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FootballscoresTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedSport by remember { mutableStateOf(SportType.SOCCER) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Sports",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Divider()
                
                SportType.values().forEach { sport ->
                    NavigationDrawerItem(
                        label = { Text(sport.displayName) },
                        selected = selectedSport == sport,
                        onClick = {
                            selectedSport = sport
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "LiveMatches",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
            
            Box(modifier = Modifier.weight(1f)) {
                key(selectedSport) { SportScreen(sportType = selectedSport) }
            }
        }
    }
}

@Composable
fun SportScreen(sportType: SportType) {
    val viewModel: ScoresViewModel = viewModel(key = sportType.name) { ScoresViewModel(sportType.apiPath) }
    val currentDate by viewModel.currentDate.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(60_000)
                if (viewModel.currentDate.value == todayDateString()) viewModel.refresh()
            }
        }
    }

    SportContent(
        sportType = sportType,
        currentDate = currentDate,
        isRefreshing = viewModel.isRefreshing.collectAsState().value,
        uiState = viewModel.uiState.collectAsState().value,
        matchDetailState = viewModel.matchDetailState.collectAsState().value,
        onRefresh = viewModel::refresh,
        onLoadScoresForDate = viewModel::loadScoresForDate,
        onDismissMatchDetail = viewModel::dismissMatchDetail,
        onMatchClick = if (sportType.hasMatchDetails) viewModel::onMatchClick else { _ -> }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportContent(
    sportType: SportType,
    currentDate: String,
    isRefreshing: Boolean,
    uiState: ScoresUiState,
    matchDetailState: MatchDetailState,
    onRefresh: () -> Unit,
    onLoadScoresForDate: (String) -> Unit,
    onDismissMatchDetail: () -> Unit,
    onMatchClick: (String) -> Unit = {}
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showLiveOnly by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sportType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDateForDisplay(currentDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isRefreshing) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• updating...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.alpha(0.8f)
                                )
                            }
                        }
                    }
                    
                    Row {
                        IconButton(
                            onClick = { showDateDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search teams & competitions...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { 
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showLiveOnly,
                        onClick = { showLiveOnly = !showLiveOnly },
                        label = { 
                            Text("🔴 Live Only") 
                        },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (uiState) {
                is ScoresUiState.Loading -> LoadingIndicator()
                is ScoresUiState.Success -> {
                    val query = searchQuery.trim().lowercase()
                    val filteredStages = uiState.response.Stages.map { stage ->
                        val stageText = listOf(stage.Snm, stage.CompN, stage.Cnm, stage.Csnm)
                            .joinToString(" ") { it.orEmpty().lowercase() }
                        stage.copy(Events = stage.Events.filter { event ->
                            (query.isEmpty() ||
                                stageText.contains(query) ||
                                event.T1.firstOrNull()?.Nm.orEmpty().lowercase().contains(query) ||
                                event.T2.firstOrNull()?.Nm.orEmpty().lowercase().contains(query)) &&
                                (!showLiveOnly || event.isLive())
                        })
                    }.filter { it.Events.isNotEmpty() }

                    if (filteredStages.isEmpty()) {
                        val sport = sportType.displayName.lowercase()
                        val live = if (showLiveOnly) "live " else ""
                        val where = if (query.isNotEmpty()) "for \"$searchQuery\"" else "for selected date"
                        EmptyState(message = "No $live$sport matches found $where", onRefresh = onRefresh)
                    } else {
                        MatchList(stages = filteredStages, onMatchClick = onMatchClick)
                    }
                }
                is ScoresUiState.Error -> ErrorMessage(message = uiState.message, onRefresh = onRefresh)
            }
        }
    }
    
    if (showDateDialog) {
        DateSelectionDialog(
            currentDate = currentDate,
            onDateSelected = { date ->
                onLoadScoresForDate(date)
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false }
        )
    }
    
    if (sportType.hasMatchDetails) {
        when (val state = matchDetailState) {
            is MatchDetailState.Success -> {
                MatchDetailBottomSheet(
                    matchDetail = state.matchDetail,
                    lineups = state.lineups,
                    event = state.event,
                    onDismiss = onDismissMatchDetail
                )
            }
            is MatchDetailState.Loading -> {
                Dialog(onDismissRequest = onDismissMatchDetail) {
                    Card(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            is MatchDetailState.Error -> {
                AlertDialog(
                    onDismissRequest = onDismissMatchDetail,
                    title = { Text("Error") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = onDismissMatchDetail) {
                            Text("OK")
                        }
                    }
                )
            }
            else -> { }
        }
    }
}

@Composable
fun MatchList(
    stages: List<com.zachvlat.footballscores.data.model.Stage>,
    onMatchClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        stages.forEach { stage ->
            if (stage.Events.isNotEmpty()) {
                item {
                    CompetitionHeader(stage = stage)
                }
                
                items(stage.Events) { event ->
                    MatchCard(event = event, onMatchClick = onMatchClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionDialog(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateStringToUtcMillis(currentDate)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = "Select Date",
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    headline = {
                        Text(
                            text = datePickerState.selectedDateMillis?.let { millis ->
                                utcFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
                            } ?: "Pick a date",
                            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    showModeToggle = false
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { datePickerState.selectedDateMillis?.let { onDateSelected(utcMillisToDateString(it)) } }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh")
        }
    }
}


private fun utcFormat(pattern: String, locale: Locale = Locale.US) =
    SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }

fun utcMillisToDateString(millis: Long): String = utcFormat("yyyyMMdd").format(Date(millis))

fun dateStringToUtcMillis(dateString: String): Long? =
    runCatching { utcFormat("yyyyMMdd").parse(dateString)?.time }.getOrNull()

fun formatDateForDisplay(dateString: String): String {
    return when {
        dateString.length == 8 -> {
            val year = dateString.substring(0, 4)
            val month = dateString.substring(4, 6)
            val day = dateString.substring(6, 8)
            "$year-$month-$day"
        }
        else -> dateString
    }
}