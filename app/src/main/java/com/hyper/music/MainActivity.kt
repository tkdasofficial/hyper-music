package com.hyper.music

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyper.music.model.Playlist
import com.hyper.music.model.Song
import com.hyper.music.ui.PlayerScreen
import com.hyper.music.ui.SettingsScreen
import com.hyper.music.ui.SongOptionsDropdown
import com.hyper.music.ui.theme.MyApplicationTheme
import com.hyper.music.viewmodel.MusicViewModel

sealed class Screen {
    object Home : Screen()
    data class PlaylistDetail(val playlistId: String) : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            MyApplicationTheme(themeMode = themeMode) {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.loadLocalAudio(context)
        }
    }

    LaunchedEffect(Unit) {
        val reqPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val allGranted = reqPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.loadLocalAudio(context)
        } else {
            permissionLauncher.launch(reqPermissions)
        }
    }

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allSongs by viewModel.allSongs.collectAsState()
    val homePlaylists by viewModel.homePlaylists.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editPlaylistInfo by remember { mutableStateOf<Playlist?>(null) }
    var showMainMenu by remember { mutableStateOf(false) }

    val currentPlaylist = (currentScreen as? Screen.PlaylistDetail)?.let { detail ->
        homePlaylists.find { it.id == detail.playlistId }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search songs...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = when (currentScreen) {
                                    is Screen.Home -> "Hyper Music"
                                    is Screen.PlaylistDetail -> currentPlaylist?.title ?: "Playlist"
                                    is Screen.Settings -> "Settings"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = if (currentScreen is Screen.Home) 28.sp else 22.sp
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSearchExpanded) {
                            IconButton(onClick = { isSearchExpanded = false; searchQuery = "" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        } else if (currentScreen != Screen.Home) {
                            IconButton(onClick = { currentScreen = Screen.Home }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (isSearchExpanded) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else {
                            if (currentScreen is Screen.Home) {
                                IconButton(onClick = { isSearchExpanded = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                                Box {
                                    IconButton(onClick = { showMainMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                                    }
                                    DropdownMenu(
                                        expanded = showMainMenu,
                                        onDismissRequest = { showMainMenu = false }
                                    ) {
                                        DropdownMenuItem(text = { Text("Settings") }, onClick = { currentScreen = Screen.Settings; showMainMenu = false })
                                        DropdownMenuItem(text = { Text("Privacy Policy") }, onClick = { showMainMenu = false })
                                        DropdownMenuItem(text = { Text("Help & Support") }, onClick = { showMainMenu = false })
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (currentSong != null) {
                    BottomPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = playbackProgress,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.skipNext() },
                        onPrevious = { viewModel.skipPrevious() },
                        onClick = { isPlayerExpanded = true }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    is Screen.Home -> {
                        val displayedSongs = if (searchQuery.isNotBlank()) {
                            allSongs.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        } else {
                            allSongs
                        }
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (searchQuery.isBlank()) {
                                PlaylistsSection(
                                    playlists = homePlaylists,
                                    onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                                    onPlaylistClick = { currentScreen = Screen.PlaylistDetail(it.id) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            SongsListSection(
                                songs = displayedSongs,
                                onSongClick = { viewModel.playSong(it) },
                                onDeleteSong = { viewModel.deleteSong(it.id) },
                                onEditSongInfo = { /* TODO */ },
                                onShareSong = { /* TODO */ },
                                onSetRingtoneSong = { /* TODO */ }
                            )
                        }
                    }
                    is Screen.PlaylistDetail -> {
                        if (currentPlaylist != null) {
                            com.hyper.music.ui.PlaylistScreen(
                                playlist = currentPlaylist,
                                onSongClick = { viewModel.playSong(it) },
                                onEditClick = { editPlaylistInfo = currentPlaylist },
                                onSongMoreClick = { /* Handled in PlaylistScreen directly */ },
                                onDeleteSong = { viewModel.deleteSong(it.id) },
                                onEditSongInfo = { /* TODO */ },
                                onShareSong = { /* TODO */ },
                                onSetRingtoneSong = { /* TODO */ }
                            )
                        }
                    }
                    is Screen.Settings -> {
                        SettingsScreen(viewModel)
                    }
                }
            }
        }

        // Full Screen Player Overlay
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (currentSong != null) {
                PlayerScreen(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    progress = playbackProgress,
                    onProgressChange = { viewModel.updateProgress(it) },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() },
                    onPrevious = { viewModel.skipPrevious() },
                    onFavoriteToggle = { viewModel.toggleFavorite(currentSong!!.id) },
                    onClose = { isPlayerExpanded = false },
                    onDelete = { viewModel.deleteSong(currentSong!!.id) },
                    onEditInfo = { /* TODO */ },
                    onShare = { /* TODO */ },
                    onSetRingtone = { /* TODO */ }
                )
            }
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name -> 
                    viewModel.createPlaylist(name)
                    showCreatePlaylistDialog = false
                }
            )
        }

        if (editPlaylistInfo != null) {
            var newName by remember { mutableStateOf(editPlaylistInfo!!.title) }
            AlertDialog(
                onDismissRequest = { editPlaylistInfo = null },
                title = { Text("Edit Playlist") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.updatePlaylist(editPlaylistInfo!!.id, newName)
                        editPlaylistInfo = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editPlaylistInfo = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Playlist Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(text) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PlaylistsSection(playlists: List<Playlist>, onCreatePlaylistClick: () -> Unit, onPlaylistClick: (Playlist) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onCreatePlaylistClick) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New")
            }
        }
        
        BoxWithConstraints {
            val itemWidth = maxWidth / 2.5f
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(playlists) { playlist ->
                    Card(
                        modifier = Modifier
                            .width(itemWidth)
                            .aspectRatio(1f)
                            .clickable { onPlaylistClick(playlist) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (playlist.imageRes != null) {
                                Image(
                                    painter = painterResource(id = playlist.imageRes),
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                            }
                            Text(
                                text = playlist.title,
                                color = if (playlist.imageRes != null) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongsListSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onEditSongInfo: (Song) -> Unit,
    onShareSong: (Song) -> Unit,
    onSetRingtoneSong: (Song) -> Unit
) {
    var expandedSongId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "All Songs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(songs) { song ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSongClick(song) }
                            .padding(vertical = 6.dp)
                    ) {
                        if (song.imageUri != null) {
                            AsyncImage(
                                model = song.imageUri,
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.app_icon_hyper_music_1786889116311),
                                fallback = painterResource(id = R.drawable.app_icon_hyper_music_1786889116311),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else if (song.imageRes != null) {
                            Image(
                                painter = painterResource(id = song.imageRes),
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(onClick = { expandedSongId = song.id }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            SongOptionsDropdown(
                                expanded = expandedSongId == song.id,
                                onDismissRequest = { expandedSongId = null },
                                onEditInfo = { onEditSongInfo(song) },
                                onDelete = { onDeleteSong(song) },
                                onShare = { onShareSong(song) },
                                onSetRingtone = { onSetRingtoneSong(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .clickable { onClick() }, 
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                // The Album Art
                if (song.imageUri != null) {
                    AsyncImage(
                        model = song.imageUri,
                        contentDescription = "Current Song",
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.app_icon_hyper_music_1786889116311),
                        fallback = painterResource(id = R.drawable.app_icon_hyper_music_1786889116311),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                } else if (song.imageRes != null) {
                    Image(
                        painter = painterResource(id = song.imageRes),
                        contentDescription = "Current Song",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                
                // The circular progress ring
                val progressColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val diameterOffset = strokeWidth / 2
                    val arcSize = size.width - strokeWidth
                    
                    // Background Track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(diameterOffset, diameterOffset),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Progress Indicator
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(diameterOffset, diameterOffset),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious, 
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext, 
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
