package fr.lumiere.tv.ui.title

import fr.lumiere.tv.data.api.EpisodeEntry
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.RemoteFile
import fr.lumiere.tv.data.api.TitleDetails
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.api.WatchState
import fr.lumiere.tv.ui.formatResume
import fr.lumiere.tv.ui.resumeTime

/** Ce que fait le bouton Lecture d'une fiche. */
sealed interface PlayAction {
    data class Play(val media: MediaRef, val label: String) : PlayAction
    data class Disabled(val message: String) : PlayAction
}

private val WatchState.inProgress: Boolean get() = !completed && position >= 60

/**
 * Bouton Lecture d'une fiche.
 * Film : fichier en cours de visionnage, sinon copie de la bibliothèque, sinon première version proposée.
 * Série : épisode en cours, sinon celui qui suit le dernier terminé, sinon le premier.
 */
fun playAction(details: TitleDetails): PlayAction =
    if (details.kind == TitleKind.MOVIE) moviePlayAction(details) else seriesPlayAction(details)

private fun moviePlayAction(details: TitleDetails): PlayAction {
    val files = details.files
    if (files.isEmpty()) return PlayAction.Disabled(details.sourceNotice ?: "Aucun fichier proposé pour ce titre.")
    val file = files.firstOrNull { it.availability.watch?.inProgress == true }
        ?: files.firstOrNull { it.availability.libraryItemId != null }
        ?: files.first()
    val watch = file.availability.watch
    val label = if (watch?.inProgress == true) formatResume(watch.position) else "Lecture"
    return PlayAction.Play(file.mediaRef(), label)
}

/** Copie locale en priorité (lecture hors ligne), sinon le fichier de l'extension. */
fun RemoteFile.mediaRef(): MediaRef =
    availability.libraryItemId?.let { MediaRef.Library(it) } ?: MediaRef.Remote(path)

private fun EpisodeEntry.watch(): WatchState? = files.firstNotNullOfOrNull { it.availability.watch }

private fun seriesPlayAction(details: TitleDetails): PlayAction {
    val seasons = details.seasons.filter { it.number != 0 }.ifEmpty { details.seasons }
    val episodes = seasons.sortedBy { it.number ?: 0 }
        .flatMap { it.episodes }
        .filter { it.season != null && it.episode != null }
        .sortedWith(compareBy({ it.season }, { it.episode }))
    if (episodes.isEmpty()) return PlayAction.Disabled(details.sourceNotice ?: "Aucun épisode connu pour cette série.")

    episodes.firstOrNull { it.watch()?.inProgress == true }?.let { ep ->
        val position = ep.watch()!!.position
        return PlayAction.Play(ep.ref(details.id), "Reprendre ${ep.code()} à ${resumeTime(position)}")
    }
    val lastDone = episodes.indexOfLast { it.watch()?.completed == true }
    val next = episodes.getOrNull(lastDone + 1) ?: episodes.first()
    return PlayAction.Play(next.ref(details.id), "Lecture ${next.code()}")
}

private fun EpisodeEntry.ref(titleId: Int) = MediaRef.Episode(titleId, season!!, episode!!)

/** « S1 É2 ». */
fun EpisodeEntry.code(): String = "S$season É$episode"
