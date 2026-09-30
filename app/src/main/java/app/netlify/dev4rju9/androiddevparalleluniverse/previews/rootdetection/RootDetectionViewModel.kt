package app.netlify.dev4rju9.androiddevparalleluniverse.previews.rootdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.netlify.dev4rju9.rootdetection.DeviceStateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RootDetectionViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<RootDetectionResult> =
        MutableStateFlow(getInitialResult())

    val uiState: StateFlow<RootDetectionResult> =
        _uiState.asStateFlow()

    init {
        clear()
    }

    fun updateResult(result: DeviceStateUtils.EvaluationResult) {
        viewModelScope.launch {
            val signals = result.allSignals

            _uiState.emit(
                RootDetectionResult(
                    status = if (result.isFlagged) {
                        "Environment Untrusted"
                    } else {
                        "Environment Trusted"
                    },
                    flags = if (signals.isEmpty()) {
                        listOf("No detection signals")
                    } else {
                        signals
                    },
                    elapsedMs = result.elapsedMs,
                    loading = false
                )
            )
        }
    }

    fun setLoading() {
        viewModelScope.launch {
            _uiState.emit(
                _uiState.value.copy(
                    status = "Checking...",
                    flags = emptyList(),
                    loading = true,
                    elapsedMs = 0L
                )
            )
        }
    }

    fun clear() {
        viewModelScope.launch {
            _uiState.emit(getInitialResult())
        }
    }

    private fun getInitialResult(): RootDetectionResult {
        return RootDetectionResult(
            status = "Not Started",
            flags = emptyList(),
            loading = false,
            elapsedMs = 0L
        )
    }

}

data class RootDetectionResult(
    val status: String,
    val flags: List<String> = emptyList(),
    val loading: Boolean = false,
    val elapsedMs: Long = 0L
)
