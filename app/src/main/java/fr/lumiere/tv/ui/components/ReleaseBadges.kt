package fr.lumiere.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.ReleaseInfo
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes

/** Caractéristiques d'une version, dans l'ordre : résolution, HDR, codec vidéo, audio, langues. */
fun releaseBadges(release: ReleaseInfo): List<String> =
    (listOf(release.resolution, release.hdr, release.videoCodec, release.audioCodec) + release.languages)
        .filterNotNull()
        .filter { it.isNotBlank() }
        .distinct()

@Composable
fun ReleaseBadgeRow(release: ReleaseInfo, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        releaseBadges(release).forEach { badge ->
            Text(
                badge,
                style = MaterialTheme.typography.labelMedium,
                color = LumiereColors.Brand200,
                modifier = Modifier
                    .background(LumiereColors.Ink700, LumiereShapes.Inner)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
