package fr.lumiere.tv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereIcons

/** Chargement : le logo de Lumière respire doucement. */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "chargement").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "respiration",
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LogoMark(Modifier.alpha(pulse), size = 52.dp)
            Text("Chargement…", style = MaterialTheme.typography.titleMedium, color = LumiereColors.TextMuted)
        }
    }
}

/** Erreur plein écran dans une carte de verre, avec Réessayer et, si utile, Changer de serveur. */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onChangeServer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(
            modifier = Modifier.widthIn(max = 640.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalSpacing = 20.dp,
            padding = 32.dp,
        ) {
            Icon(LumiereIcons.CircleAlert, contentDescription = null, tint = LumiereColors.Error, modifier = Modifier.size(34.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LumiereButton(
                    "Réessayer",
                    onClick = onRetry,
                    icon = LumiereIcons.RefreshCw,
                    style = GlassStyle.Primary,
                    modifier = Modifier.focusRequester(focus),
                )
                if (onChangeServer != null) {
                    LumiereButton("Changer de serveur", onClick = onChangeServer, icon = LumiereIcons.Server)
                }
            }
        }
    }
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = LumiereColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 640.dp),
        )
    }
}
