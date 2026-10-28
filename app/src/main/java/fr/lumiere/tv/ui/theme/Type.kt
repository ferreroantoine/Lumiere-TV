package fr.lumiere.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.tv.material3.Typography
import fr.lumiere.tv.R

/** Graisse d'Inter variable : le même fichier sert toutes les graisses. */
private fun inter(weight: Int) = Font(
    R.font.inter_variable,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Inter, la police de Lumière sur le web (fichier variable, alphabet latin). */
val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700), inter(800))

/** Ombre qui garde un texte lisible posé sur une image (`text-shadow-legible` du web). */
val LegibleShadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 10f)

private fun TextStyle.inter(weight: Int, size: Int? = null, tracking: Float = 0f) = copy(
    fontFamily = Inter,
    fontWeight = FontWeight(weight),
    fontSize = size?.sp ?: fontSize,
    letterSpacing = tracking.em,
)

/** Échelle de texte de Lumière adaptée à la TV : titres très gras et serrés, texte courant régulier. */
fun lumiereTypography(): Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.inter(800, tracking = -0.025f),
        displayMedium = t.displayMedium.inter(800, size = 44, tracking = -0.025f),
        displaySmall = t.displaySmall.inter(800, size = 38, tracking = -0.02f),
        headlineLarge = t.headlineLarge.inter(800, size = 32, tracking = -0.02f),
        headlineMedium = t.headlineMedium.inter(700, size = 26, tracking = -0.015f),
        headlineSmall = t.headlineSmall.inter(700, size = 22, tracking = -0.01f),
        titleLarge = t.titleLarge.inter(700, size = 19, tracking = -0.01f),
        titleMedium = t.titleMedium.inter(600, size = 16),
        titleSmall = t.titleSmall.inter(600, size = 14),
        bodyLarge = t.bodyLarge.inter(400, size = 16),
        bodyMedium = t.bodyMedium.inter(400, size = 14),
        bodySmall = t.bodySmall.inter(400, size = 12),
        labelLarge = t.labelLarge.inter(600, size = 14),
        labelMedium = t.labelMedium.inter(600, size = 12),
        labelSmall = t.labelSmall.inter(700, size = 11, tracking = 0.06f),
    )
}
