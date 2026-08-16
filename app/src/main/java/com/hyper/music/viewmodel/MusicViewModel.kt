package com.hyper.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.media.MediaPlayer
import android.net.Uri
import com.hyper.music.R
import com.hyper.music.model.Playlist
import com.hyper.music.model.Song
import com.hyper.music.ui.theme.TextSize
import com.hyper.music.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.util.UUID

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _textSize = MutableStateFlow(TextSize.Medium)
    val textSize: StateFlow<TextSize> = _textSize.asStateFlow()

    private val _audioQuality = MutableStateFlow("High")
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _wifiOnly = MutableStateFlow(true)
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _sleepTimerDuration = MutableStateFlow(0)
    val sleepTimerDuration: StateFlow<Int> = _sleepTimerDuration.asStateFlow()

    private var sleepTimerJob: Job? = null

    // Removed all mock data for a clean production state
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _customPlaylists = MutableStateFlow<List<Playlist>>(emptyList())

    val homePlaylists: StateFlow<List<Playlist>> = combine(_allSongs, _customPlaylists) { songs, custom ->
        val mostPlayedSongs = songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        val mostPlayedImage = mostPlayedSongs.firstOrNull()?.imageRes ?: R.drawable.ic_playlist_play
        val mostPlayed = Playlist("auto_most_played", "Most Played", mostPlayedSongs, mostPlayedImage, true)
        
        val favoriteSongs = songs.filter { it.isFavorite }.sortedByDescending { it.favoriteTimestamp }
        val favoriteImage = favoriteSongs.firstOrNull()?.imageRes ?: R.drawable.ic_playlist_love
        val favorites = Playlist("auto_fav", "Favorites", favoriteSongs, favoriteImage, true)
        
        val artistAutoPlaylists = songs.groupBy { it.artist }
            .filter { it.value.size >= 1 }
            .map { Playlist("auto_artist_${it.key}", "${it.key} Essentials", it.value, it.value.first().imageRes, true) }
            
        listOf(mostPlayed, favorites) + custom + artistAutoPlaylists
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun loadLocalAudio(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val audioList = mutableListOf<Song>()
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
            } else {
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.DURATION,
                android.provider.MediaStore.Audio.Media.DATA,
                android.provider.MediaStore.Audio.Media.ALBUM_ID
            )
            
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationColumn)
                    val data = cursor.getString(dataColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val contentUri = android.content.ContentUris.withAppendedId(collection, id)
                    val artworkUri = android.net.Uri.parse("content://media/external/audio/albumart/$albumId")

                    audioList.add(
                        Song(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            imageRes = R.drawable.app_icon_hyper_music_1786889116311,
                            imageUri = artworkUri.toString(),
                            dataUri = contentUri.toString(),
                            durationMs = duration
                        )
                    )
                }
            }
            _allSongs.value = audioList
            if (_currentSong.value == null && audioList.isNotEmpty()) {
                _currentSong.value = audioList.first()
            }
        }
    }

    init {
        _currentSong.value = _allSongs.value.firstOrNull()
    }

    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode }
    fun setTextSize(size: TextSize) { _textSize.value = size }
    fun setAudioQuality(quality: String) { _audioQuality.value = quality }
    fun toggleGapless() { _gaplessPlayback.value = !_gaplessPlayback.value }
    fun toggleWifiOnly() { _wifiOnly.value = !_wifiOnly.value }
    
    fun setSleepTimer(minutes: Int) {
        _sleepTimerDuration.value = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                _isPlaying.value = false
                _sleepTimerDuration.value = 0
            }
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.toFloat()
                        val dur = player.duration.toFloat()
                        if (dur > 0) {
                            _playbackProgress.value = pos / dur
                        }
                    }
                }
                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        val playing = !_isPlaying.value
        _isPlaying.value = playing
        mediaPlayer?.let {
            if (playing) it.start() else it.pause()
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true
        _playbackProgress.value = 0f
        
        mediaPlayer?.release()
        try {
            if (song.dataUri != null) {
                mediaPlayer = MediaPlayer.create(getApplication(), Uri.parse(song.dataUri))?.apply {
                    isLooping = _isLooping.value
                    setOnCompletionListener {
                        if (!isLooping) skipNext()
                    }
                    start()
                }
                startProgressLoop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _allSongs.update { songs ->
            songs.map { if (it.id == song.id) it.copy(playCount = it.playCount + 1) else it }
        }
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
        mediaPlayer?.isLooping = _isLooping.value
    }

    fun toggleFavorite(songId: String) {
        _allSongs.update { songs ->
            songs.map { if (it.id == songId) it.copy(isFavorite = !it.isFavorite, favoriteTimestamp = System.currentTimeMillis()) else it }
        }
    }

    fun deleteSong(songId: String) {
        _allSongs.update { it.filter { s -> s.id != songId } }
        if (_currentSong.value?.id == songId) {
            _currentSong.value = _allSongs.value.firstOrNull()
            _isPlaying.value = false
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            title = name,
            songs = emptyList(),
            imageRes = R.drawable.app_icon_hyper_music_1786889116311 
        )
        _customPlaylists.update { it + newPlaylist }
    }

    fun updatePlaylist(playlistId: String, newName: String) {
        _customPlaylists.update { playlists ->
            playlists.map { if (it.id == playlistId) it.copy(title = newName) else it }
        }
    }
    
    fun skipNext() {
        val songs = _allSongs.value
        val current = _currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < songs.size - 1) {
            playSong(songs[currentIndex + 1])
        } else if (songs.isNotEmpty()) {
            playSong(songs.first())
        }
    }
    
    fun skipPrevious() {
        val songs = _allSongs.value
        val current = _currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playSong(songs[currentIndex - 1])
        } else if (songs.isNotEmpty()) {
            playSong(songs.last())
        }
    }

    fun updateProgress(progress: Float) {
        _playbackProgress.value = progress
        mediaPlayer?.let {
            it.seekTo((it.duration * progress).toInt())
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
        progressJob?.cancel()
    }
}
