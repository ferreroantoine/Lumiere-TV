package fr.lumiere.tv.ui.theme

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Palette d'ambiance : trois couleurs vives tirées d'une affiche ou d'une image de fond, qui
 * colorent la lumière derrière le verre. Même calcul que Lumière sur le web
 * (apps/web/src/design/ambient/palette.ts). Couleurs en ARGB.
 */
object AmbientPalette {

    /** Ambre, bleu et turquoise de Lumière, sans image. */
    val Default: List<Int> = listOf(hsl(38f, 0.85f, 0.42f), hsl(232f, 0.60f, 0.46f), hsl(186f, 0.60f, 0.34f))

    /**
     * Trois couleurs dominantes et vives de pixels ARGB (histogramme pondéré par la vivacité, par
     * tranches de 30° de teinte, en privilégiant des teintes distinctes). `null` si l'image est trop
     * terne (niveaux de gris, noir…).
     */
    fun fromPixels(pixels: IntArray): List<Int>? {
        val weight = DoubleArray(12)
        val red = DoubleArray(12)
        val green = DoubleArray(12)
        val blue = DoubleArray(12)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val (hue, saturation, lightness) = rgbToHsl(r, g, b)
            if (saturation < 0.18f || lightness < 0.08f || lightness > 0.92f) continue
            val w = saturation * (1 - abs(lightness - 0.5f))
            val bucket = floor(hue / 30f).toInt() % 12
            weight[bucket] += w.toDouble()
            red[bucket] += r * w.toDouble()
            green[bucket] += g * w.toDouble()
            blue[bucket] += b * w.toDouble()
        }
        val ranked = (0 until 12).filter { weight[it] > 0.6 }.sortedByDescending { weight[it] }
        if (ranked.isEmpty()) return null

        val chosen = mutableListOf<Int>()
        for (bucket in ranked) {
            val distinct = chosen.all { other ->
                val gap = abs(other - bucket)
                min(gap, 12 - gap) >= 2
            }
            if (distinct) chosen += bucket
            if (chosen.size == 3) break
        }
        for (bucket in ranked) {
            if (chosen.size == 3) break
            if (bucket !in chosen) chosen += bucket
        }
        val colors = chosen.map { toAmbient(red[it] / weight[it], green[it] / weight[it], blue[it] / weight[it]) }
        return listOf(colors[0], colors.getOrElse(1) { colors[0] }, colors.getOrElse(2) { colors.getOrElse(1) { colors[0] } })
    }

    /** Couleur lumineuse mais sombre : de la lumière d'ambiance, jamais un aplat criard. */
    private fun toAmbient(r: Double, g: Double, b: Double): Int {
        val (hue, saturation, lightness) = rgbToHsl(r.roundToInt(), g.roundToInt(), b.roundToInt())
        // Arrondis du web : teinte au degré, saturation et luminosité au pour cent.
        return hsl(
            hue.roundToInt().toFloat(),
            (saturation.coerceIn(0.42f, 0.88f) * 100).roundToInt() / 100f,
            (lightness.coerceIn(0.32f, 0.5f) * 100).roundToInt() / 100f,
        )
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val red = r / 255f
        val green = g / 255f
        val blue = b / 255f
        val maxC = max(red, max(green, blue))
        val minC = min(red, min(green, blue))
        val lightness = (maxC + minC) / 2
        if (maxC == minC) return Triple(0f, 0f, lightness)
        val delta = maxC - minC
        val saturation = if (lightness > 0.5f) delta / (2 - maxC - minC) else delta / (maxC + minC)
        val hue = when (maxC) {
            red -> (green - blue) / delta + (if (green < blue) 6 else 0)
            green -> (blue - red) / delta + 2
            else -> (red - green) / delta + 4
        }
        return Triple(hue * 60, saturation, lightness)
    }

    /** Couleur ARGB opaque depuis teinte (degrés), saturation et luminosité (0 à 1). */
    fun hsl(hue: Float, saturation: Float, lightness: Float): Int {
        val c = (1 - abs(2 * lightness - 1)) * saturation
        val h = ((hue % 360) + 360) % 360 / 60f
        val x = c * (1 - abs(h % 2 - 1))
        val (r, g, b) = when {
            h < 1 -> Triple(c, x, 0f)
            h < 2 -> Triple(x, c, 0f)
            h < 3 -> Triple(0f, c, x)
            h < 4 -> Triple(0f, x, c)
            h < 5 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = lightness - c / 2
        fun channel(v: Float) = ((v + m) * 255).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
