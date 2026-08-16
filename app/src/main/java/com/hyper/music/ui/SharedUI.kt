package com.hyper.music.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(mode.name)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Text Size", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        TextSize.entries.forEach { size ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = textSize == size,
                    onClick = { viewModel.setTextSize(size) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(size.name)
            }
        }
    }
}
