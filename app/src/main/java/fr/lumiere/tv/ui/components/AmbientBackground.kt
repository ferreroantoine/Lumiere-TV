package fr.lumiere.tv.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import fr.lumiere.tv.ui.theme.LumiereColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fond « salle obscure » teinté par l'illustration qui a le focus, comme l'ambiance du web.
 * La couleur est extraite d'une petite version de l'image (Palette) et change en douceur.
 */
@Composable
fun AmbientBackground(imageUrl: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tint by remember { mutableStateOf(LumiereColors.Ink900) }
    LaunchedEffect(imageUrl) {
        tint = if (imageUrl == null) LumiereColors.Ink900 else dominantColor(context, imageUrl) ?: LumiereColors.Ink900
    }
    val animated by animateColorAsState(tint, animationSpec = tween(600), label = "ambiance")
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to animated.copy(alpha = 0.55f).compositeOver(LumiereColors.Ink900),
                0.65f to LumiereColors.Ink900,
                1f to LumiereColors.Ink950,
            ),
        ),
    )
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}

private suspend fun dominantColor(context: android.content.Context, url: String): Color? {
    val request = ImageRequest.Builder(context).data(url).size(96).allowHardware(false).build()
    val image = SingletonImageLoader.get(context).execute(request).image ?: return null
    return withContext(Dispatchers.Default) {
        val palette = Palette.from(image.toBitmap()).generate()
        val rgb = palette.darkVibrantSwatch?.rgb ?: palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
        rgb?.let { Color(it) }
    }
}
