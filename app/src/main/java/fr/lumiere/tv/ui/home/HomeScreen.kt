package fr.lumiere.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.lumiere.tv.data.api.LibraryItem
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.api.TitleSummary
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.GlassStyle
import fr.lumiere.tv.ui.components.Hero
import fr.lumiere.tv.ui.components.HeroText
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.LumiereButton
import fr.lumiere.tv.ui.components.MediaRow
import fr.lumiere.tv.ui.components.PosterCard
import fr.lumiere.tv.ui.components.WideCard
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.formatSize
import fr.lumiere.tv.ui.navigation.Navigator
import fr.lumiere.tv.ui.theme.LumiereIcons

/** Ligne d'informations d'un titre : « Film · 2024 · ★ 7,4 · Science-fiction ». */
fun TitleSummary.metaLine(): String = buildList {
    add(if (kind == TitleKind.MOVIE) "Film" else "Série")
    year?.let { add(it.toString()) }
    rating?.takeIf { it > 0 }?.let { add("★ " + "%.1f".format(it).replace('.', ',')) }
    genres.take(2).forEach { add(it) }
}.joinToString(" · ")

fun LibraryItem.episodeLabel(): String? =
    if (season != null && episode != null) "S$season É$episode" else null

/** Sous-titre d'un fichier de la bibliothèque, comme sur le web : « S1 É1 · 2160p · 7,98 Go ». */
fun LibraryItem.fileLine(): String =
    listOfNotNull(episodeLabel(), release.resolution, formatSize(size)).joinToString(" · ")

/** Texte du bandeau pour un titre du catalogue. */
fun TitleSummary.heroText(): HeroText = HeroText(title, kind, year, rating, genres.take(2), overview)

@Composable
fun HomeScreen(navigator: Navigator) {
    val vm = containerViewModel { HomeViewModel(it.api) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Rechargement à chaque retour sur l'écran (reprise de lecture à jour).
    var firstResume by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        if (!firstResume) vm.reload()
        firstResume = false
        onPauseOrDispose { }
    }

    when (val s = state) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(s.message, onRetry = vm::reload, onChangeServer = navigator::changeServer.takeIf { s.unreachable })
        is UiState.Ready -> HomeContentView(s.value, navigator)
    }
}

@Composable
private fun HomeContentView(content: HomeContent, navigator: Navigator) {
    val api = appContainer().api
    // Le bandeau présente le titre mis en avant, puis celui dont l'affiche a le focus ;
    // ses boutons agissent sur le titre présenté.
    var shown by remember(content.featured) { mutableStateOf(content.featured) }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(content.featured) { runCatching { playFocus.requestFocus() } }
    LaunchedEffect(shown) {
        navigator.setAmbient(shown?.posterUrl?.let(api::absoluteUrl))
        navigator.setBackdrop((shown?.backdropUrl ?: shown?.posterUrl)?.let(api::absoluteUrl))
    }

    Column(Modifier.fillMaxSize()) {
        shown?.let { title ->
            Hero(text = title.heroText()) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 6.dp)) {
                    LumiereButton(
                        "Lecture",
                        onClick = { navigator.playTitle(title.id) },
                        icon = LumiereIcons.Play,
                        style = GlassStyle.Primary,
                        modifier = Modifier.focusRequester(playFocus),
                    )
                    LumiereButton("Fiche", onClick = { navigator.openTitle(title.id) }, icon = LumiereIcons.Info)
                }
            }
        }
        HomeRows(content, navigator, api::absoluteUrl, onTitleFocus = { shown = it }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HomeRows(
    content: HomeContent,
    navigator: Navigator,
    url: (String) -> String,
    onTitleFocus: (TitleSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (content.resume.isNotEmpty()) {
            item(key = "resume") {
                MediaRow("Reprendre la lecture") {
                    items(content.resume, key = { it.mediaKey }) { item ->
                        WideCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            imageUrl = (item.backdropUrl ?: item.posterUrl)?.let(url),
                            progress = item.duration?.takeIf { it > 0 }?.let { (item.position / it).toFloat() },
                            onClick = { navigator.play(item.media, item.titleId) },
                        )
                    }
                }
            }
        }
        titleRow("movies", "Films populaires", content.movies, navigator, url, onTitleFocus)
        titleRow("series", "Séries populaires", content.series, navigator, url, onTitleFocus)
        if (content.library.isNotEmpty()) {
            item(key = "library") {
                MediaRow("Dans ma bibliothèque") {
                    items(content.library, key = { it.id }) { item ->
                        WideCard(
                            title = item.title,
                            subtitle = item.fileLine(),
                            imageUrl = (item.backdropUrl ?: item.posterUrl)?.let(url),
                            progress = item.watch?.let { w -> w.duration?.takeIf { it > 0 }?.let { (w.position / it).toFloat() } },
                            onClick = { navigator.play(MediaRef.Library(item.id), item.titleId) },
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.titleRow(
    key: String,
    label: String,
    titles: List<TitleSummary>,
    navigator: Navigator,
    url: (String) -> String,
    onFocus: (TitleSummary) -> Unit,
) {
    if (titles.isEmpty()) return
    item(key = key) {
        MediaRow(label) {
            items(titles, key = { it.id }) { title ->
                PosterCard(
                    title = title.title,
                    imageUrl = title.posterUrl?.let(url),
                    onClick = { navigator.openTitle(title.id) },
                    onFocus = { onFocus(title) },
                )
            }
        }
    }
}
