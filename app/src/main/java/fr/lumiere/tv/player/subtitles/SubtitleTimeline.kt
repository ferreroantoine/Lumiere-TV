package fr.lumiere.tv.player.subtitles

/**
 * Répliques visibles à un instant, avec le recalage de Lumière :
 * instant d'affichage = rate × instant prévu + offset.
 * Recherche dichotomique : le lecteur interroge plusieurs fois par seconde.
 */
class SubtitleTimeline(cues: List<Cue>) {
    private val cues = cues.sortedBy { it.start }

    /** Fin la plus tardive parmi les répliques 0..i (une longue réplique peut couvrir les suivantes). */
    private val maxEndUpTo = DoubleArray(this.cues.size).also { ends ->
        var max = Double.NEGATIVE_INFINITY
        this.cues.forEachIndexed { i, cue -> max = maxOf(max, cue.end); ends[i] = max }
    }

    fun activeAt(position: Double, offset: Double, rate: Double): List<Cue> {
        if (cues.isEmpty()) return emptyList()
        // Dernière réplique dont le début affiché est atteint.
        var lo = 0
        var hi = cues.size - 1
        var last = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (rate * cues[mid].start + offset <= position) {
                last = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        val visible = ArrayDeque<Cue>()
        var i = last
        while (i >= 0 && rate * maxEndUpTo[i] + offset > position) {
            if (rate * cues[i].end + offset > position) visible.addFirst(cues[i])
            i--
        }
        return visible.toList()
    }
}
