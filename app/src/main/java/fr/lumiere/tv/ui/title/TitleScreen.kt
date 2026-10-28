package fr.lumiere.tv.ui.title

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import fr.lumiere.tv.data.api.EpisodeEntry
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.RemoteFile
import fr.lumiere.tv.data.api.TitleDetails
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.ui.UiState
import fr.lumiere.tv.ui.appContainer
import fr.lumiere.tv.ui.components.AmbientBackground
import fr.lumiere.tv.ui.components.ErrorScreen
import fr.lumiere.tv.ui.components.GlassChip
import fr.lumiere.tv.ui.components.GlassPanel
import fr.lumiere.tv.ui.components.GlassStyle
import fr.lumiere.tv.ui.components.GlassSurface
import fr.lumiere.tv.ui.components.HintText
import fr.lumiere.tv.ui.components.LoadingScreen
import fr.lumiere.tv.ui.components.LumiereButton
import fr.lumiere.tv.ui.components.MetaRow
import fr.lumiere.tv.ui.components.ReleaseBadgeRow
import fr.lumiere.tv.ui.components.WideCard
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.formatRuntime
import fr.lumiere.tv.ui.formatSize
import fr.lumiere.tv.ui.navigation.Navigator
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LegibleShadow
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.Safe
import fr.lumiere.tv.ui.theme.glass
import fr.lumiere.tv.ui.theme.glassRim

