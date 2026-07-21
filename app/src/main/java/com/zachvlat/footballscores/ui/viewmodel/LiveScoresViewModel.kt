package com.zachvlat.footballscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zachvlat.footballscores.data.model.LiveScoresResponse
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import com.zachvlat.footballscores.data.model.LineupsResponse
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.repository.LiveScoresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class LiveScoresViewModel(private val repository: LiveScoresRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LiveScoresUiState>(LiveScoresUiState.Loading)
    val uiState: StateFlow<LiveScoresUiState> = _uiState.asStateFlow()
    
    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _selectedMatchId = MutableStateFlow<String?>(null)
    val selectedMatchId: StateFlow<String?> = _selectedMatchId.asStateFlow()
    
    private val _matchDetailState = MutableStateFlow<MatchDetailState>(MatchDetailState.Hidden)
    val matchDetailState: StateFlow<MatchDetailState> = _matchDetailState.asStateFlow()
    
    init {
        loadTodayScores()
        startAutoRefresh()
    }
    
    fun loadTodayScores() {
        viewModelScope.launch {
            _uiState.value = LiveScoresUiState.Loading
            
            repository.getTodayLiveScores().fold(
                onSuccess = { response ->
                    _uiState.value = LiveScoresUiState.Success(response)
                },
                onFailure = { error ->
                    _uiState.value = LiveScoresUiState.Error(
                        message = error.message ?: "Unknown error occurred"
                    )
                }
            )
        }
    }
    
    fun loadScoresForDate(dateString: String) {
        _currentDate.value = dateString
        loadScoresForDateInternal(dateString, showLoading = true)
    }
    
    fun refresh() {
        loadScoresForDateInternal(_currentDate.value, showLoading = false)
    }
    
    private fun loadScoresForDateInternal(dateString: String, showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = LiveScoresUiState.Loading
            } else {
                _isRefreshing.value = true
            }
            
            repository.getLiveScoresForDate(dateString).fold(
                onSuccess = { response ->
                    _uiState.value = LiveScoresUiState.Success(response)
                    _isRefreshing.value = false
                },
                onFailure = { error ->
                    if (showLoading) {
                        _uiState.value = LiveScoresUiState.Error(
                            message = error.message ?: "Failed to load scores for selected date"
                        )
                    }
                    _isRefreshing.value = false
                }
            )
        }
    }
    
    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60000)
                if (_currentDate.value == getTodayDateString()) {
                    refresh()
                }
            }
        }
    }
    
    fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        return dateFormat.format(Date())
    }
    
    fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        return dateFormat.format(calendar.time)
    }
    
    fun getTomorrowDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        return dateFormat.format(calendar.time)
    }
    
    fun onMatchClick(matchId: String) {
        _selectedMatchId.value = matchId
        loadMatchDetails(matchId)
    }
    
    fun dismissMatchDetail() {
        _selectedMatchId.value = null
        _matchDetailState.value = MatchDetailState.Hidden
    }
    
    private fun loadMatchDetails(matchId: String) {
        viewModelScope.launch {
            _matchDetailState.value = MatchDetailState.Loading

            val currentState = _uiState.value
            val event = if (currentState is LiveScoresUiState.Success) {
                currentState.response.Stages.flatMap { it.Events }.find { it.Eid == matchId }
            } else null

            val matchDetailResult = repository.getMatchDetails(matchId)
            val lineupsResult = repository.getLineups(matchId)

            val matchDetail = matchDetailResult.getOrNull()
            val lineups = lineupsResult.getOrNull()

            if (matchDetail != null || lineups != null) {
                _matchDetailState.value = MatchDetailState.Success(
                    matchDetail = matchDetail,
                    lineups = lineups,
                    event = event
                )
            } else {
                val error = matchDetailResult.exceptionOrNull() ?: lineupsResult.exceptionOrNull()
                _matchDetailState.value = MatchDetailState.Error(
                    message = error?.message ?: "Failed to load match details"
                )
            }
        }
    }
}

sealed class MatchDetailState {
    object Hidden : MatchDetailState()
    object Loading : MatchDetailState()
    data class Success(
        val matchDetail: MatchDetailResponse?,
        val lineups: LineupsResponse?,
        val event: Event?
    ) : MatchDetailState()
    data class Error(val message: String) : MatchDetailState()
}

sealed class LiveScoresUiState {
    object Loading : LiveScoresUiState()
    data class Success(val response: LiveScoresResponse) : LiveScoresUiState()
    data class Error(val message: String) : LiveScoresUiState()
}

class LiveScoresViewModelFactory(private val repository: LiveScoresRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiveScoresViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LiveScoresViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}