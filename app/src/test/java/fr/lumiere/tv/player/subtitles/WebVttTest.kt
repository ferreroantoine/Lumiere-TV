package fr.lumiere.tv.player.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebVttTest {
    private val sample = """
        WEBVTT

        1
        00:00:01.000 --> 00:00:02.500
        Bonjour

        00:01:02.500 --> 00:01:04.000 align:start position:10%
        <i>Bonjour</i> &amp; salut
        Deuxième ligne

        NOTE ceci est un commentaire

        00:01:05.000 --> 00:01:06.000
        Fin
    """.trimIndent()

    @Test
    fun troisRepliques() {
        val cues = WebVtt.parse(sample)
        assertEquals(3, cues.size)
        assertEquals(1.0, cues[0].start, 0.0001)
        assertEquals(2.5, cues[0].end, 0.0001)
        assertEquals(62.5, cues[1].start, 0.0001)
    }

    @Test
    fun balisesEtEntitesRetirees() {
        val cue = WebVtt.parse(sample)[1]
        assertEquals(listOf("Bonjour & salut", "Deuxième ligne"), cue.lines)
    }

    @Test
    fun bomEtFinsDeLigneWindows() {
        val text = "﻿WEBVTT\r\n\r\n00:01.000 --> 00:02.000\r\nSalut\r\n"
        val cues = WebVtt.parse(text)
        assertEquals(1, cues.size)
        assertEquals(listOf("Salut"), cues[0].lines)
    }

    @Test
    fun heuresFacultatives() {
        val cues = WebVtt.parse("WEBVTT\n\n01:02.000 --> 01:03.000\nA\n\n1:00:00.000 --> 1:00:01.500\nB")
        assertEquals(62.0, cues[0].start, 0.0001)
        assertEquals(3600.0, cues[1].start, 0.0001)
    }

    @Test
    fun texteVide() = assertTrue(WebVtt.parse("").isEmpty())

    @Test
    fun sansEnteteWebvtt() = assertTrue(WebVtt.parse("00:01.000 --> 00:02.000\nA").isEmpty())

    @Test
    fun repliqueMalFormeeSautee() {
        val cues = WebVtt.parse("WEBVTT\n\n00:xx.000 --> 00:02.000\nCassée\n\n00:03.000 --> 00:04.000\nBonne")
        assertEquals(listOf(listOf("Bonne")), cues.map { it.lines })
    }

    @Test
    fun trieesParDebut() {
        val cues = WebVtt.parse("WEBVTT\n\n00:05.000 --> 00:06.000\nB\n\n00:01.000 --> 00:02.000\nA")
        assertEquals(listOf("A", "B"), cues.map { it.lines.first() })
    }
}
