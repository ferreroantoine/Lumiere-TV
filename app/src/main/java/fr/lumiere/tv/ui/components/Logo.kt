package fr.lumiere.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.theme.LumiereColors

/** Icône de Lumière en couches de verre : fond fumé, faisceau ambré, reflet (components/layout/Logo.tsx). */
@Composable
fun LogoMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 64f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        val corner = CornerRadius(18 * u)
        val beam = Brush.linearGradient(
            0f to LumiereColors.Brand100,
            0.5f to LumiereColors.Brand400,
            1f to LumiereColors.Brand600,
            start = Offset.Zero,
            end = Offset(this.size.width, this.size.height),
        )
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2A2A35), Color(0xFF0C0C12))), cornerRadius = corner)
        drawPath(Path().apply { moveTo(13 * u, 17 * u); lineTo(52 * u, 32 * u); lineTo(13 * u, 47 * u); close() }, beam, alpha = 0.16f)
        drawPath(Path().apply { moveTo(24 * u, 20.5f * u); lineTo(46 * u, 32 * u); lineTo(24 * u, 43.5f * u); close() }, beam)
        drawCircle(beam, radius = 5.5f * u, center = p(16.5f, 32f))
        clipRect(bottom = 32 * u) {
            drawRoundRect(
                Brush.verticalGradient(0f to Color.White.copy(alpha = 0.32f), 1f to Color.Transparent, endY = 32 * u),
                cornerRadius = corner,
            )
        }
        drawRoundRect(
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.55f),
                0.45f to Color.White.copy(alpha = 0.06f),
                1f to Color.White.copy(alpha = 0.3f),
                start = Offset.Zero,
                end = Offset(this.size.width, this.size.height),
            ),
            topLeft = p(0.75f, 0.75f),
            size = Size(62.5f * u, 62.5f * u),
            cornerRadius = CornerRadius(17.25f * u),
            style = Stroke(1.5f * u),
        )
    }
}

/** Icône et nom de Lumière. */
@Composable
fun Logo(modifier: Modifier = Modifier, showName: Boolean = true) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoMark()
        if (showName) Text("Lumière", style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1)
    }
}
