package fr.lumiere.tv.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.PlaybackSession
import fr.lumiere.tv.player.ExoPlayerPort
import fr.lumiere.tv.player.subtitles.Cue
import fr.lumiere.tv.ui.components.GlassPanel
import fr.lumiere.tv.ui.components.GlassStyle
import fr.lumiere.tv.ui.components.LumiereButton
import fr.lumiere.tv.ui.components.ProgressLine
import fr.lumiere.tv.ui.containerViewModel
import fr.lumiere.tv.ui.formatClock
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LegibleShadow
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.Safe
import fr.lumiere.tv.ui.theme.glass
import fr.lumiere.tv.ui.theme.glassLens
import fr.lumiere.tv.ui.theme.glassProminent
import kotlinx.coroutines.delay
import java.util.Locale

private const val CONTROLS_VISIBLE_MS = 4_000L
private const val SEEK_STEP_SEC = 10.0

/** Lecteur plein écran piloté à la télécommande. */
@Composable
fun PlayerScreen(media: MediaRef, onExit: () -> Unit) {
    val key = remember(media) { media.toString() }
    val vm = containerViewModel(key = "player-$key") { PlayerViewModel(it, media) }
    val subs = containerViewModel(key = "subs-$key") { SubtitlesViewModel(it.api) }
    val state by vm.state.collectAsStateWithLifecycle()
    val subtitles by subs.state.collectAsStateWithLifecycle()

    var panelOpen by remember { mutableStateOf(false) }
    var controlsUntil by remember { mutableLongStateOf(System.currentTimeMillis() + CONTROLS_VISIBLE_MS) }
    var position by remember { mutableStateOf(0.0) }
    var cues by remember { mutableStateOf(emptyList<Cue>()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val rootFocus = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }

    LaunchedEffect(state.session?.sessionId) { state.session?.let(subs::attach) }
    LaunchedEffect(state.ended) { if (state.ended) onExit() }
    // Plus de panneau ni de carte : l'image reprend le focus (les touches du lecteur remarchent).
    val overlayShown = panelOpen || state.nextCountdown != null || state.error != null
    LaunchedEffect(overlayShown) { if (!overlayShown) runCatching { rootFocus.requestFocus() } }
    // Position et sous-titres rafraîchis quatre fois par seconde.
    LaunchedEffect(Unit) {
        while (true) {
            position = vm.controller.position
            cues = subs.cuesAt(position)
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    // Appli mise en arrière-plan : pause (et position envoyée).
    LifecycleStartEffect(Unit) { onStopOrDispose { if (!vm.controller.paused) vm.controller.pause() } }

    BackHandler(enabled = panelOpen) { panelOpen = false }

    fun showControls() { controlsUntil = System.currentTimeMillis() + CONTROLS_VISIBLE_MS }
    // Pause : seulement celle demandée ; un flux qui se charge (démarrage, saut) n'en est pas une.
    val paused = vm.controller.paused
    val loading = state.loading || vm.port.isBuffering
    val controlsVisible = now < controlsUntil || paused || loading

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            // Retour ferme le panneau dès le premier appui (sinon Compose retire d'abord le focus).
            .onPreviewKeyEvent { event ->
                if (panelOpen && event.key == Key.Back) {
                    if (event.type == KeyEventType.KeyUp) panelOpen = false
                    true
                } else {
                    false
                }
            }
            .focusRequester(rootFocus)
            .onFocusChanged { rootFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                // Touches du lecteur seulement quand l'image a le focus : une carte (épisode suivant,
                // erreur) ou le panneau gardent la navigation normale au pavé directionnel.
                if (!rootFocused || panelOpen || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause, Key.Spacebar -> {
                        vm.togglePause(); showControls(); true
                    }
                    Key.MediaPlay -> { vm.controller.resume(); showControls(); true }
                    Key.MediaPause -> { vm.controller.pause(); showControls(); true }
                    Key.DirectionLeft, Key.MediaRewind -> { vm.seekBy(-SEEK_STEP_SEC); showControls(); true }
                    Key.DirectionRight, Key.MediaFastForward -> { vm.seekBy(SEEK_STEP_SEC); showControls(); true }
                    Key.DirectionUp, Key.Menu -> { panelOpen = true; true }
                    Key.DirectionDown -> { showControls(); true }
                    else -> false
                }
            },
    ) {
        VideoSurface(vm.port)
        SubtitleOverlay(cues, raised = controlsVisible)

        if (controlsVisible) ControlsOverlay(state.session, position, state.duration, paused, loading)

        state.nextCountdown?.let { seconds ->
            NextEpisodeCard(
                label = state.session?.next?.label.orEmpty(),
                seconds = seconds,
                onPlay = vm.controller::playNextNow,
                onCancel = { vm.controller.cancelNext(); runCatching { rootFocus.requestFocus() } },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }

        state.error?.let { message ->
            ErrorCard(message, onExit, Modifier.align(Alignment.Center))
        }

        if (panelOpen) {
            TracksPanel(
                audio = audioChoices(vm, state.session),
                subtitles = subtitles,
                onSubtitle = subs::select,
                onNudge = subs::nudge,
                onAutoSync = subs::autoSync,
                onSearchOnline = { subs.searchOnline() },
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = Safe.Vertical, horizontal = 24.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(port: ExoPlayerPort) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                keepScreenOn = true
                isFocusable = false
                player = port.exoPlayer
            }
        },
        onRelease = { it.player = null },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Pistes audio : celles d'ExoPlayer en lecture directe (changement immédiat), celles de la session sinon. */
@Composable
private fun audioChoices(vm: PlayerViewModel, session: PlaybackSession?): List<AudioChoice> {
    session ?: return emptyList()
    if (session.mode != "direct") {
        return session.audioTracks.map { track ->
            AudioChoice(
                key = "s${track.index}",
                label = listOfNotNull(track.label, track.codec.uppercase(), track.channels?.let { "$it can." }).joinToString(" · "),
                selected = track.index == session.selectedAudioTrack,
                onPick = { vm.changeAudioTrack(track.index) },
            )
        }
    }
    val exo = vm.port.exoPlayer
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) { version++ }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener) }
    }
    return remember(version) {
        exo.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.flatMapIndexed { g, group ->
            (0 until group.length).filter(group::isTrackSupported).map { i ->
                val format = group.getTrackFormat(i)
                val language = format.language?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.FRENCH) }
                    ?.replaceFirstChar { it.uppercase() }
                AudioChoice(
                    key = "e$g-$i",
                    label = listOfNotNull(format.label ?: language ?: "Piste ${g + 1}", format.channelCount.takeIf { it > 0 }?.let { "$it can." })
                        .joinToString(" · "),
                    selected = group.isTrackSelected(i),
                    onPick = {
                        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                            .build()
                    },
                )
            }
        }
    }
}

