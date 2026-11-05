package com.zachvlat.footballscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.model.LineupsResponse
import com.zachvlat.footballscores.data.model.LiveScoresResponse
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import com.zachvlat.footballscores.data.repository.LiveScoresRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun todayDateString(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

class ScoresViewModel(private val sport: String) : ViewModel() {

    private val _uiState = MutableStateFlow<ScoresUiState>(ScoresUiState.Loading)
    val uiState: StateFlow<ScoresUiState> = _uiState.asStateFlow()

    private val _currentDate = MutableStateFlow(todayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedMatchId = MutableStateFlow<String?>(null)

    private val _matchDetailState = MutableStateFlow<MatchDetailState>(MatchDetailState.Hidden)
    val matchDetailState: StateFlow<MatchDetailState> = _matchDetailState.asStateFlow()

    init {
        loadScoresForDate(todayDateString())
    }

    fun loadScoresForDate(dateString: String) {
        _currentDate.value = dateString
        load(dateString, showLoading = true)
    }

    fun refresh() {
        load(_currentDate.value, showLoading = false)
    }

    private fun load(dateString: String, showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _uiState.value = ScoresUiState.Loading else _isRefreshing.value = true

            val result = LiveScoresRepository.getScores(sport, dateString)
            _isRefreshing.value = false
            if (dateString != _currentDate.value) return@launch

            result.fold(
                onSuccess = { _uiState.value = ScoresUiState.Success(it) },
                onFailure = { error ->
                    if (showLoading) {
                        _uiState.value = ScoresUiState.Error(error.message ?: "Failed to load scores for selected date")
                    }
                }
            )
        }
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

            val event = (_uiState.value as? ScoresUiState.Success)
                ?.response?.Stages?.flatMap { it.Events }?.find { it.Eid == matchId }

            val detailCall = async { LiveScoresRepository.getMatchDetails(sport, matchId) }
            val lineupsCall = async { LiveScoresRepository.getLineups(sport, matchId) }
            val matchDetailResult = detailCall.await()
            val lineupsResult = lineupsCall.await()

            if (_selectedMatchId.value != matchId) return@launch

            val matchDetail = matchDetailResult.getOrNull()
            val lineups = lineupsResult.getOrNull()
            _matchDetailState.value = if (matchDetail != null || lineups != null) {
                MatchDetailState.Success(matchDetail = matchDetail, lineups = lineups, event = event)
            } else {
                val error = matchDetailResult.exceptionOrNull() ?: lineupsResult.exceptionOrNull()
                MatchDetailState.Error(error?.message ?: "Failed to load match details")
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

sealed class ScoresUiState {
    object Loading : ScoresUiState()
    data class Success(val response: LiveScoresResponse) : ScoresUiState()
    data class Error(val message: String) : ScoresUiState()
}
