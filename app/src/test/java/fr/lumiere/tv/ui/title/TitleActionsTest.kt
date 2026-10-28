package fr.lumiere.tv.ui.title

import fr.lumiere.tv.data.api.EpisodeEntry
import fr.lumiere.tv.data.api.MediaAvailability
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.data.api.RemoteFile
import fr.lumiere.tv.data.api.SeasonEntry
import fr.lumiere.tv.data.api.TitleDetails
import fr.lumiere.tv.data.api.TitleKind
import fr.lumiere.tv.data.api.WatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class TitleActionsTest {

    private fun movie(files: List<RemoteFile>, notice: String? = null) =
        TitleDetails(id = 3, kind = TitleKind.MOVIE, title = "Cosmos Odyssey", files = files, sourceNotice = notice)

    private fun file(path: String, libraryId: Int? = null, watch: WatchState? = null) =
        RemoteFile(name = path, path = path, availability = MediaAvailability(libraryItemId = libraryId, watch = watch))

    @Test
    fun filmSansFichierAvecMessageDuServeur() = assertEquals(
        PlayAction.Disabled("L'extension a atteint sa limite de liens"),
        playAction(movie(emptyList(), notice = "L'extension a atteint sa limite de liens")),
    )

    @Test
    fun filmSansFichierNiMessage() = assertEquals(
        PlayAction.Disabled("Aucun fichier proposé pour ce titre."),
        playAction(movie(emptyList())),
    )

    @Test
    fun copieDeLaBibliothequeEnPriorite() = assertEquals(
        PlayAction.Play(MediaRef.Library(9), "Lecture"),
        playAction(movie(listOf(file("/a.mkv"), file("/b.mkv", libraryId = 9)))),
    )

    @Test
    fun repriseDUnFilmCommence() = assertEquals(
        PlayAction.Play(MediaRef.Remote("/a.mkv"), "Reprendre à 42 min"),
        playAction(movie(listOf(file("/a.mkv", watch = WatchState(2520.0, 6000.0, completed = false))))),
    )

    @Test
    fun serieJamaisVue() {
        val series = TitleDetails(
            id = 7,
            kind = TitleKind.SERIES,
            title = "Les Explorateurs",
            seasons = listOf(
                SeasonEntry(key = "s1", number = 1, name = "Saison 1", episodes = listOf(
                    EpisodeEntry(key = "e1", season = 1, episode = 1, title = "Le Départ"),
                    EpisodeEntry(key = "e2", season = 1, episode = 2, title = "Le Passage"),
                )),
            ),
        )
        assertEquals(PlayAction.Play(MediaRef.Episode(7, 1, 1), "Lecture S1 É1"), playAction(series))
    }

    @Test
    fun serieRepriseDeLEpisodeEnCours() {
        val watched = WatchState(600.0, 2400.0, completed = false)
        val series = TitleDetails(
            id = 7,
            kind = TitleKind.SERIES,
            title = "Les Explorateurs",
            seasons = listOf(
                SeasonEntry(key = "s1", number = 1, name = "Saison 1", episodes = listOf(
                    EpisodeEntry(key = "e1", season = 1, episode = 1, title = "Le Départ",
                        files = listOf(file("/e1.mkv", watch = WatchState(2400.0, 2400.0, completed = true)))),
                    EpisodeEntry(key = "e2", season = 1, episode = 2, title = "Le Passage",
                        files = listOf(file("/e2.mkv", watch = watched))),
                )),
            ),
        )
        assertEquals(PlayAction.Play(MediaRef.Episode(7, 1, 2), "Reprendre S1 É2 à 10 min"), playAction(series))
    }

    @Test
    fun serieEpisodeSuivantApresUnEpisodeTermine() {
        val series = TitleDetails(
            id = 7,
            kind = TitleKind.SERIES,
            title = "Les Explorateurs",
            seasons = listOf(
                SeasonEntry(key = "s1", number = 1, name = "Saison 1", episodes = listOf(
                    EpisodeEntry(key = "e1", season = 1, episode = 1, title = "Le Départ",
                        files = listOf(file("/e1.mkv", watch = WatchState(2400.0, 2400.0, completed = true)))),
                    EpisodeEntry(key = "e2", season = 1, episode = 2, title = "Le Passage"),
                )),
            ),
        )
        assertEquals(PlayAction.Play(MediaRef.Episode(7, 1, 2), "Lecture S1 É2"), playAction(series))
    }

    @Test
    fun serieSansEpisode() = assertEquals(
        PlayAction.Disabled("Aucun épisode connu pour cette série."),
        playAction(TitleDetails(id = 8, kind = TitleKind.SERIES, title = "Orbite")),
    )
}