/**
 * Commandes du lecteur : titre en haut à gauche, barre de verre en bas avec l'état (lecture ou
 * pause), le temps écoulé, la progression et la durée, puis les touches utiles.
 */
@Composable
private fun ControlsOverlay(session: PlaybackSession?, position: Double, duration: Double?, paused: Boolean, loading: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.6f), 0.25f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(horizontal = Safe.Horizontal, vertical = Safe.Vertical + 4.dp),
        ) {
            Text(
                session?.title ?: "Chargement…",
                style = MaterialTheme.typography.headlineMedium.copy(shadow = LegibleShadow),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session?.subtitle?.let {
                Text(it, style = MaterialTheme.typography.titleMedium.copy(shadow = LegibleShadow), color = LumiereColors.Zinc300)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(horizontal = Safe.Horizontal, vertical = Safe.Vertical),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .glass(LumiereShapes.Pill, GlassVariant.Regular)
                    .padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Box(
                    Modifier.size(44.dp).then(
                        if (paused) Modifier.glassProminent(LumiereShapes.Pill) else Modifier.glassLens(LumiereShapes.Pill),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (paused) LumiereIcons.Play else LumiereIcons.Pause,
                        contentDescription = if (paused) "En pause" else "Lecture",
                        tint = if (paused) LumiereColors.Ink950 else Color.White,
                        modifier = Modifier.padding(start = if (paused) 2.dp else 0.dp).size(18.dp),
                    )
                }
                Text(formatClock(position), style = MaterialTheme.typography.titleSmall, color = Color.White)
                ProgressLine(
                    duration?.takeIf { it > 0 }?.let { (position / it).toFloat() } ?: 0f,
                    Modifier.weight(1f),
                    height = 5.dp,
                )
                Text(duration?.let(::formatClock) ?: "--:--", style = MaterialTheme.typography.titleSmall, color = LumiereColors.Zinc300)
            }
            Text(
                when {
                    loading -> "Chargement…"
                    paused -> "En pause · OK : reprendre · Haut : pistes et sous-titres"
                    else -> "OK : pause · ◀ ▶ : 10 s · Haut : pistes et sous-titres"
                },
                style = MaterialTheme.typography.bodyMedium.copy(shadow = LegibleShadow),
                color = LumiereColors.Zinc300,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun NextEpisodeCard(label: String, seconds: Int, onPlay: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    GlassPanel(modifier = modifier.padding(horizontal = Safe.Horizontal, vertical = Safe.Vertical + 70.dp).widthIn(max = 440.dp), padding = 24.dp) {
        Text("Épisode suivant dans $seconds s", style = MaterialTheme.typography.titleLarge, color = Color.White)
        if (label.isNotBlank()) Text(label, color = LumiereColors.Zinc300, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LumiereButton("Lancer", onClick = onPlay, icon = LumiereIcons.SkipForward, style = GlassStyle.Primary, modifier = Modifier.focusRequester(focus))
            LumiereButton("Annuler", onClick = onCancel, icon = LumiereIcons.X)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(message) { runCatching { focus.requestFocus() } }
    GlassPanel(
        modifier = modifier.widthIn(max = 640.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalSpacing = 18.dp,
        padding = 32.dp,
    ) {
        Icon(LumiereIcons.CircleAlert, contentDescription = null, tint = LumiereColors.Error, modifier = Modifier.size(34.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
        LumiereButton("Retour", onClick = onExit, icon = LumiereIcons.ChevronLeft, style = GlassStyle.Primary, modifier = Modifier.focusRequester(focus))
    }
}
