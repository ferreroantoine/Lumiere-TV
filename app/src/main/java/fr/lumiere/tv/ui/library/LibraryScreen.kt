package fr.lumiere.tv.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.LibraryItem
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.EmptyMessage
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.MediaRow
import fr.lumiere.tv.ui.components.PosterCard
import fr.lumiere.tv.ui.components.WideCard
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.home.episodeLabel
import fr.lumiere.tv.ui.navigation.Navigator

private fun LibraryItem.progress(): Float? =
    watch?.let { w -> w.duration?.takeIf { it > 0 }?.let { (w.position / it).toFloat() } }

/** Fichiers téléchargés sur le serveur, lisibles depuis la TV. */
@Composable
fun LibraryScreen(navigator: Navigator) {
    val vm = containerViewModel { LibraryViewModel(it.api) }
    val state by vm.state.collectAsStateWithLifecycle()
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (resumed) vm.reload()
        resumed = true
        onPauseOrDispose { }
    }

    when (val s = state) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(s.message, onRetry = vm::reload, onChangeServer = navigator::changeServer.takeIf { s.unreachable })
        is UiState.Ready -> {
            val content = s.value
            if (content.movies.isEmpty() && content.episodes.isEmpty()) {
                EmptyMessage("Votre bibliothèque est vide. Les téléchargements se lancent depuis Lumière sur le web.")
            } else {
                LibraryRows(content, navigator)
            }
        }
    }
}

@Composable
private fun LibraryRows(content: LibraryContent, navigator: Navigator) {
    val api = appContainer().api
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 48.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Bibliothèque", style = MaterialTheme.typography.headlineMedium) }
        if (content.movies.isNotEmpty()) {
            item(key = "films") {
                MediaRow("Films") {
                    items(content.movies, key = { it.id }) { item ->
                        PosterCard(
                            title = item.title,
                            imageUrl = item.posterUrl?.let(api::absoluteUrl),
                            progress = item.progress(),
                            onClick = { navigator.play(MediaRef.Library(item.id), item.titleId) },
                            onFocus = { navigator.setAmbient(item.posterUrl?.let(api::absoluteUrl)) },
                            modifier = if (item == content.movies.first()) Modifier.focusRequester(first) else Modifier,
                        )
                    }
                }
            }
        }
        if (content.episodes.isNotEmpty()) {
            item(key = "series") {
                MediaRow("Séries") {
                    items(content.episodes, key = { it.id }) { item ->
                        WideCard(
                            title = item.title,
                            subtitle = item.episodeLabel(),
                            imageUrl = (item.backdropUrl ?: item.posterUrl)?.let(api::absoluteUrl),
                            progress = item.progress(),
                            onClick = { navigator.play(MediaRef.Library(item.id), item.titleId) },
                            onFocus = { navigator.setAmbient(item.posterUrl?.let(api::absoluteUrl)) },
                            modifier = if (content.movies.isEmpty() && item == content.episodes.first()) Modifier.focusRequester(first) else Modifier,
                        )
                    }
                }
            }
        }
    }
}
