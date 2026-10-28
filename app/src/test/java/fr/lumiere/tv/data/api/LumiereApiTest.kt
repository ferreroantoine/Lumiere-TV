package fr.lumiere.tv.data.api

import fr.lumiere.tv.testing.address
import fr.lumiere.tv.testing.fixture
import fr.lumiere.tv.testing.reply
import fr.lumiere.tv.testing.testClient
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LumiereApiTest {
    private val server = MockWebServer()
    private lateinit var api: LumiereApi

    @Before
    fun setUp() {
        server.start()
        val address = server.address()
        api = LumiereApi(testClient()) { address }
    }

    @After fun tearDown() = server.close()

    private val caps = ClientCapabilities(listOf("h264"), listOf("aac"), listOf("mp4"))

    @Test
    fun catalogueEnvoieLesBonsParametres() = runTest {
        server.reply(body = fixture("titles-page"))
        api.titles(TitleKind.MOVIE, "amélie", "popular", null, 2)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/titles?kind=movie&q=am%C3%A9lie&sort=popular&page=2&pageSize=40", request.target)
    }

    @Test
    fun creationDeSessionEnvoieLaReferenceDEpisode() = runTest {
        server.reply(body = fixture("session-direct"))
        api.createSession(PlaybackRequest(MediaRef.Episode(7, 1, 2), caps))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/playback/sessions", request.target)
        assertTrue(request.body!!.utf8().contains(""""type":"episode""""))
    }

    @Test
    fun recalageEnregistreEnPut() = runTest {
        server.reply(body = """{"sync":{"offset":1.5,"rate":1.0,"source":"manual","note":null}}""")
        val sync = api.saveSubtitleSync("s1", "3", 1.5, 1.0)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/playback/sessions/s1/subtitles/3/sync", request.target)
        assertEquals("""{"offset":1.5,"rate":1.0}""", request.body!!.utf8())
        assertEquals(1.5, sync.offset, 0.0)
    }

    @Test
    fun progressionEnvoieUneDureeNulleExplicite() = runTest {
        server.reply(code = 204)
        api.saveProgress("s1", ProgressUpdate(12.5, null))
        assertEquals("""{"position":12.5,"duration":null}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun erreurDuServeurAvecSonMessage() = runTest {
        server.reply(code = 404, body = """{"error":"not_found","message":"Titre introuvable"}""")
        try {
            api.title(999)
            fail("exception attendue")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
            assertEquals("Titre introuvable", e.message)
        }
    }

    @Test
    fun erreurDuServeurSansJson() = runTest {
        server.reply(code = 500, body = "boom", contentType = "text/plain")
        try {
            api.home()
            fail("exception attendue")
        } catch (e: ApiException) {
            assertEquals(500, e.status)
            assertEquals("Le serveur Lumière a rencontré une erreur (500).", e.message)
        }
    }

    @Test
    fun serveurInjoignable() = runTest {
        server.close()
        try {
            api.home()
            fail("exception attendue")
        } catch (_: ServerUnreachableException) {
        }
    }

    @Test
    fun urlAbsolueDesImages() {
        assertEquals(server.address().baseUrl + "/api/images/w342/a.jpg", api.absoluteUrl("/api/images/w342/a.jpg"))
        assertEquals("https://x/y.jpg", api.absoluteUrl("https://x/y.jpg"))
    }

    @Test
    fun origineDuFlux() = runTest {
        server.reply(body = """{"start":12.48}""")
        assertEquals(12.48, api.streamOrigin("s1", "abc"), 0.0)
        assertEquals("/api/playback/sessions/s1/streams/abc/origin", server.takeRequest().target)
    }

    @Test
    fun recalageAutomatiqueSansCorpsJson() = runTest {
        server.reply(body = """{"sync":null,"message":"Rien de sûr"}""")
        api.autoSubtitleSync("s1", "3")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(null, request.headers["Content-Type"])
    }

    @Test
    fun sousTitresEnTexte() = runTest {
        server.reply(body = "WEBVTT\n\n00:01.000 --> 00:02.000\nBonjour\n", contentType = "text/vtt")
        assertTrue(api.subtitles("s1", "2").startsWith("WEBVTT"))
    }
}
