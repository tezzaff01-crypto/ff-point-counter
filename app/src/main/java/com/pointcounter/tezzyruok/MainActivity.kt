package com.pointcounter.tezzyruok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pointcounter.tezzyruok.ui.screens.*
import com.pointcounter.tezzyruok.ui.theme.PointCounterTheme
import com.pointcounter.tezzyruok.viewmodel.TournamentViewModel

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SETUP("Setup", Icons.Filled.Settings),
    MATCH("Match", Icons.Filled.SportsEsports),
    LEADERBOARD("Klasemen", Icons.Filled.Leaderboard),
    MOSTKILL("Most Kill", Icons.Filled.LocalFireDepartment),
    POSTER("Poster", Icons.Filled.Image),
    CERTIFICATE("Sertifikat", Icons.Filled.WorkspacePremium)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PointCounterTheme {
                AppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val vm: TournamentViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.state.collectAsState()
    val toast by vm.toast.collectAsState()
    var tab by remember { mutableStateOf(Tab.SETUP) }
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.tourneyName.ifBlank { stringResource(R.string.app_name) }) },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, maxLines = 1) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.SETUP -> SetupScreen(vm, state)
                Tab.MATCH -> MatchScreen(vm, state)
                Tab.LEADERBOARD -> LeaderboardScreen(vm, state)
                Tab.MOSTKILL -> MostKillScreen(vm, state)
                Tab.POSTER -> PosterScreen(vm, state)
                Tab.CERTIFICATE -> CertificateScreen(vm, state)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Semua Data?") },
            text = { Text("Semua tim, skor, dan pengaturan akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(onClick = { vm.resetAll(); showResetDialog = false }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            }
        )
    }
}
