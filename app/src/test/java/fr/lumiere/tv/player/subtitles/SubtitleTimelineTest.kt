package fr.lumiere.tv.player.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleTimelineTest {
    private val cue = Cue(10.0, 12.0, listOf("Bonjour"))
    private val timeline = SubtitleTimeline(listOf(Cue(1.0, 2.0, listOf("Avant")), cue, Cue(20.0, 21.0, listOf("Après"))))

    @Test
    fun visibleEntreDebutEtFin() {
        assertEquals(listOf(cue), timeline.activeAt(10.0, 0.0, 1.0))
        assertEquals(listOf(cue), timeline.activeAt(11.9, 0.0, 1.0))
        assertTrue(timeline.activeAt(12.0, 0.0, 1.0).isEmpty())
    }

    @Test
    fun decalage() {
        assertEquals(listOf(cue), timeline.activeAt(11.5, 1.5, 1.0))
        assertTrue(timeline.activeAt(11.4, 1.5, 1.0).isEmpty())
    }

    @Test
    fun vitesse() {
        val rate = 25.0 / 23.976
        assertTrue(timeline.activeAt(10.0 * rate - 0.01, 0.0, rate).isEmpty())
        assertEquals(listOf(cue), timeline.activeAt(10.0 * rate + 0.01, 0.0, rate))
    }

    @Test
    fun chevauchement() {
        val a = Cue(5.0, 9.0, listOf("A"))
        val b = Cue(6.0, 7.0, listOf("B"))
        assertEquals(listOf(a, b), SubtitleTimeline(listOf(a, b)).activeAt(6.5, 0.0, 1.0))
    }

    @Test
    fun longueRepliqueAvantDesCourtes() {
        // Une réplique longue commencée tôt reste visible malgré les courtes qui suivent.
        val long = Cue(0.0, 100.0, listOf("Long"))
        val cues = listOf(long) + (1..50).map { Cue(it.toDouble(), it + 0.5, listOf("C$it")) }
        assertEquals(listOf(long), SubtitleTimeline(cues).activeAt(30.7, 0.0, 1.0))
    }
}
