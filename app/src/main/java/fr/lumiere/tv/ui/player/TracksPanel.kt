package fr.lumiere.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes

/** Piste audio proposée dans le panneau (ExoPlayer en lecture directe, session sinon). */
data class AudioChoice(val key: String, val label: String, val selected: Boolean, val onPick: () -> Unit)

/** Panneau latéral du lecteur : audio, sous-titres, décalage et recalage. */
@Composable
fun TracksPanel(
    audio: List<AudioChoice>,
    subtitles: SubtitlesState,
    onSubtitle: (String?) -> Unit,
    onNudge: (Double) -> Unit,
    onAutoSync: () -> Unit,
    onSearchOnline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxHeight().width(520.dp)
            .background(LumiereColors.Ink850, LumiereShapes.Panel)
            .padding(28.dp),
    ) {
        item { SectionTitle("Audio") }
        audio.forEachIndexed { index, choice ->
            item(key = "a-${choice.key}") {
                ChoiceRow(choice.label, choice.selected, choice.onPick, if (index == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
        item { SectionTitle("Sous-titres") }
        item(key = "s-none") {
            ChoiceRow("Aucun", subtitles.selected == null, { onSubtitle(null) }, if (audio.isEmpty()) Modifier.focusRequester(first) else Modifier)
        }
        subtitles.tracks.forEach { track ->
            item(key = "s-${track.id}") {
                val unavailable = track.id in subtitles.unavailable || !track.supported
                val label = buildList {
                    add(track.label)
                    track.provider?.let { add(it) }
                    if (track.hashMatch == true) add("fait pour ce fichier")
                    if (track.isForced) add("forcés")
                    if (unavailable) add("indisponible")
                }.joinToString(" · ")
                ChoiceRow(label, subtitles.selected == track.id, { if (!unavailable) onSubtitle(track.id) })
            }
        }
        if (subtitles.onlineAvailable) {
            item(key = "s-online") {
                ChoiceRow(if (subtitles.searching) "Recherche en ligne…" else "Chercher en ligne", false, onSearchOnline)
            }
        }
        if (subtitles.selected != null) {
            item { SectionTitle("Décalage des sous-titres") }
            item(key = "sync") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onNudge(-0.25) }) { Text("− 0,25 s") }
                    Text(
                        "%+.2f s".format(subtitles.offset).replace('.', ','),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(onClick = { onNudge(0.25) }) { Text("+ 0,25 s") }
                }
            }
            item(key = "auto") { ChoiceRow("Recaler automatiquement", false, onAutoSync) }
        }
        subtitles.message?.let { message ->
            item(key = "msg") { Text(message, style = MaterialTheme.typography.bodyMedium, color = LumiereColors.Brand200) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = LumiereColors.TextMuted, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(LumiereShapes.Inner),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) LumiereColors.Ink700 else LumiereColors.Ink850,
            contentColor = if (selected) LumiereColors.Brand200 else LumiereColors.Text,
            focusedContainerColor = LumiereColors.Brand400,
            focusedContentColor = LumiereColors.Ink950,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text((if (selected) "● " else "") + label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
