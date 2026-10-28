package fr.lumiere.tv.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmbientPaletteTest {

    private fun hex(color: Int) = "#%06x".format(color and 0xFFFFFF)

    @Test
    fun paletteParDefautIdentiqueAuWeb() {
        // --ambient-a, --ambient-b, --ambient-c de Lumière.
        assertEquals(listOf("#c68310", "#2f42bc", "#23808b"), AmbientPalette.Default.map(::hex))
    }

    @Test
    fun imageTerneSansPalette() {
        val grey = IntArray(64) { if (it % 2 == 0) 0xFF808080.toInt() else 0xFF101010.toInt() }
        assertNull(AmbientPalette.fromPixels(grey))
    }

    @Test
    fun couleursVivesAssombriesEtTeintesDistinctes() {
        // Rouge pur en majorité, bleu pur ensuite : deux teintes, la troisième reprend la deuxième.
        val pixels = IntArray(100) { if (it < 60) 0xFFFF0000.toInt() else 0xFF0000FF.toInt() }
        val palette = AmbientPalette.fromPixels(pixels)!!
        val red = AmbientPalette.hsl(0f, 0.88f, 0.5f)
        val blue = AmbientPalette.hsl(240f, 0.88f, 0.5f)
        assertEquals(listOf(red, blue, blue).map(::hex), palette.map(::hex))
    }

    @Test
    fun teintesVoisinesEcarteesAuProfitDUneTeinteDistincte() {
        // Orange (30°) voisin du rouge : écarté tant qu'une teinte distincte (vert) reste disponible.
        val pixels = IntArray(100) {
            when {
                it < 50 -> 0xFFFF0000.toInt()
                it < 80 -> 0xFFFF8000.toInt()
                else -> 0xFF00FF00.toInt()
            }
        }
        val palette = AmbientPalette.fromPixels(pixels)!!
        assertEquals(hex(AmbientPalette.hsl(0f, 0.88f, 0.5f)), hex(palette[0]))
        assertEquals(hex(AmbientPalette.hsl(120f, 0.88f, 0.5f)), hex(palette[1]))
        assertEquals(hex(AmbientPalette.hsl(30f, 0.88f, 0.5f)), hex(palette[2]))
    }
}
