package fr.lumiere.tv.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.lumiere.tv.player.subtitles.Cue

private val base = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 38.sp)

/** Sous-titres en bas de l'image, lisibles à 3 m : texte blanc cerné de noir. */
@Composable
fun SubtitleOverlay(cues: List<Cue>, raised: Boolean, modifier: Modifier = Modifier) {
    if (cues.isEmpty()) return
    val text = cues.flatMap { it.lines }.joinToString("\n")
    Box(modifier.fillMaxSize().padding(start = 96.dp, end = 96.dp, bottom = if (raised) 150.dp else 54.dp), contentAlignment = Alignment.BottomCenter) {
        // Contour : le même texte tracé en noir sous le texte blanc.
        Text(text, style = base.copy(color = Color.Black, drawStyle = Stroke(width = 7f)))
        Text(text, style = base.copy(color = Color.White, shadow = Shadow(Color.Black, blurRadius = 6f)))
    }
}
