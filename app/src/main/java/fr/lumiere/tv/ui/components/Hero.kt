package fr.lumiere.tv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.ui.theme.LegibleShadow
import fr.lumiere.tv.ui.theme.LumiereColors

/** Texte du bandeau : nom, informations (type, année, note, genres) et résumé. */
data class HeroText(
    val title: String,
    val kind: TitleKind?,
    val year: Int?,
    val rating: Double?,
    val extras: List<String>,
    val overview: String?,
)

/**
 * Bandeau de Lumière (features/home/Hero.tsx) : informations, très grand titre, résumé, puis les
 * actions passées en [actions]. L'illustration est peinte en plein cadre par le fond d'ambiance.
 */
@Composable
fun Hero(
    text: HeroText,
    modifier: Modifier = Modifier,
    height: Dp = 250.dp,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(height)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 620.dp).padding(bottom = 6.dp),
        ) {
            // Le texte change en fondu quand une autre affiche prend le focus ; les boutons restent.
            AnimatedContent(text, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "bandeau") { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetaRow(shown.kind, shown.year, shown.rating, shown.extras)
                    Text(
                        shown.title,
                        style = MaterialTheme.typography.displayMedium.copy(shadow = LegibleShadow),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (shown.overview != null) {
                        Text(
                            shown.overview,
                            style = MaterialTheme.typography.bodyLarge.copy(shadow = LegibleShadow),
                            color = LumiereColors.Zinc200,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            actions()
        }
    }
}
