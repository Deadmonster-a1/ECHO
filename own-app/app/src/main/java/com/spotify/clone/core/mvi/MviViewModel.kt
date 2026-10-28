package com.spotify.clone.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface UiState
interface UiEvent
interface UiEffect

abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effectChannel = Channel<F>(Channel.BUFFERED)
    val effect = _effectChannel.receiveAsFlow()

    protected val currentState: S
        get() = _uiState.value

    abstract fun handleEvent(event: E)

    fun dispatch(event: E) {
        handleEvent(event)
    }

    protected fun setState(reducer: S.() -> S) {
        _uiState.update(reducer)
    }

    protected fun emitEffect(builder: () -> F) {
        val newEffect = builder()
        viewModelScope.launch {
            _effectChannel.send(newEffect)
        }
    }
}
