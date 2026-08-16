package com.hyper.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyper.music.R
import com.hyper.music.model.Playlist
import com.hyper.music.model.Song
import com.hyper.music.ui.theme.TextSize
import com.hyper.music.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

class MusicViewModel : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _textSize = MutableStateFlow(TextSize.Medium)
    val textSize: StateFlow<TextSize> = _textSize.asStateFlow()

    private val _allSongs = MutableStateFlow(
        listOf(
            Song("1", "Neon Horizon", "Cyber Mages", R.drawable.album_art_synthwave_1786889191503, 150, true),
            Song("2", "Electric Dream", "Cyber Mages", R.drawable.album_art_synthwave_1786889191503, 120, false),
            Song("3", "Digital Sunset", "Cyber Mages", R.drawable.album_art_synthwave_1786889191503, 40, true),
            Song("4", "Synth City", "Cyber Mages", R.drawable.album_art_synthwave_1786889191503, 10, false),
            Song("5", "Outrun The Night", "Cyber Mages", R.drawable.album_art_synthwave_1786889191503, 5, false),
            Song("6", "Rainy Afternoon", "Lofi Beats", R.drawable.album_art_lofi_1786889206026, 200, true),
            Song("7", "Coffee Shop Vibe", "Chill Masters", R.drawable.album_art_lofi_1786889206026, 180, true),
            Song("8", "Study Session", "Lofi Beats", R.drawable.album_art_lofi_1786889206026, 90, false),
            Song("9", "Retro Rush", "Pixel Warriors", R.drawable.app_icon_hyper_music_1786889116311, 300, true),
            Song("10", "Future Bass", "Electronic Flow", R.drawable.app_icon_hyper_music_1786889116311, 50, false)
        )
    )
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _customPlaylists = MutableStateFlow<List<Playlist>>(emptyList())

    val homePlaylists: StateFlow<List<Playlist>> = combine(_allSongs, _customPlaylists) { songs, custom ->
        val mostPlayed = Playlist("auto_most_played", "Most Played", songs.sortedByDescending { it.playCount }, R.drawable.app_icon_hyper_music_1786889116311, true)
        val favorites = Playlist("auto_fav", "Favorites", songs.filter { it.isFavorite }, R.drawable.album_art_lofi_1786889206026, true)
        
        val artistAutoPlaylists = songs.groupBy { it.artist }
            .filter { it.value.size >= 5 }
            .map { Playlist("auto_artist_${it.key}", "${it.key} Essentials", it.value, it.value.first().imageRes, true) }
            
        listOf(mostPlayed, favorites) + custom + artistAutoPlaylists
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    init {
        _currentSong.value = _allSongs.value.first()
    }

    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode }
    fun setTextSize(size: TextSize) { _textSize.value = size }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true
        _playbackProgress.value = 0f
        
        _allSongs.update { songs ->
            songs.map { if (it.id == song.id) it.copy(playCount = it.playCount + 1) else it }
        }
    }

    fun toggleFavorite(songId: String) {
        _allSongs.update { songs ->
            songs.map { if (it.id == songId) it.copy(isFavorite = !it.isFavorite) else it }
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
    }
}
