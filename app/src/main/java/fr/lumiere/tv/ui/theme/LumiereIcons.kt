package fr.lumiere.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icônes Lucide utilisées par Lumière sur le web (lucide-react, licence ISC) : tracés repris
 * tels quels, trait de 2 sur une grille de 24.
 */
object LumiereIcons {
    val Home by lazy { lucide("house", "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val Film by lazy { lucide("film", rect(3f, 3f, 18f, 18f, 2f), "M7 3v18", "M3 7.5h4", "M3 12h18", "M3 16.5h4", "M17 3v18", "M17 7.5h4", "M17 16.5h4") }
    val Tv by lazy { lucide("tv", "m17 2-5 5-5-5", rect(2f, 7f, 20f, 15f, 2f)) }
    val Library by lazy { lucide("library", "m16 6 4 14", "M12 6v14", "M8 8v12", "M4 4v16") }
    val Settings by lazy {
        lucide(
            "settings",
            "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
            circle(12f, 12f, 3f),
        )
    }
    val Search by lazy { lucide("search", "m21 21-4.34-4.34", circle(11f, 11f, 8f)) }
    val Play by lazy { lucide("play", "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z", filled = true) }
    val Pause by lazy { lucide("pause", rect(14f, 3f, 5f, 18f, 1f), rect(5f, 3f, 5f, 18f, 1f), filled = true) }
    val Info by lazy { lucide("info", circle(12f, 12f, 10f), "M12 16v-4", "M12 8h.01") }
    val Star by lazy {
        lucide(
            "star",
            "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
            filled = true,
        )
    }
    val Check by lazy { lucide("check", "M20 6 9 17l-5-5") }
    val ChevronLeft by lazy { lucide("chevron-left", "m15 18-6-6 6-6") }
    val ChevronRight by lazy { lucide("chevron-right", "m9 18 6-6-6-6") }
    val Captions by lazy { lucide("captions", rect(3f, 5f, 18f, 14f, 2f), "M7 15h4M15 15h2M7 11h2M13 11h4") }
    val AudioLines by lazy { lucide("audio-lines", "M2 10v3", "M6 6v11", "M10 3v18", "M14 8v7", "M18 5v13", "M22 10v3") }
    val RotateCcw by lazy { lucide("rotate-ccw", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5") }
    val RotateCw by lazy { lucide("rotate-cw", "M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8", "M21 3v5h-5") }
    val RefreshCw by lazy {
        lucide("refresh-cw", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5")
    }
    val Server by lazy { lucide("server", rect(2f, 2f, 20f, 8f, 2f), rect(2f, 14f, 20f, 8f, 2f), "M6 6h.01", "M6 18h.01") }
    val SkipForward by lazy { lucide("skip-forward", "M21 4v16", "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z") }
    val Languages by lazy { lucide("languages", "m5 8 6 6", "m4 14 6-6 2-3", "M2 5h12", "M7 2h1", "m22 22-5-10-5 10", "M14 18h6") }
    val CircleAlert by lazy { lucide("circle-alert", circle(12f, 12f, 10f), "M12 8v4", "M12 16h.01") }
    val X by lazy { lucide("x", "M18 6 6 18", "m6 6 12 12") }
}

/** Cercle SVG (`<circle>`) en tracé : deux demi-arcs. */
private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

/** Rectangle SVG à coins arrondis (`<rect rx>`) en tracé. */
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun lucide(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = if (filled) SolidColor(Color.White) else null,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        .build()
