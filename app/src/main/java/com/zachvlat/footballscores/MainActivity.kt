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
import com.zachvlat.footballscores.data.repository.LiveScoresRepository
import com.zachvlat.footballscores.ui.components.*
import com.zachvlat.footballscores.ui.theme.FootballscoresTheme
import com.zachvlat.footballscores.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

enum class SportType(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SOCCER("Football", Icons.Default.Search),
    BASKETBALL("Basketball", Icons.Default.Refresh),
    CRICKET("Cricket", Icons.Default.DateRange),
    HOCKEY("Hockey", Icons.Default.Menu)
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
            // App Header with Menu Button
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
            
            // Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (selectedSport) {
                    SportType.SOCCER -> SportScreen(sportType = SportType.SOCCER)
                    SportType.BASKETBALL -> SportScreen(sportType = SportType.BASKETBALL)
                    SportType.CRICKET -> SportScreen(sportType = SportType.CRICKET)
                    SportType.HOCKEY -> SportScreen(sportType = SportType.HOCKEY)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportScreen(sportType: SportType) {
    when (sportType) {
        SportType.SOCCER -> {
            val repository = remember { LiveScoresRepository() }
            val viewModel: LiveScoresViewModel = viewModel(
                factory = LiveScoresViewModelFactory(repository)
            )
            SportContent(
                sportType = sportType,
                viewModel = viewModel,
                currentDate = viewModel.currentDate.collectAsState().value,
                isRefreshing = viewModel.isRefreshing.collectAsState().value,
                uiState = viewModel.uiState.collectAsState().value,
                matchDetailState = viewModel.matchDetailState.collectAsState().value,
                onRefresh = { viewModel.refresh() },
                onLoadScoresForDate = { viewModel.loadScoresForDate(it) },
                onDismissMatchDetail = { viewModel.dismissMatchDetail() }
            )
        }
        SportType.BASKETBALL -> {
            val repository = remember { LiveScoresRepository() }
            val viewModel: BasketballViewModel = viewModel(
                factory = BasketballViewModelFactory(repository)
            )
            SportContent(
                sportType = sportType,
                viewModel = viewModel,
                currentDate = viewModel.currentDate.collectAsState().value,
                isRefreshing = viewModel.isRefreshing.collectAsState().value,
                uiState = viewModel.uiState.collectAsState().value,
                matchDetailState = null,
                onRefresh = { viewModel.refresh() },
                onLoadScoresForDate = { viewModel.loadScoresForDate(it) },
                onDismissMatchDetail = { }
            )
        }
        SportType.CRICKET -> {
            val repository = remember { LiveScoresRepository() }
            val viewModel: CricketViewModel = viewModel(
                factory = CricketViewModelFactory(repository)
            )
            SportContent(
                sportType = sportType,
                viewModel = viewModel,
                currentDate = viewModel.currentDate.collectAsState().value,
                isRefreshing = viewModel.isRefreshing.collectAsState().value,
                uiState = viewModel.uiState.collectAsState().value,
                matchDetailState = null,
                onRefresh = { viewModel.refresh() },
                onLoadScoresForDate = { viewModel.loadScoresForDate(it) },
                onDismissMatchDetail = { }
            )
        }
        SportType.HOCKEY -> {
            val repository = remember { LiveScoresRepository() }
            val viewModel: HockeyViewModel = viewModel(
                factory = HockeyViewModelFactory(repository)
            )
            SportContent(
                sportType = sportType,
                viewModel = viewModel,
                currentDate = viewModel.currentDate.collectAsState().value,
                isRefreshing = viewModel.isRefreshing.collectAsState().value,
                uiState = viewModel.uiState.collectAsState().value,
                matchDetailState = null,
                onRefresh = { viewModel.refresh() },
                onLoadScoresForDate = { viewModel.loadScoresForDate(it) },
                onDismissMatchDetail = { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportContent(
    sportType: SportType,
    viewModel: Any,
    currentDate: String,
    isRefreshing: Boolean,
    uiState: Any,
    matchDetailState: Any?,
    onRefresh: () -> Unit,
    onLoadScoresForDate: (String) -> Unit,
    onDismissMatchDetail: () -> Unit
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showLiveOnly by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Filter Bar
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
                    // Date and Status Info
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
                    
                    // Action Buttons
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
                
                // Search Field
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
                
                // Filter Chips
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
        
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (uiState) {
                is LiveScoresUiState -> {
                    when (uiState) {
                        is LiveScoresUiState.Loading -> {
                            LoadingIndicator()
                        }
                        is LiveScoresUiState.Success -> {
                            val filteredStages = uiState.response.Stages.map { stage ->
                                var filteredEvents = stage.Events
                                
                                // Apply search filter (teams + competitions + country)
                                if (searchQuery.isNotBlank()) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        val team1Name = event.T1.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val team2Name = event.T2.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val competitionName = stage.Snm?.lowercase() ?: ""
                                        val competitionFullName = stage.CompN?.lowercase() ?: ""
                                        val countryName = stage.Cnm?.lowercase() ?: ""
                                        val countryShortName = stage.Csnm?.lowercase() ?: ""
                                        val query = searchQuery.lowercase()
                                        
                                        team1Name.contains(query) || 
                                        team2Name.contains(query) ||
                                        competitionName.contains(query) ||
                                        competitionFullName.contains(query) ||
                                        countryName.contains(query) ||
                                        countryShortName.contains(query)
                                    }
                                }
                                
                                // Apply live filter
                                if (showLiveOnly) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        event.isLive()
                                    }
                                }
                                
                                // Create new Stage object with filtered events
                                com.zachvlat.footballscores.data.model.Stage(
                                    Sid = stage.Sid ?: "",
                                    Snm = stage.Snm ?: "",
                                    Scd = stage.Scd ?: "",
                                    Cnm = stage.Cnm ?: "",
                                    CnmT = stage.CnmT ?: "",
                                    Csnm = stage.Csnm ?: "",
                                    Ccd = stage.Ccd ?: "",
                                    CompId = stage.CompId ?: "",
                                    CompN = stage.CompN ?: "",
                                    CompUrlName = stage.CompUrlName ?: "",
                                    CompD = stage.CompD ?: "",
                                    CompST = stage.CompST ?: "",
                                    Scu = stage.Scu ?: 0,
                                    badgeUrl = stage.badgeUrl,
                                    firstColor = stage.firstColor ?: "",
                                    Events = filteredEvents
                                )
                            }.filter { it.Events.isNotEmpty() }
                            
                            if (filteredStages.isEmpty()) {
                                val message = when {
                                    showLiveOnly && searchQuery.isNotBlank() -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    showLiveOnly -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for selected date"
                                    searchQuery.isNotBlank() -> 
                                        "No ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    else -> 
                                        "No ${sportType.displayName.lowercase()} matches found for selected date"
                                }
                                EmptyState(
                                    message = message,
                                    onRefresh = onRefresh
                                )
                            } else {
                                MatchList(stages = filteredStages, viewModel as LiveScoresViewModel)
                            }
                        }
                        is LiveScoresUiState.Error -> {
                            ErrorMessage(
                                message = uiState.message,
                                onRefresh = onRefresh
                            )
                        }
                    }
                }
                is BasketballUiState -> {
                    when (uiState) {
                        is BasketballUiState.Loading -> {
                            LoadingIndicator()
                        }
                        is BasketballUiState.Success -> {
                            val filteredStages = uiState.response.Stages.map { stage ->
                                var filteredEvents = stage.Events
                                
                                // Apply search filter (teams + competitions)
                                if (searchQuery.isNotBlank()) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        val team1Name = event.T1.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val team2Name = event.T2.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val competitionName = stage.Snm?.lowercase() ?: ""
                                        val competitionFullName = stage.CompN?.lowercase() ?: ""
                                        val query = searchQuery.lowercase()
                                        
                                        team1Name.contains(query) || 
                                        team2Name.contains(query) ||
                                        competitionName.contains(query) ||
                                        competitionFullName.contains(query)
                                    }
                                }
                                
                                // Apply live filter
                                if (showLiveOnly) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        event.isLive()
                                    }
                                }
                                
                                // Create new Stage object with filtered events
                                com.zachvlat.footballscores.data.model.Stage(
                                    Sid = stage.Sid ?: "",
                                    Snm = stage.Snm ?: "",
                                    Scd = stage.Scd ?: "",
                                    Cnm = stage.Cnm ?: "",
                                    CnmT = stage.CnmT ?: "",
                                    Csnm = stage.Csnm ?: "",
                                    Ccd = stage.Ccd ?: "",
                                    CompId = stage.CompId ?: "",
                                    CompN = stage.CompN ?: "",
                                    CompUrlName = stage.CompUrlName ?: "",
                                    CompD = stage.CompD ?: "",
                                    CompST = stage.CompST ?: "",
                                    Scu = stage.Scu ?: 0,
                                    badgeUrl = stage.badgeUrl,
                                    firstColor = stage.firstColor ?: "",
                                    Events = filteredEvents
                                )
                            }.filter { it.Events.isNotEmpty() }
                            
                            if (filteredStages.isEmpty()) {
                                val message = when {
                                    showLiveOnly && searchQuery.isNotBlank() -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    showLiveOnly -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for selected date"
                                    searchQuery.isNotBlank() -> 
                                        "No ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    else -> 
                                        "No ${sportType.displayName.lowercase()} matches found for selected date"
                                }
                                EmptyState(
                                    message = message,
                                    onRefresh = onRefresh
                                )
                            } else {
                                MatchList(stages = filteredStages, viewModel as BasketballViewModel)
                            }
                        }
                        is BasketballUiState.Error -> {
                            ErrorMessage(
                                message = uiState.message,
                                onRefresh = onRefresh
                            )
                        }
                    }
                }
                is CricketUiState -> {
                    when (uiState) {
                        is CricketUiState.Loading -> {
                            LoadingIndicator()
                        }
                        is CricketUiState.Success -> {
                            val filteredStages = uiState.response.Stages.map { stage ->
                                var filteredEvents = stage.Events
                                
                                if (searchQuery.isNotBlank()) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        val team1Name = event.T1.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val team2Name = event.T2.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val competitionName = stage.Snm?.lowercase() ?: ""
                                        val competitionFullName = stage.CompN?.lowercase() ?: ""
                                        val query = searchQuery.lowercase()
                                        
                                        team1Name.contains(query) || 
                                        team2Name.contains(query) ||
                                        competitionName.contains(query) ||
                                        competitionFullName.contains(query)
                                    }
                                }
                                
                                if (showLiveOnly) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        event.isLive()
                                    }
                                }
                                
                                com.zachvlat.footballscores.data.model.Stage(
                                    Sid = stage.Sid ?: "",
                                    Snm = stage.Snm ?: "",
                                    Scd = stage.Scd ?: "",
                                    Cnm = stage.Cnm ?: "",
                                    CnmT = stage.CnmT ?: "",
                                    Csnm = stage.Csnm ?: "",
                                    Ccd = stage.Ccd ?: "",
                                    CompId = stage.CompId ?: "",
                                    CompN = stage.CompN ?: "",
                                    CompUrlName = stage.CompUrlName ?: "",
                                    CompD = stage.CompD ?: "",
                                    CompST = stage.CompST ?: "",
                                    Scu = stage.Scu ?: 0,
                                    badgeUrl = stage.badgeUrl,
                                    firstColor = stage.firstColor ?: "",
                                    Events = filteredEvents
                                )
                            }.filter { it.Events.isNotEmpty() }
                            
                            if (filteredStages.isEmpty()) {
                                val message = when {
                                    showLiveOnly && searchQuery.isNotBlank() -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    showLiveOnly -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for selected date"
                                    searchQuery.isNotBlank() -> 
                                        "No ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    else -> 
                                        "No ${sportType.displayName.lowercase()} matches found for selected date"
                                }
                                EmptyState(
                                    message = message,
                                    onRefresh = onRefresh
                                )
                            } else {
                                MatchList(stages = filteredStages, viewModel as CricketViewModel)
                            }
                        }
                        is CricketUiState.Error -> {
                            ErrorMessage(
                                message = uiState.message,
                                onRefresh = onRefresh
                            )
                        }
                    }
                }
                is HockeyUiState -> {
                    when (uiState) {
                        is HockeyUiState.Loading -> {
                            LoadingIndicator()
                        }
                        is HockeyUiState.Success -> {
                            val filteredStages = uiState.response.Stages.map { stage ->
                                var filteredEvents = stage.Events
                                
                                if (searchQuery.isNotBlank()) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        val team1Name = event.T1.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val team2Name = event.T2.firstOrNull()?.Nm?.lowercase() ?: ""
                                        val competitionName = stage.Snm?.lowercase() ?: ""
                                        val competitionFullName = stage.CompN?.lowercase() ?: ""
                                        val query = searchQuery.lowercase()
                                        
                                        team1Name.contains(query) || 
                                        team2Name.contains(query) ||
                                        competitionName.contains(query) ||
                                        competitionFullName.contains(query)
                                    }
                                }
                                
                                if (showLiveOnly) {
                                    filteredEvents = filteredEvents.filter { event ->
                                        event.isLive()
                                    }
                                }
                                
                                com.zachvlat.footballscores.data.model.Stage(
                                    Sid = stage.Sid ?: "",
                                    Snm = stage.Snm ?: "",
                                    Scd = stage.Scd ?: "",
                                    Cnm = stage.Cnm ?: "",
                                    CnmT = stage.CnmT ?: "",
                                    Csnm = stage.Csnm ?: "",
                                    Ccd = stage.Ccd ?: "",
                                    CompId = stage.CompId ?: "",
                                    CompN = stage.CompN ?: "",
                                    CompUrlName = stage.CompUrlName ?: "",
                                    CompD = stage.CompD ?: "",
                                    CompST = stage.CompST ?: "",
                                    Scu = stage.Scu ?: 0,
                                    badgeUrl = stage.badgeUrl,
                                    firstColor = stage.firstColor ?: "",
                                    Events = filteredEvents
                                )
                            }.filter { it.Events.isNotEmpty() }
                            
                            if (filteredStages.isEmpty()) {
                                val message = when {
                                    showLiveOnly && searchQuery.isNotBlank() -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    showLiveOnly -> 
                                        "No live ${sportType.displayName.lowercase()} matches found for selected date"
                                    searchQuery.isNotBlank() -> 
                                        "No ${sportType.displayName.lowercase()} matches found for \"${searchQuery}\""
                                    else -> 
                                        "No ${sportType.displayName.lowercase()} matches found for selected date"
                                }
                                EmptyState(
                                    message = message,
                                    onRefresh = onRefresh
                                )
                            } else {
                                MatchList(stages = filteredStages, viewModel as HockeyViewModel)
                            }
                        }
                        is HockeyUiState.Error -> {
                            ErrorMessage(
                                message = uiState.message,
                                onRefresh = onRefresh
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDateDialog) {
        DateSelectionDialog(
            onDateSelected = { date ->
                onLoadScoresForDate(date)
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false }
        )
    }
    
    // Match Detail Popup (only for soccer)
    if (sportType == SportType.SOCCER) {
        when (val state = matchDetailState) {
            is MatchDetailState.Success -> {
                MatchDetailPopup(
                    matchDetail = state.matchDetail,
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
            else -> { /* Hidden state - do nothing */ }
        }
    }
}

@Composable
fun MatchList(stages: List<com.zachvlat.footballscores.data.model.Stage>, viewModel: Any? = null) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        stages.forEach { stage ->
            if (stage.Events.isNotEmpty()) {
                // Competition Header
                item {
                    CompetitionHeader(stage = stage)
                }
                
                // Matches for this competition
                items(stage.Events) { event ->
                    MatchCard(event = event)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
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
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                sdf.format(Date(millis))
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
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
                                onDateSelected(sdf.format(Date(millis)))
                            }
                        }
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