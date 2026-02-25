package com.standard.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val mood: String,
    val url: String
)

enum class Screen {
    Library,
    Search,
    Favorites
}

data class PlayerUiState(
    val tracks: List<Track> = emptyList(),
    val searchResults: List<Track> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val currentScreen: Screen = Screen.Library
)

class MusicViewModel : ViewModel() {
    private val catalog = listOf(
        Track("1", "Sunrise Drive", "Nova Lane", "Chill", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Track("2", "Night Frequency", "Atlas Echo", "Focus", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        Track("3", "Velvet Sky", "Kai River", "Lo-fi", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        Track("4", "City Pulse", "Mina Sol", "Workout", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
        Track("5", "Afterglow", "The Standard", "Evening", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3")
    )

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            tracks = catalog,
            searchResults = catalog
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun setScreen(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun toggleFavorite(track: Track) {
        val updated = _uiState.value.favorites.toMutableSet().apply {
            if (contains(track.id)) remove(track.id) else add(track.id)
        }
        _uiState.value = _uiState.value.copy(favorites = updated)
    }

    fun updateSearch(query: String) {
        val trimmed = query.trim()
        val results = if (trimmed.isBlank()) {
            catalog
        } else {
            catalog.filter {
                it.title.contains(trimmed, true) ||
                    it.artist.contains(trimmed, true) ||
                    it.mood.contains(trimmed, true)
            }
        }
        _uiState.value = _uiState.value.copy(searchResults = results)
    }

    fun play(track: Track) {
        _uiState.value = _uiState.value.copy(current = track, isPlaying = true)
    }

    fun pause() {
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val vm: MusicViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            MusicViewModel() as T
                    })
                    StandardApp(vm)
                }
            }
        }
    }
}

@Composable
private fun StandardApp(viewModel: MusicViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavTab(Screen.Library, state.currentScreen, "Library") { viewModel.setScreen(Screen.Library) }
                NavTab(Screen.Search, state.currentScreen, "Search") { viewModel.setScreen(Screen.Search) }
                NavTab(Screen.Favorites, state.currentScreen, "Favorites") { viewModel.setScreen(Screen.Favorites) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.currentScreen) {
                Screen.Library -> TrackList(
                    title = "Discover",
                    tracks = state.tracks,
                    favorites = state.favorites,
                    onFavorite = viewModel::toggleFavorite,
                    onPlay = { track ->
                        startPlayback(exoPlayer, track)
                        viewModel.play(track)
                    }
                )

                Screen.Search -> SearchScreen(
                    results = state.searchResults,
                    favorites = state.favorites,
                    onSearch = viewModel::updateSearch,
                    onFavorite = viewModel::toggleFavorite,
                    onPlay = { track ->
                        startPlayback(exoPlayer, track)
                        viewModel.play(track)
                    }
                )

                Screen.Favorites -> TrackList(
                    title = "Favorites",
                    tracks = state.tracks.filter { it.id in state.favorites },
                    favorites = state.favorites,
                    emptyText = "No favorites yet. Tap the heart icon on any song.",
                    onFavorite = viewModel::toggleFavorite,
                    onPlay = { track ->
                        startPlayback(exoPlayer, track)
                        viewModel.play(track)
                    }
                )
            }

            NowPlayingBar(
                track = state.current,
                isPlaying = state.isPlaying,
                onPlayPause = {
                    if (state.isPlaying) {
                        exoPlayer.pause()
                        viewModel.pause()
                    } else {
                        state.current?.let {
                            exoPlayer.play()
                            viewModel.play(it)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NavTab(screen: Screen, current: Screen, label: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = current == screen,
        onClick = onClick,
        icon = { Icon(Icons.Default.MusicNote, contentDescription = label) },
        label = { Text(label) }
    )
}

private fun startPlayback(player: ExoPlayer, track: Track) {
    player.setMediaItem(MediaItem.fromUri(track.url))
    player.prepare()
    player.play()
}

@Composable
private fun TrackList(
    title: String,
    tracks: List<Track>,
    favorites: Set<String>,
    onFavorite: (Track) -> Unit,
    onPlay: (Track) -> Unit,
    emptyText: String = "No tracks found."
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        if (tracks.isEmpty()) {
            Text(emptyText, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackCard(
                        track = track,
                        isFavorite = track.id in favorites,
                        onFavorite = { onFavorite(track) },
                        onPlay = { onPlay(track) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    results: List<Track>,
    favorites: Set<String>,
    onSearch: (String) -> Unit,
    onFavorite: (Track) -> Unit,
    onPlay: (Track) -> Unit
) {
    var query by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text("Search by title, artist, or mood") }
        )
        TrackList(
            title = "Search results",
            tracks = results,
            favorites = favorites,
            onFavorite = onFavorite,
            onPlay = onPlay
        )
    }
}

@Composable
private fun TrackCard(track: Track, isFavorite: Boolean, onFavorite: () -> Unit, onPlay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(track.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${track.artist} • ${track.mood}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "toggle favorite"
                )
            }
        }
    }
}

@Composable
private fun NowPlayingBar(track: Track?, isPlaying: Boolean, onPlayPause: () -> Unit) {
    if (track == null) return

    Spacer(Modifier.height(4.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Now Playing", style = MaterialTheme.typography.labelMedium)
                Text(track.title, fontWeight = FontWeight.Bold)
                Text(track.artist, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Text(if (isPlaying) " Pause" else " Play")
            }
        }
    }
}
