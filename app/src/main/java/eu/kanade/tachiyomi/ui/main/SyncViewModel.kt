package eu.kanade.tachiyomi.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import javax.inject.Inject
import eu.kanade.tachiyomi.data.sync.SyncManager
import eu.kanade.tachiyomi.data.sync.SyncPreferences

@HiltViewModel
class SyncViewModel @Inject constructor() : ViewModel() {

    private val syncPreferences: SyncPreferences = Injekt.get()

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val host = syncPreferences.syncHost().get()
        val isEnabled = syncPreferences.syncEnabled().get()
        val interval = syncPreferences.syncInterval().get()
        val lastSync = syncPreferences.lastSyncDate().get()

        _uiState.value = _uiState.value.copy(
            syncHost = host,
            isSyncEnabled = isEnabled,
            syncInterval = interval.toLong(),
            lastSyncTime = lastSync
        )
    }

    fun updateHost(newHost: String) {
        syncPreferences.syncHost().set(newHost)
        _uiState.value = _uiState.value.copy(syncHost = newHost)
    }

    fun toggleSync(enabled: Boolean) {
        syncPreferences.syncEnabled().set(enabled)
        _uiState.value = _uiState.value.copy(isSyncEnabled = enabled)
    }

    fun updateInterval(minutes: Long) {
        syncPreferences.syncInterval().set(minutes.toInt())
        _uiState.value = _uiState.value.copy(syncInterval = minutes)
    }

    fun forceSyncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                SyncManager.fetchInstructions()
                val now = System.currentTimeMillis()
                syncPreferences.lastSyncDate().set(now)
                _uiState.value = _uiState.value.copy(lastSyncTime = now, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

data class SyncUiState(
    val syncHost: String = "",
    val isSyncEnabled: Boolean = false,
    val syncInterval: Long = 15,
    val lastSyncTime: Long = 0,
    val isLoading: Boolean = false
)
