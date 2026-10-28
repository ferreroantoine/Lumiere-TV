package fr.lumiere.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes

/** Texte d'en-tête d'un titre : nom, ligne d'informations, résumé. */
data class HeroText(val title: String, val meta: String?, val overview: String?)

/**
 * Bandeau haut : grande image de fond fondue dans le décor, titre, informations et résumé,
 * puis les actions passées en [actions].
 */
@Composable
fun Hero(
    imageUrl: String?,
    text: HeroText,
    modifier: Modifier = Modifier,
    height: Dp = 230.dp,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(height)) {
        Crossfade(imageUrl, label = "fond", modifier = Modifier.fillMaxSize()) { url ->
            if (url != null) Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(0.72f).align(Alignment.TopEnd).clip(LumiereShapes.Panel)) {
                    AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(0f to LumiereColors.Ink900.copy(alpha = 0.95f), 0.55f to Color.Transparent),
                        ),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0.6f to Color.Transparent, 1f to LumiereColors.Ink900.copy(alpha = 0.9f)),
                        ),
                    )
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 640.dp).padding(bottom = 8.dp),
        ) {
            Text(text.title, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (text.meta != null) {
                Text(text.meta, style = MaterialTheme.typography.titleSmall, color = LumiereColors.Brand200)
            }
            if (text.overview != null) {
                Text(
                    text.overview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LumiereColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actions()
        }
    }
}
