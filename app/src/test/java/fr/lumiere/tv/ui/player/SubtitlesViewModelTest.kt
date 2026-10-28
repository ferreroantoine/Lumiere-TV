package fr.lumiere.tv.ui.player

import fr.lumiere.tv.data.api.LumiereJson
import fr.lumiere.tv.data.api.PlaybackSession
import fr.lumiere.tv.testing.MainDispatcherRule
import fr.lumiere.tv.testing.api
import fr.lumiere.tv.testing.routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class SubtitlesViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private val vtt = "WEBVTT\n\n00:10.000 --> 00:12.000\nBonjour\n"

    private fun session(autoLanguage: String? = null) = LumiereJson.decodeFromString<PlaybackSession>(
        """
        {"sessionId":"s1","mode":"direct","streamUrl":"/x","mimeType":"video/mp4","seek":"native","mediaKey":"k","title":"T",
         "subtitleTracks":[{"id":"2","label":"Français","language":"fr","codec":"subrip","source":"embedded"}],
         "onlineSubtitles":{"available":true,"autoLanguage":${autoLanguage?.let { "\"$it\"" } ?: "null"}}}
        """.trimIndent(),
    )

    @Test
    fun selectionAppliqueLeRecalageEnregistre() = runTest {
        server.routes(
            "/api/playback/sessions/s1/subtitles/2" to (200 to vtt),
            "/api/playback/sessions/s1/subtitles/2/sync" to (200 to """{"sync":{"offset":1.0,"rate":1.0,"source":"manual"},"autoFailed":false}"""),
        )
        val vm = SubtitlesViewModel(server.api())
        vm.attach(session())
        vm.select("2")
        vm.state.first { it.selected == "2" && it.loaded }
        assertTrue(vm.cuesAt(10.5).isEmpty())
        assertEquals(listOf("Bonjour"), vm.cuesAt(11.5).single().lines)
    }

    @Test
    fun decalageEnregistreParPas() = runTest {
        server.routes(
            "/api/playback/sessions/s1/subtitles/2" to (200 to vtt),
            "/api/playback/sessions/s1/subtitles/2/sync" to (200 to """{"sync":null,"autoFailed":false}"""),
        )
        val vm = SubtitlesViewModel(server.api())
        vm.attach(session())
        vm.select("2")
        vm.state.first { it.loaded }
        vm.nudge(0.25)
        vm.nudge(0.25)
        vm.state.first { it.offset == 0.5 }
        // Lecture des requêtes hors du fil du test, pour que les enregistrements lancés puissent s'exécuter.
        val puts = withContext(Dispatchers.IO) { (1..6).mapNotNull { server.takeRequest(1, TimeUnit.SECONDS) } }
            .filter { it.method == "PUT" }
        assertEquals("""{"offset":0.5,"rate":1.0}""", puts.last().body!!.utf8())
    }

    @Test
    fun fichierVideMarqueIndisponible() = runTest {
        server.routes(
            "/api/playback/sessions/s1/subtitles/2" to (200 to ""),
            "/api/playback/sessions/s1/subtitles/2/sync" to (200 to """{"sync":null,"autoFailed":false}"""),
        )
        val vm = SubtitlesViewModel(server.api())
        vm.attach(session())
        vm.select("2")
        val state = vm.state.first { "2" in it.unavailable }
        assertEquals(null, state.selected)
        assertTrue(vm.cuesAt(11.0).isEmpty())
    }

    @Test
    fun rechercheEnLigneDOfficeDansLaLanguePreferee() = runTest {
        server.routes(
            "/api/playback/sessions/s1/subtitles/online" to (200 to """{"tracks":[
                {"id":"o1","label":"English","language":"en","codec":"vtt","source":"online"},
                {"id":"o2","label":"Français (OpenSubtitles)","language":"fr","codec":"vtt","source":"online"}],
                "providers":["OpenSubtitles"],"notices":[]}"""),
            "/api/playback/sessions/s1/subtitles/o2" to (200 to vtt),
            "/api/playback/sessions/s1/subtitles/o2/sync" to (200 to """{"sync":null,"autoFailed":false}"""),
        )
        val vm = SubtitlesViewModel(server.api())
        val s = session(autoLanguage = "fr").copy(subtitleTracks = emptyList())
        vm.attach(s)
        val state = vm.state.first { it.selected == "o2" }
        assertTrue(state.tracks.any { it.id == "o1" })
    }
}
