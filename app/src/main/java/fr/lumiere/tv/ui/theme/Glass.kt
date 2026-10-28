package fr.lumiere.tv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Épaisseurs du verre de Lumière (`.glass-clear`, `.glass`, `.glass-thick`). Sans flou derrière,
 * la teinte est plus couvrante que sur le web pour que le texte reste lisible.
 */
enum class GlassVariant(val alpha: Float) {
    /** Posé sur une image : pastilles, badges. */
    Clear(0.45f),

    /** Barre latérale, boutons, champs. */
    Regular(0.72f),

    /** Panneaux qui recouvrent du contenu : pistes, versions, cartes d'erreur. */
    Thick(0.9f),
}

/** Fumée du verre de Lumière (`--lg-tint: 22 22 30`). */
private val Tint = Color(red = 22, green = 22, blue = 30)

/** Reflet vertical du verre : plus clair en haut, un peu de lumière en bas. */
private val Sheen = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.086f),
    0.42f to Color.White.copy(alpha = 0.02f),
    1f to Color.White.copy(alpha = 0.04f),
)

/** Reflet du verre doré (`.glass-prominent`). */
private val ProminentSheen = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.38f),
    0.45f to Color.White.copy(alpha = 0.08f),
    0.6f to Color.Transparent,
    1f to Color.White.copy(alpha = 0.12f),
)

/** Intensité du liseré lumineux (`--lg-edge`). */
private const val EDGE = 0.55f

/**
 * Verre « Liquid Glass » de Lumière, sans flou en temps réel (trop coûteux pour le stick et
 * absent avant Android 12) : teinte fumée, reflet vertical, liseré éclairé à 135° et ombre portée.
 */
fun Modifier.glass(
    shape: Shape,
    variant: GlassVariant = GlassVariant.Regular,
    elevation: Dp = 14.dp,
): Modifier =
    (if (elevation > 0.dp) shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black) else this)
        .background(Tint.copy(alpha = variant.alpha), shape)
        .background(Sheen, shape)
        .glassRim(shape)

/** Verre doré de l'action principale (`.glass-prominent`) : texte en encre sombre. */
fun Modifier.glassProminent(shape: Shape, bright: Boolean = false): Modifier =
    background(if (bright) LumiereColors.Brand300 else LumiereColors.Brand400.copy(alpha = 0.95f), shape)
        .background(ProminentSheen, shape)
        .glassRim(shape, edge = 0.75f)

/** Pastille active d'une liste (indicateur « lentille » du menu de Lumière). */
fun Modifier.glassLens(shape: Shape): Modifier =
    background(Color.White.copy(alpha = 0.04f), shape)
        .background(
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.16f), 1f to Color.White.copy(alpha = 0.07f)),
            shape,
        )
        .glassRim(shape, edge = 0.42f)

/**
 * Liseré du verre : lumière venue du coin haut gauche, quasi nulle au milieu des bords, reprise
 * en bas à droite (`.glass::after`). Le trait est tracé à l'intérieur de la forme.
 */
fun Modifier.glassRim(shape: Shape, edge: Float = EDGE, width: Dp = 1.dp): Modifier = drawWithCache {
    val stroke = width.toPx()
    val inner = Size(size.width - stroke, size.height - stroke)
    val outline = shape.createOutline(inner, layoutDirection, this)
    val brush = Brush.linearGradient(
        0f to Color.White.copy(alpha = edge),
        0.2f to Color.White.copy(alpha = edge * 0.18f),
        0.42f to Color.Transparent,
        0.58f to Color.Transparent,
        0.8f to Color.White.copy(alpha = edge * 0.14f),
        1f to Color.White.copy(alpha = edge * 0.62f),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val ring = Color.White.copy(alpha = 0.06f)
    onDrawWithContent {
        drawContent()
        translate(stroke / 2, stroke / 2) {
            drawOutline(outline, ring, style = Stroke(stroke))
            drawOutline(outline, brush, style = Stroke(stroke))
        }
    }
}

/** Contour d'un élément qui a le focus : lumière blanche qui vire à l'ambre de Lumière. */
val FocusRimBrush = Brush.linearGradient(
    0f to Color.White,
    0.45f to LumiereColors.Brand200,
    1f to LumiereColors.Brand400,
)
