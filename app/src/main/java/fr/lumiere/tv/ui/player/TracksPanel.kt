package fr.lumiere.tv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.components.GlassSurface
import fr.lumiere.tv.ui.components.LumiereButton
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.glass

/** Piste audio proposée dans le panneau (ExoPlayer en lecture directe, session sinon). */
data class AudioChoice(val key: String, val label: String, val selected: Boolean, val onPick: () -> Unit)

/** Panneau latéral du lecteur, en verre épais : audio, sous-titres, décalage et recalage. */
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxHeight().width(520.dp)
            .glass(LumiereShapes.Panel, GlassVariant.Thick, elevation = 28.dp)
            .padding(24.dp),
    ) {
        item { SectionTitle("Audio", LumiereIcons.AudioLines) }
        audio.forEachIndexed { index, choice ->
            item(key = "a-${choice.key}") {
                ChoiceRow(choice.label, choice.selected, choice.onPick, if (index == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
        item { SectionTitle("Sous-titres", LumiereIcons.Captions) }
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
            item { SectionTitle("Décalage des sous-titres", LumiereIcons.RotateCcw) }
            item(key = "sync") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LumiereButton("− 0,25 s", onClick = { onNudge(-0.25) })
                    Text(
                        "%+.2f s".format(subtitles.offset).replace('.', ','),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    LumiereButton("+ 0,25 s", onClick = { onNudge(0.25) })
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
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = LumiereColors.Brand300, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = LumiereColors.Zinc300)
    }
}

/** Choix d'une liste : lentille et coche sur l'élément retenu, blanc au focus. */
@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = LumiereShapes.Inner,
        selected = selected,
        transparent = true,
        focusedScale = 1.02f,
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (selected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    LumiereIcons.Check,
                    contentDescription = "Choisi",
                    tint = if (focused) LumiereColors.Ink950 else LumiereColors.Brand300,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
