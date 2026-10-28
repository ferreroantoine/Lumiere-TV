package fr.lumiere.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.ReleaseInfo
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.glassRim

/** Caractéristiques d'une version, dans l'ordre : résolution, HDR, codec vidéo, audio, langues. */
fun releaseBadges(release: ReleaseInfo): List<String> =
    (listOf(release.resolution, release.hdr, release.videoCodec, release.audioCodec) + release.languages)
        .filterNotNull()
        .filter { it.isNotBlank() }
        .distinct()

/**
 * Badges de verre clair ; la résolution, en tête, est ambrée. [onLight] : posés sur un fond clair
 * (élément qui a le focus), ils passent en encre sombre.
 */
@Composable
fun ReleaseBadgeRow(release: ReleaseInfo, modifier: Modifier = Modifier, onLight: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        releaseBadges(release).forEachIndexed { index, badge ->
            Text(
                badge,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    onLight -> LumiereColors.Ink950
                    index == 0 && release.resolution != null -> LumiereColors.Brand200
                    else -> LumiereColors.Zinc200
                },
                modifier = Modifier
                    .background(if (onLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f), LumiereShapes.Pill)
                    .glassRim(LumiereShapes.Pill, edge = 0.3f)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
    }
}
