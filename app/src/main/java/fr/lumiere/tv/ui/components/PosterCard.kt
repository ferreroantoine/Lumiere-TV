package fr.lumiere.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import fr.lumiere.tv.ui.theme.FocusRimBrush
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.glass

object CardSize {
    val PosterWidth = 128.dp
    val PosterHeight = 192.dp
    val WideWidth = 284.dp
    val WideHeight = 160.dp
}

/** Reflet spéculaire posé sur une image qui a le focus (`.media-frame::before` du web). */
private val FocusSheen = Brush.radialGradient(
    0f to Color.White.copy(alpha = 0.26f),
    0.4f to Color.White.copy(alpha = 0.05f),
    0.65f to Color.Transparent,
    center = Offset(0f, 0f),
    radius = 420f,
)

/**
 * Cadre d'image de Lumière (`.media-frame`) : tuile arrondie, fin liseré, et au focus un
 * agrandissement, un contour lumineux blanc et ambré, un halo et un reflet.
 */
@Composable
fun MediaFrame(
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(width, height).onFocusChanged { if (it.isFocused) onFocus() },
        shape = ClickableSurfaceDefaults.shape(LumiereShapes.Tile),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = LumiereColors.Ink800,
            contentColor = Color.White,
            focusedContainerColor = LumiereColors.Ink800,
            focusedContentColor = Color.White,
            pressedContainerColor = LumiereColors.Ink800,
            pressedContentColor = Color.White,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), shape = LumiereShapes.Tile),
            focusedBorder = Border(BorderStroke(2.5.dp, FocusRimBrush), shape = LumiereShapes.Tile),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(LumiereColors.Brand400.copy(alpha = 0.32f), 18.dp)),
        interactionSource = interaction,
    ) {
        content(focused)
        if (focused) Box(Modifier.fillMaxSize().background(FocusSheen))
    }
}

/** Affiche cliquable ; sans image, la tuile porte le titre. */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    progress: Float? = null,
    width: Dp = CardSize.PosterWidth,
    height: Dp = CardSize.PosterHeight,
) {
    MediaFrame(onClick, width, height, modifier, onFocus, onLongClick) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(12.dp),
        )
        if (imageUrl != null) {
            AsyncImage(model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (progress != null) {
            ProgressLine(progress, Modifier.align(Alignment.BottomCenter).padding(10.dp))
        }
    }
}

/**
 * Carte large de Lumière (reprise de lecture, bibliothèque, épisodes) : image en 16/9, titre et
 * sous-titre incrustés en bas, pastille de lecture en verre au focus, progression éventuelle.
 */
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
    MediaFrame(onClick, CardSize.WideWidth, CardSize.WideHeight, modifier, onFocus, onLongClick) { focused ->
        if (imageUrl != null) {
            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Black.copy(alpha = 0.88f),
                ),
            ),
        )
        if (focused) {
            Box(Modifier.align(Alignment.Center).size(54.dp).glass(LumiereShapes.Pill, GlassVariant.Clear, elevation = 0.dp)) {
                Icon(LumiereIcons.Play, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).padding(start = 3.dp).size(22.dp))
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LumiereColors.Zinc300, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (progress != null) ProgressLine(progress, Modifier.padding(top = 8.dp))
        }
    }
}
