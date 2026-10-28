package fr.lumiere.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Couleurs de Lumière (apps/web/src/styles/theme.css) : fond « salle obscure », accent ambré. */
object LumiereColors {
    val Ink950 = Color(0xFF050508)
    val Ink900 = Color(0xFF0B0B10)
    val Ink850 = Color(0xFF111118)
    val Ink800 = Color(0xFF18181F)
    val Ink700 = Color(0xFF22222B)
    val Ink600 = Color(0xFF2F2F3B)
    val Ink500 = Color(0xFF4A4A59)
    val Brand200 = Color(0xFFFDE68A)
    val Brand300 = Color(0xFFFCD34D)
    val Brand400 = Color(0xFFFBBF24)
    val Brand500 = Color(0xFFF59E0B)
    val Text = Color(0xFFF4F4F5)
    val TextMuted = Color(0xFFA1A1AA)
    val Error = Color(0xFFF87171)
}

/** Rayons de Lumière (docs/DESIGN.md) adaptés à la TV. */
object LumiereShapes {
    val Panel = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(22.dp)
    val Tile = RoundedCornerShape(18.dp)
    val Inner = RoundedCornerShape(14.dp)
    val Pill = RoundedCornerShape(50)
}

/** Zone de sécurité de l'écran TV. */
object Safe {
    val Horizontal = 48.dp
    val Vertical = 27.dp
}

private val colors = darkColorScheme(
    primary = LumiereColors.Brand400,
    onPrimary = LumiereColors.Ink950,
    primaryContainer = LumiereColors.Brand500,
    onPrimaryContainer = LumiereColors.Ink950,
    secondary = LumiereColors.Brand200,
    onSecondary = LumiereColors.Ink950,
    background = LumiereColors.Ink900,
    onBackground = LumiereColors.Text,
    surface = LumiereColors.Ink850,
    onSurface = LumiereColors.Text,
    surfaceVariant = LumiereColors.Ink700,
    onSurfaceVariant = LumiereColors.TextMuted,
    border = LumiereColors.Brand300,
    error = LumiereColors.Error,
)

@Composable
fun LumiereTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