/** Compléments de la ligne d'informations : durée d'un film, nombre de saisons et d'épisodes d'une série. */
private fun TitleDetails.infoExtras(): List<String> = buildList {
    runtime?.takeIf { it > 0 }?.let { add(formatRuntime(it)) }
    if (kind == TitleKind.SERIES && seasons.isNotEmpty()) {
        add(if (seasons.size > 1) "${seasons.size} saisons" else "1 saison")
        val episodes = seasons.sumOf { it.episodes.size }
        if (episodes > 0) add(if (episodes > 1) "$episodes épisodes" else "1 épisode")
    }
}

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
        UiState.Loading -> Box {
            AmbientBackground(null)
            LoadingScreen()
        }
        is UiState.Error -> Box {
            AmbientBackground(null)
            ErrorScreen(s.message, onRetry = vm::reload, onChangeServer = navigator::changeServer.takeIf { s.unreachable })
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleContent(details: TitleDetails, action: PlayAction, vm: TitleViewModel, navigator: Navigator) {
    val api = appContainer().api
    val versions by vm.versions.collectAsStateWithLifecycle()
    val season by vm.season.collectAsStateWithLifecycle()
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
    BackHandler(enabled = versions != null) { vm.closeVersions() }

    Box(
        Modifier.fillMaxSize()
            // Retour ferme le panneau des versions dès le premier appui.
            .onPreviewKeyEvent { event ->
                if (versions != null && event.key == Key.Back) {
                    if (event.type == KeyEventType.KeyUp) vm.closeVersions()
                    true
                } else {
                    false
                }
            },
    ) {
        AmbientBackground(
            imageUrl = details.posterUrl?.let(api::absoluteUrl),
            backdropUrl = (details.backdropUrl ?: details.posterUrl)?.let(api::absoluteUrl),
        )

        // Sur TV, Compose fait défiler pour garder l'élément en focus à un tiers de l'écran : la fiche
        // s'ouvrirait titre coupé. Ici on ne défile que ce qu'il faut pour montrer l'élément.
        // Les rangées vont d'un bord à l'autre de l'écran ; la marge de sécurité est dans leur contenu.
        CompositionLocalProvider(LocalBringIntoViewSpec provides MinimalScroll) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = Safe.Vertical + 16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "header") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(horizontal = Safe.Horizontal),
                    ) {
                        StaticPoster(details.posterUrl?.let(api::absoluteUrl), details.title)
                        TitleHeader(details, action, playFocus, onPlay = { media -> navigator.play(media, details.id) }, onVersions = {
                            vm.openVersions(VersionsRequest(details.title, null, null))
                        })
                    }
                }
                if (details.kind == TitleKind.SERIES && details.seasons.isNotEmpty()) {
                    item(key = "seasons") {
                        // En arrivant sur les saisons, le focus va sur celle affichée.
                        val selectedSeason = remember { FocusRequester() }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = Safe.Horizontal),
                            modifier = Modifier.focusRestorer(selectedSeason).focusGroup(),
                        ) {
                            items(details.seasons, key = { it.key }) { s ->
                                GlassChip(
                                    s.name,
                                    selected = season == s.number,
                                    onClick = { vm.selectSeason(s.number) },
                                    badge = s.episodes.size.takeIf { it > 0 }?.toString(),
                                    modifier = if (season == s.number) Modifier.focusRequester(selectedSeason) else Modifier,
                                )
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
        }

        versions?.let { panel ->
            VersionsSidePanel(panel, onPick = { file ->
                vm.closeVersions()
                navigator.play(file.mediaRef(), details.id)
            }, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

/** Défilement minimal (celui des téléphones) : `calculateScrollDistance` par défaut. */
@OptIn(ExperimentalFoundationApi::class)
private val MinimalScroll = object : BringIntoViewSpec {}

/** Affiche de la fiche : cadre de Lumière, sans interaction. */
@Composable
private fun StaticPoster(url: String?, title: String) {
    Box(
        Modifier.size(width = 150.dp, height = 225.dp)
            .shadow(28.dp, LumiereShapes.Tile, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(LumiereShapes.Tile)
            .background(LumiereColors.Ink800)
            .glassRim(LumiereShapes.Tile, edge = 0.45f),
    ) {
        if (url != null) AsyncImage(url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/** En-tête de la fiche (features/title du web) : informations, titre, genres, accroche, résumé, actions. */
@Composable
private fun TitleHeader(
    details: TitleDetails,
    action: PlayAction,
    playFocus: FocusRequester,
    onPlay: (MediaRef) -> Unit,
    onVersions: () -> Unit,
) {
    // En-tête tenu dans un écran : sinon la liste défile à l'ouverture et masque le titre.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 620.dp)) {
        MetaRow(details.kind, details.year, details.rating, details.infoExtras())
        Text(
            details.title,
            style = MaterialTheme.typography.displaySmall.copy(shadow = LegibleShadow),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (details.genres.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                details.genres.take(4).forEach { genre ->
                    Text(
                        genre,
                        style = MaterialTheme.typography.labelMedium,
                        color = LumiereColors.Zinc200,
                        modifier = Modifier.glass(LumiereShapes.Pill, GlassVariant.Clear, elevation = 0.dp)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
        details.tagline?.takeIf { it.isNotBlank() }?.let {
            Text(
                "« $it »",
                style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic, shadow = LegibleShadow),
                color = LumiereColors.Brand300,
            )
        }
        details.overview?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge.copy(shadow = LegibleShadow),
                color = LumiereColors.Zinc200,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp)) {
            LumiereButton(
                (action as? PlayAction.Play)?.label ?: "Lecture",
                onClick = { (action as? PlayAction.Play)?.let { onPlay(it.media) } },
                icon = LumiereIcons.Play,
                style = GlassStyle.Primary,
                enabled = action is PlayAction.Play,
                modifier = Modifier.focusRequester(playFocus),
            )
            if (details.kind == TitleKind.MOVIE && details.files.isNotEmpty()) {
                LumiereButton("Versions (${details.files.size})", onClick = onVersions, icon = LumiereIcons.Film)
            }
        }
        if (action is PlayAction.Disabled) {
            Text(action.message, color = LumiereColors.ErrorText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EpisodeRow(details: TitleDetails, episodes: List<EpisodeEntry>, navigator: Navigator, onVersions: (EpisodeEntry) -> Unit) {
    val api = appContainer().api
    HintText("OK : lecture de l'épisode · appui long : choisir une version", Modifier.padding(start = Safe.Horizontal + 4.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = Safe.Horizontal),
        modifier = Modifier.focusRestorer().focusGroup(),
    ) {
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
    GlassPanel(
        modifier = modifier.fillMaxHeight().padding(vertical = Safe.Vertical, horizontal = 24.dp).width(560.dp),
        shape = LumiereShapes.Panel,
        verticalSpacing = 12.dp,
    ) {
        Text("Versions", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(panel.request.label, style = MaterialTheme.typography.bodyMedium, color = LumiereColors.TextMuted)
        when (val s = panel.state) {
            UiState.Loading -> Text("Recherche des versions…", color = LumiereColors.TextMuted)
            is UiState.Error -> Text(s.message, color = LumiereColors.ErrorText)
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
    GlassSurface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = LumiereShapes.Card, focusedScale = 1.02f) { focused ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(16.dp)) {
            ReleaseBadgeRow(file.release, onLight = focused)
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val extra = listOfNotNull(
                file.size?.let(::formatSize),
                file.label,
                if (file.availability.libraryItemId != null) "Dans la bibliothèque" else null,
            )
            if (extra.isNotEmpty()) {
                Text(
                    extra.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (focused) LumiereColors.Ink600 else LumiereColors.TextMuted,
                    maxLines = 2,
                )
            }
        }
    }
}
