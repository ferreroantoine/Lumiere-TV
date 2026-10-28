package fr.lumiere.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun repriseEnMinutes() = assertEquals("Reprendre à 42 min", formatResume(2520.0))
    @Test fun repriseEnHeures() = assertEquals("Reprendre à 1 h 05", formatResume(3900.0))
    @Test fun repriseTresTot() = assertEquals("Reprendre au début", formatResume(30.0))
    @Test fun dureeEnHeures() = assertEquals("1 h 52", formatRuntime(112))
    @Test fun dureeEnMinutes() = assertEquals("45 min", formatRuntime(45))
}
