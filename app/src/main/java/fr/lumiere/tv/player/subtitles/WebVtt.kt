package fr.lumiere.tv.player.subtitles

/** Réplique de sous-titres : instants en secondes, lignes de texte sans balises. */
data class Cue(val start: Double, val end: Double, val lines: List<String>)

/** Lecture du WebVTT servi par Lumière (`…/subtitles/:piste`). */
object WebVtt {
    private val TIMING = Regex("""^\s*((?:\d+:)?\d{1,2}:\d{2}[.,]\d{1,3})\s+-->\s+((?:\d+:)?\d{1,2}:\d{2}[.,]\d{1,3})""")
    private val TAG = Regex("""<[^>]*>""")
    private val ENTITIES = mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&nbsp;" to " ", "&lrm;" to "", "&rlm;" to "")

    /** Répliques triées par début ; une réplique mal formée est ignorée, un texte qui n'est pas du WebVTT donne une liste vide. */
    fun parse(text: String): List<Cue> {
        val normalized = text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        if (!normalized.trimStart().startsWith("WEBVTT")) return emptyList()
        val cues = mutableListOf<Cue>()
        for (block in normalized.split(Regex("\n{2,}"))) {
            val lines = block.lines().filter { it.isNotBlank() }
            val timingIndex = lines.indexOfFirst { "-->" in it }
            if (timingIndex < 0 || timingIndex > 1) continue
            val match = TIMING.find(lines[timingIndex]) ?: continue
            val start = parseTime(match.groupValues[1]) ?: continue
            val end = parseTime(match.groupValues[2]) ?: continue
            val textLines = lines.drop(timingIndex + 1).map(::clean).filter { it.isNotEmpty() }
            if (textLines.isNotEmpty() && end > start) cues += Cue(start, end, textLines)
        }
        return cues.sortedBy { it.start }
    }

    private fun clean(line: String): String {
        var out = TAG.replace(line, "")
        ENTITIES.forEach { (entity, value) -> out = out.replace(entity, value) }
        return out.trim()
    }

    private fun parseTime(value: String): Double? {
        val parts = value.replace(',', '.').split(':')
        val seconds = parts.last().toDoubleOrNull() ?: return null
        val minutes = parts[parts.size - 2].toIntOrNull() ?: return null
        val hours = if (parts.size == 3) parts[0].toIntOrNull() ?: return null else 0
        return hours * 3600 + minutes * 60 + seconds
    }
}
