package eu.kanade.tachiyomi.ui.main

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.sync.SyncManager
import eu.kanade.tachiyomi.data.sync.SyncPreferences

class SyncViewModel : ScreenModel {

    private val syncPreferences: SyncPreferences = Injekt.get()

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val host = syncPreferences.syncHost().get()
        val isEnabled = syncPreferences.syncEnabled().get()
        val lastSync = syncPreferences.lastSyncDate().get()

        _uiState.value = _uiState.value.copy(
            syncHost = host,
            isSyncEnabled = isEnabled,
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

    fun forceSyncNow() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                SyncManager.performImmediateSync()
                val now = System.currentTimeMillis()
                syncPreferences.lastSyncDate().set(now)
                _uiState.value = _uiState.value.copy(lastSyncTime = now, isLoading = false)
            } catch (e: Exception) {
                // Errore silenzioso
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

data class SyncUiState(
    val syncHost: String = "",
    val isSyncEnabled: Boolean = false,
    val lastSyncTime: Long = 0,
    val isLoading: Boolean = false
)
