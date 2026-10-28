package fr.lumiere.tv.data.api

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoDecodingTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name.json")) { "fixture $name absente" }
            .readText()

    @Test
    fun accueil() {
        val home = LumiereJson.decodeFromString<HomeFeed>(fixture("home"))
        assertTrue(home.popularMovies.isNotEmpty())
    }

    @Test
    fun pageDuCatalogue() {
        val page = LumiereJson.decodeFromString<Page<TitleSummary>>(fixture("titles-page"))
        assertTrue(page.items.isNotEmpty())
        assertEquals(1, page.page)
    }

    @Test
    fun ficheFilm() {
        val title = LumiereJson.decodeFromString<TitleDetails>(fixture("title-movie"))
        assertEquals(TitleKind.MOVIE, title.kind)
        assertTrue(title.files.isNotEmpty())
    }

    @Test
    fun ficheSerieAvecSaisonsEtEpisodes() {
        val title = LumiereJson.decodeFromString<TitleDetails>(fixture("title-series"))
        assertEquals(TitleKind.SERIES, title.kind)
        assertTrue(title.seasons.any { it.episodes.isNotEmpty() })
    }

    @Test
    fun fichiersDunEpisode() {
        val files = LumiereJson.decodeFromString<EpisodeFiles>(fixture("episode-files"))
        assertTrue(files.files.isNotEmpty())
    }

    @Test
    fun bibliotheque() {
        LumiereJson.decodeFromString<List<LibraryItem>>(fixture("library"))
    }

    @Test
    fun reprendreLaLecture() {
        LumiereJson.decodeFromString<List<ContinueWatchingItem>>(fixture("continue"))
    }

    @Test
    fun systeme() {
        val system = LumiereJson.decodeFromString<SystemInfo>(fixture("system"))
        assertTrue(system.version.isNotBlank())
    }

    @Test
    fun sessionDirecte() {
        val session = LumiereJson.decodeFromString<PlaybackSession>(fixture("session-direct"))
        assertEquals("direct", session.mode)
        assertEquals("native", session.seek)
    }

    @Test
    fun sessionConvertie() {
        val session = LumiereJson.decodeFromString<PlaybackSession>(fixture("session-transcode"))
        assertEquals("transcode", session.mode)
        assertEquals("restart", session.seek)
    }

    @Test
    fun erreur() {
        val error = LumiereJson.decodeFromString<ApiErrorBody>(fixture("error"))
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun mediaRefEpisodeEncodeAvecSonType() {
        val json = LumiereJson.encodeToString<MediaRef>(MediaRef.Episode(12, 1, 3))
        assertEquals("""{"type":"episode","titleId":12,"season":1,"episode":3}""", json)
    }

    @Test
    fun requeteSansPisteAudioNEmetPasLaCle() {
        val request = PlaybackRequest(
            media = MediaRef.Library(4),
            capabilities = ClientCapabilities(listOf("h264"), listOf("aac"), listOf("mp4")),
        )
        val json = LumiereJson.encodeToString(request)
        assertFalse(json.contains("audioTrack"))
        assertTrue(json.contains(""""mode":"auto""""))
    }
}
