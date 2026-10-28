package fr.lumiere.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.LegibleShadow
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons
import fr.lumiere.tv.ui.theme.LumiereShapes
import fr.lumiere.tv.ui.theme.glass
import fr.lumiere.tv.ui.theme.glassLens
import fr.lumiere.tv.ui.theme.glassProminent
import fr.lumiere.tv.ui.theme.glassRim

/** Apparence d'un contrôle en verre selon son rôle. */
enum class GlassStyle {
    /** Action principale : verre doré (bouton « primary » du web). */
    Primary,

    /** Action secondaire : verre fumé (bouton « glass » du web). */
    Glass,
}

/**
 * Surface cliquable en verre pour la télécommande. Au focus elle grandit et s'éclaire : le verre
 * fumé devient blanc (texte sombre), le verre doré s'illumine. [selected] pose la lentille du menu.
 */
@Composable
fun GlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = LumiereShapes.Pill,
    style: GlassStyle = GlassStyle.Glass,
    selected: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    focusedScale: Float = 1.08f,
    /** Sans verre au repos (entrées du menu) : seuls la lentille et le focus se voient. */
    transparent: Boolean = false,
    selectedContentColor: Color = Color.White,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val primary = style == GlassStyle.Primary
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = when {
                primary -> LumiereColors.Ink950
                selected -> selectedContentColor
                transparent -> LumiereColors.Zinc300
                else -> LumiereColors.Zinc200
            },
            focusedContainerColor = Color.Transparent,
            focusedContentColor = LumiereColors.Ink950,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = LumiereColors.Ink950,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = if (primary) LumiereColors.Ink950 else LumiereColors.Zinc400,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(if (primary) LumiereColors.Brand400.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f), 14.dp),
        ),
        interactionSource = interactionSource,
    ) {
        Box(
            Modifier.matchParentSize().then(
                when {
                    primary -> Modifier.glassProminent(shape, bright = focused)
                    focused -> Modifier.background(Color.White.copy(alpha = 0.95f), shape).glassRim(shape, edge = 0.9f)
                    selected -> Modifier.glassLens(shape)
                    transparent -> Modifier
                    else -> Modifier.glass(shape, GlassVariant.Regular, elevation = 0.dp)
                },
            ),
        )
        content(focused)
    }
}

/** Bouton pilule de Lumière, avec icône facultative. */
@Composable
fun LumiereButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: GlassStyle = GlassStyle.Glass,
    enabled: Boolean = true,
) {
    GlassSurface(onClick = onClick, modifier = modifier, style = style, enabled = enabled) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = if (icon != null) 20.dp else 26.dp, end = 26.dp, top = 12.dp, bottom = 12.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

/** Bouton rond en verre ne portant qu'une icône. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    GlassSurface(onClick = onClick, modifier = modifier.size(size), shape = LumiereShapes.Pill) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.align(Alignment.Center).size(size * 0.42f))
    }
}

/** Pastille de filtre ou d'onglet (tri, genre, saison) ; [badge] : petit compteur à droite. */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    GlassSurface(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        focusedScale = 1.06f,
        selectedContentColor = LumiereColors.Brand300,
    ) { focused ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (focused) LumiereColors.Ink950 else LumiereColors.Zinc300,
                    modifier = Modifier
                        .background(if (focused) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.12f), LumiereShapes.Pill)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** Panneau en verre (cartes, panneaux latéraux), sans interaction propre. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = LumiereShapes.Card,
    variant: GlassVariant = GlassVariant.Thick,
    padding: Dp = 28.dp,
    verticalSpacing: Dp = 14.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalAlignment = horizontalAlignment,
        modifier = modifier.glass(shape, variant, elevation = 24.dp).padding(padding),
        content = content,
    )
}

/** Petite étiquette majuscule posée sur une image (« FILM », « SÉRIE »). */
@Composable
fun KindPill(kind: TitleKind, modifier: Modifier = Modifier) {
    Text(
        (if (kind == TitleKind.MOVIE) "Film" else "Série").uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.16f), LumiereShapes.Pill)
            .glassRim(LumiereShapes.Pill, edge = 0.35f)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Note TMDB : étoile ambrée et « 7,4 ». */
fun formatRating(rating: Double?): String? =
    rating?.takeIf { it > 0 }?.let { "%.1f".format(it).replace('.', ',') }

/**
 * Ligne d'informations sous un titre, comme le bandeau du web : type, année, note étoilée puis
 * éléments libres (genres, durée…), séparés par des points.
 */
@Composable
fun MetaRow(
    kind: TitleKind?,
    year: Int?,
    rating: Double?,
    extras: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val legible = style.copy(shadow = LegibleShadow)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        kind?.let { KindPill(it) }
        year?.let { Text(it.toString(), style = legible, color = LumiereColors.Zinc200) }
        formatRating(rating)?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(LumiereIcons.Star, contentDescription = "Note", tint = LumiereColors.Brand400, modifier = Modifier.size(14.dp))
                Text(it, style = legible, color = LumiereColors.Zinc200)
            }
        }
        extras.forEach { Text("· $it", style = legible, color = LumiereColors.Zinc300) }
    }
}

/** Fine barre de progression de Lumière : piste claire, remplissage ambré. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    Box(modifier.fillMaxWidth().height(height).background(Color.White.copy(alpha = 0.22f), LumiereShapes.Pill)) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).background(
                Brush.horizontalGradient(listOf(LumiereColors.Brand300, LumiereColors.Brand400)),
                LumiereShapes.Pill,
            ),
        )
    }
}

/** Titre de section de Lumière (« Reprendre la lecture »). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge.copy(shadow = LegibleShadow),
        color = Color.White,
        modifier = modifier,
    )
}

/** Petit texte d'aide en bas d'écran. */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LumiereColors.TextMuted, modifier = modifier)
}
