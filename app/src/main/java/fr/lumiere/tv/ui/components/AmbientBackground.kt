package fr.lumiere.tv.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import fr.lumiere.tv.ui.theme.AmbientPalette
import fr.lumiere.tv.ui.theme.LumiereColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DefaultPalette = AmbientPalette.Default.map { Color(it) }

/**
 * Fond de Lumière (`.ambient`) : trois halos de lumière sur l'encre, colorés par l'image qui a le
 * focus et qui changent en douceur. [backdropUrl] ajoute l'illustration plein cadre de l'accueil
 * et des fiches, fondue vers le bas et assombrie à gauche pour le texte.
 */
@Composable
fun AmbientBackground(imageUrl: String?, modifier: Modifier = Modifier, backdropUrl: String? = null) {
    val context = LocalContext.current
    var palette by remember { mutableStateOf(DefaultPalette) }
    LaunchedEffect(imageUrl) {
        palette = imageUrl?.let { ambientPalette(context, it) } ?: DefaultPalette
    }
    val a by animateColorAsState(palette[0], tween(900), label = "ambiance-a")
    val b by animateColorAsState(palette[1], tween(900), label = "ambiance-b")
    val c by animateColorAsState(palette[2], tween(900), label = "ambiance-c")

    Box(
        modifier.fillMaxSize().background(LumiereColors.Ink950).drawBehind {
            glow(a.copy(alpha = 0.52f), Offset(0.08f, -0.08f), 0.62f, 0.58f, 0.72f)
            glow(b.copy(alpha = 0.42f), Offset(1f, 0.06f), 0.52f, 0.60f, 0.70f)
            glow(c.copy(alpha = 0.36f), Offset(0.55f, 1.12f), 0.80f, 0.55f, 0.72f)
        },
    ) {
        Crossfade(backdropUrl, label = "illustration", modifier = Modifier.fillMaxSize()) { url ->
            if (url != null) Backdrop(url)
        }
    }
}

/**
 * Halo elliptique (`radial-gradient(rx ry at x y, couleur, transparent fin)`) ; position et rayons
 * en fractions de l'écran.
 */
private fun DrawScope.glow(color: Color, at: Offset, rx: Float, ry: Float, end: Float) {
    val center = Offset(size.width * at.x, size.height * at.y)
    val radiusY = size.height * ry
    val radiusX = size.width * rx
    scale(scaleX = radiusX / radiusY, scaleY = 1f, pivot = center) {
        drawCircle(
            Brush.radialGradient(0f to color, end to Color.Transparent, center = center, radius = radiusY),
            radius = radiusY,
            center = center,
        )
    }
}

/** Illustration de fond (bandeau du web) : visible en haut à droite, fondue vers le bas et la gauche. */
@Composable
private fun Backdrop(url: String) {
    Box(
        Modifier.fillMaxWidth().fillMaxHeight(0.86f)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0.5f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            },
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.78f),
                    0.32f to Color.Black.copy(alpha = 0.62f),
                    0.62f to Color.Black.copy(alpha = 0.18f),
                    0.9f to Color.Transparent,
                ),
            ),
        )
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.3f).background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 1f to Color.Transparent),
            ),
        )
    }
}

/** Palette d'une image : vignette de 32 x 32 points, comme sur le web. */
private suspend fun ambientPalette(context: android.content.Context, url: String): List<Color>? {
    val request = ImageRequest.Builder(context).data(url).size(64).allowHardware(false).build()
    val image = SingletonImageLoader.get(context).execute(request).image ?: return null
    return withContext(Dispatchers.Default) {
        val sample = Bitmap.createScaledBitmap(image.toBitmap(), SAMPLE, SAMPLE, true)
        val pixels = IntArray(SAMPLE * SAMPLE)
        sample.getPixels(pixels, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE)
        AmbientPalette.fromPixels(pixels)?.map { Color(it) }
    }
}

private const val SAMPLE = 32
