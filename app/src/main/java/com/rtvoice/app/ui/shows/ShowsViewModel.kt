package com.rtvvoice.app.ui.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtvvoice.app.data.api.NewsRepository
import com.rtvvoice.app.data.api.Show
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShowsViewModel : ViewModel() {

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Ready(val shows: List<Show>) : State()
        data object Error : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    init {
        load()
        viewModelScope.launch {
            NewsRepository.editionChanges.collect { load() }
        }
    }

    fun load() {
        if (_state.value is State.Loading) return
        _state.value = State.Loading
        viewModelScope.launch {
            _state.value = runCatching { NewsRepository.shows() }
                .fold({ shows -> if (shows.isEmpty()) State.Error else State.Ready(shows) },
                    { State.Error })
        }
    }
}
