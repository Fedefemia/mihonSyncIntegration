package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Date

import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import eu.kanade.presentation.components.AppBar
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import eu.kanade.tachiyomi.ui.main.SyncViewModel

object SettingsSyncScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = navigator::pop,
                )
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                SyncSettingsContent()
            }
        }
    }
}

@Composable
private fun SyncSettingsContent(
    viewModel: SyncViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            text = "Configurazione Server",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = state.syncHost,
            onValueChange = { viewModel.updateHost(it) },
            label = { Text("Indirizzo Server (es. http://192.168.1.5:8000)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sincronizzazione Automatica",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = state.isSyncEnabled,
                onCheckedChange = { isEnabled ->
                    viewModel.toggleSync(isEnabled)
                }
            )
        }

        // --- SEZIONE INTERVALLO ---
        if (state.isSyncEnabled) {
            Column {
                Text(
                    text = "Intervallo aggiornamento: ${state.syncInterval} minuti",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = state.syncInterval.toFloat(),
                    onValueChange = { newValue ->
                        viewModel.updateInterval(newValue.toLong())
                    },
                    valueRange = 15f..1440f,
                    steps = 10
                )
            }
        }

        // --- SEZIONE AZIONI ---
        Button(
            onClick = { viewModel.forceSyncNow() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sincronizza Ora")
            }
        }

        if (state.lastSyncTime > 0) {
            Text(
                text = "Ultima sincronizzazione: ${Date(state.lastSyncTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
