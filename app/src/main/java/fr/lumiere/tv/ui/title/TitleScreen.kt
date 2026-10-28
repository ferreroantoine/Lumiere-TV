package fr.lumiere.tv.ui.title

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import fr.lumiere.tv.data.api.EpisodeEntry
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.RemoteFile
import fr.lumiere.tv.data.api.TitleDetails
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.ReleaseBadgeRow
import fr.lumiere.tv.ui.components.WideCard
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.formatRuntime
import fr.lumiere.tv.ui.formatSize
import fr.lumiere.tv.ui.navigation.Navigator
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.Safe
import fr.lumiere.tv.ui.theme.lumiereChipColors

/** Ligne d'informations de la fiche : « 2025 · 1 h 52 · ★ 7,9 · Science-Fiction · Drame ». */
private fun TitleDetails.infoLine(): String = buildList {
    year?.let { add(it.toString()) }
    runtime?.takeIf { it > 0 }?.let { add(formatRuntime(it)) }
    rating?.takeIf { it > 0 }?.let { add("★ " + "%.1f".format(it).replace('.', ',')) }
    addAll(genres.take(3))
}.joinToString(" · ")

@Composable
fun TitleScreen(titleId: Int, autoPlay: Boolean, navigator: Navigator) {
    val vm = containerViewModel(key = "title-$titleId") { TitleViewModel(it.api, titleId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var autoPlayDone by rememberSaveable { mutableStateOf(!autoPlay) }
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
            val action = playAction(s.value)
            LaunchedEffect(Unit) {
                if (!autoPlayDone) {
                    autoPlayDone = true
                    if (action is PlayAction.Play) navigator.play(action.media, titleId)
                }
            }
            TitleContent(s.value, action, vm, navigator)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TitleContent(details: TitleDetails, action: PlayAction, vm: TitleViewModel, navigator: Navigator) {
    val api = appContainer().api
    val versions by vm.versions.collectAsStateWithLifecycle()
    val season by vm.season.collectAsStateWithLifecycle()
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
    BackHandler(enabled = versions != null) { vm.closeVersions() }

    Box(Modifier.fillMaxSize().background(LumiereColors.Ink900)) {
        val backdrop = (details.backdropUrl ?: details.posterUrl)?.let(api::absoluteUrl)
        if (backdrop != null) {
            AsyncImage(backdrop, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to LumiereColors.Ink900, 0.45f to LumiereColors.Ink900.copy(alpha = 0.85f), 1f to Color.Transparent),
            ),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to LumiereColors.Ink900)))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = Safe.Horizontal, vertical = Safe.Vertical + 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 680.dp)) {
                    Text(details.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(details.infoLine(), style = MaterialTheme.typography.titleSmall, color = LumiereColors.Brand200)
                    details.tagline?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, color = LumiereColors.Text)
                    }
                    details.overview?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = LumiereColors.TextMuted, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(
                            onClick = { (action as? PlayAction.Play)?.let { navigator.play(it.media, details.id) } },
                            enabled = action is PlayAction.Play,
                            modifier = Modifier.focusRequester(playFocus),
                        ) { Text((action as? PlayAction.Play)?.label ?: "Lecture") }
                        if (details.kind == TitleKind.MOVIE && details.files.isNotEmpty()) {
                            OutlinedButton(onClick = { vm.openVersions(VersionsRequest(details.title, null, null)) }) {
                                Text("Versions (${details.files.size})")
                            }
                        }
                    }
                    if (action is PlayAction.Disabled) {
                        Text(action.message, color = LumiereColors.Error, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (details.kind == TitleKind.SERIES && details.seasons.isNotEmpty()) {
                item(key = "seasons") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                        items(details.seasons, key = { it.key }) { s ->
                            FilterChip(
                                selected = season == s.number,
                                onClick = { vm.selectSeason(s.number) },
                                colors = lumiereChipColors(),
                            ) { Text(s.name) }
                        }
                    }
                }
                item(key = "episodes") {
                    val episodes = details.seasons.firstOrNull { it.number == season }?.episodes.orEmpty()
                    Column { EpisodeRow(details, episodes, navigator, onVersions = { ep ->
                        vm.openVersions(VersionsRequest("${details.title} · S${ep.season} É${ep.episode}", ep.season, ep.episode))
                    }) }
                }
            }
        }

        versions?.let { panel ->
            VersionsSidePanel(panel, onPick = { file ->
                vm.closeVersions()
                navigator.play(file.mediaRef(), details.id)
            }, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun EpisodeRow(details: TitleDetails, episodes: List<EpisodeEntry>, navigator: Navigator, onVersions: (EpisodeEntry) -> Unit) {
    val api = appContainer().api
    Text(
        "OK : lecture de l'épisode · appui long : choisir une version",
        style = MaterialTheme.typography.bodySmall,
        color = LumiereColors.TextMuted,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)) {
        items(episodes, key = { it.key }) { ep ->
            val watch = ep.files.firstNotNullOfOrNull { it.availability.watch }
            WideCard(
                title = "É${ep.episode} · ${ep.title}",
                subtitle = ep.runtime?.takeIf { it > 0 }?.let(::formatRuntime),
                imageUrl = (ep.stillUrl ?: details.backdropUrl)?.let(api::absoluteUrl),
                progress = watch?.let { w -> w.duration?.takeIf { it > 0 }?.let { (w.position / it).toFloat() } },
                onClick = {
                    if (ep.season != null && ep.episode != null) navigator.play(MediaRef.Episode(details.id, ep.season, ep.episode), details.id)
                },
                onLongClick = { onVersions(ep) },
            )
        }
    }
}

/** Panneau latéral listant les versions proposées par les extensions. */
@Composable
private fun VersionsSidePanel(panel: VersionsPanel, onPick: (RemoteFile) -> Unit, modifier: Modifier = Modifier) {
    val first = remember { FocusRequester() }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxHeight().width(560.dp)
            .background(LumiereColors.Ink850, LumiereShapes.Panel)
            .padding(28.dp),
    ) {
        Text("Versions", style = MaterialTheme.typography.headlineSmall)
        Text(panel.request.label, style = MaterialTheme.typography.bodyMedium, color = LumiereColors.TextMuted)
        when (val s = panel.state) {
            UiState.Loading -> Text("Recherche des versions…", color = LumiereColors.TextMuted)
            is UiState.Error -> Text(s.message, color = LumiereColors.Error)
            is UiState.Ready -> {
                if (s.value.isEmpty()) {
                    Text(panel.notice ?: "Aucun fichier proposé pour l'instant.", color = LumiereColors.TextMuted)
                } else {
                    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(s.value, key = { it.path }) { file ->
                            VersionItem(file, onClick = { onPick(file) }, modifier = if (file == s.value.first()) Modifier.focusRequester(first) else Modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionItem(file: RemoteFile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(LumiereShapes.Card),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = LumiereColors.Ink800,
            focusedContainerColor = LumiereColors.Ink600,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(16.dp)) {
            ReleaseBadgeRow(file.release)
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val extra = listOfNotNull(
                file.size?.let(::formatSize),
                file.label,
                if (file.availability.libraryItemId != null) "Dans la bibliothèque" else null,
            )
            if (extra.isNotEmpty()) {
                Text(extra.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LumiereColors.TextMuted, maxLines = 2)
            }
        }
    }
}
