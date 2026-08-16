package com.hyper.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.theme.ThemeMode
import com.hyper.music.ui.theme.TextSize
import com.hyper.music.viewmodel.MusicViewModel

@Composable
fun SongOptionsDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEditInfo: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSetRingtone: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(text = { Text("Edit info") }, onClick = { onEditInfo(); onDismissRequest() })
        DropdownMenuItem(text = { Text("Share") }, onClick = { onShare(); onDismissRequest() })
        DropdownMenuItem(text = { Text("Set ringtone") }, onClick = { onSetRingtone(); onDismissRequest() })
        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); onDismissRequest() })
    }
}

@Composable
fun SettingsScreen(viewModel: MusicViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val gapless by viewModel.gaplessPlayback.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val sleepTimer by viewModel.sleepTimerDuration.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsHeader("Appearance")
        SettingsClickableItem(
            title = "Theme",
            subtitle = themeMode.name,
            onClick = { showThemeDialog = true }
        )
        SettingsClickableItem(
            title = "Text Size",
            subtitle = textSize.name,
            onClick = { showTextSizeDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsHeader("Playback")
        SettingsClickableItem(
            title = "Audio Quality",
            subtitle = audioQuality,
            onClick = { showAudioQualityDialog = true }
        )
        SettingsClickableItem(
            title = "Sleep Timer",
            subtitle = if (sleepTimer == 0) "Off" else "$sleepTimer minutes",
            onClick = { showSleepTimerDialog = true }
        )
        SettingsSwitchItem(
            title = "Gapless Playback",
            subtitle = "Eliminate pauses between tracks",
            checked = gapless,
            onCheckedChange = { viewModel.toggleGapless() }
        )
        SettingsClickableItem(
            title = "Equalizer",
            subtitle = "Adjust audio frequencies",
            onClick = { /* TODO: Open Equalizer */ }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsHeader("Downloads")
        SettingsSwitchItem(
            title = "Download over Wi-Fi only",
            subtitle = "Save mobile data",
            checked = wifiOnly,
            onCheckedChange = { viewModel.toggleWifiOnly() }
        )
        SettingsClickableItem(
            title = "Clear Cache",
            subtitle = "Free up storage space",
            onClick = { /* TODO */ }
        )
    }

    if (showThemeDialog) {
        SettingsDialog(
            title = "Theme",
            options = ThemeMode.entries.map { it.name },
            selectedOption = themeMode.name,
            onOptionSelected = { viewModel.setThemeMode(ThemeMode.valueOf(it)) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showTextSizeDialog) {
        SettingsDialog(
            title = "Text Size",
            options = TextSize.entries.map { it.name },
            selectedOption = textSize.name,
            onOptionSelected = { viewModel.setTextSize(TextSize.valueOf(it)) },
            onDismiss = { showTextSizeDialog = false }
        )
    }

    if (showAudioQualityDialog) {
        SettingsDialog(
            title = "Audio Quality",
            options = listOf("High", "Standard", "Low"),
            selectedOption = audioQuality,
            onOptionSelected = { viewModel.setAudioQuality(it) },
            onDismiss = { showAudioQualityDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SettingsDialog(
            title = "Sleep Timer",
            options = listOf("Off", "15 minutes", "30 minutes", "45 minutes", "60 minutes"),
            selectedOption = if (sleepTimer == 0) "Off" else "$sleepTimer minutes",
            onOptionSelected = { option ->
                val mins = when(option) {
                    "15 minutes" -> 15
                    "30 minutes" -> 30
                    "45 minutes" -> 45
                    "60 minutes" -> 60
                    else -> 0
                }
                viewModel.setSleepTimer(mins)
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsClickableItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null // Handled by Row click
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
