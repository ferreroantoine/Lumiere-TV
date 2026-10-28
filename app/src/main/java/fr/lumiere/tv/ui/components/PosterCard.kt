package fr.lumiere.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes

object CardSize {
    val PosterWidth = 128.dp
    val PosterHeight = 192.dp
    val WideWidth = 284.dp
    val WideHeight = 160.dp
}

/**
 * Affiche ou vignette cliquable : agrandie au focus, bordure ambrée et halo.
 * Sans image, une tuile sombre porte le titre.
 */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    progress: Float? = null,
    wide: Boolean = false,
    width: Dp = if (wide) CardSize.WideWidth else CardSize.PosterWidth,
    height: Dp = if (wide) CardSize.WideHeight else CardSize.PosterHeight,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(width, height).onFocusChanged { if (it.isFocused) onFocus() },
        shape = ClickableSurfaceDefaults.shape(LumiereShapes.Tile),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = LumiereColors.Ink800,
            focusedContainerColor = LumiereColors.Ink700,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, LumiereColors.Brand300), shape = LumiereShapes.Tile),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(LumiereColors.Brand400.copy(alpha = 0.45f), 16.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(12.dp),
            )
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (progress != null) {
                ProgressLine(progress, Modifier.align(Alignment.BottomCenter).padding(10.dp))
            }
        }
    }
}

/** Fine barre de progression de visionnage. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(5.dp).background(LumiereColors.Ink950.copy(alpha = 0.7f), LumiereShapes.Pill)) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(5.dp)
                .background(LumiereColors.Brand400, LumiereShapes.Pill),
        )
    }
}

/** Carte large avec titre et sous-titre dessous (reprise de lecture, épisodes). */
@Composable
fun WideCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    progress: Float? = null,
) {
    Column(modifier.width(CardSize.WideWidth)) {
        PosterCard(title, imageUrl, onClick, onFocus = onFocus, onLongClick = onLongClick, progress = progress, wide = true)
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LumiereColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
