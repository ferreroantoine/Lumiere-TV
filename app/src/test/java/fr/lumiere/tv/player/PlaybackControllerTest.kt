package fr.lumiere.tv.player

import fr.lumiere.tv.data.api.ClientCapabilities
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.testing.api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class PlaybackControllerTest {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private val caps = ClientCapabilities(listOf("h264", "hevc"), listOf("aac"), listOf("mp4", "mkv"))

    /** Sessions renvoyées dans l'ordre par POST /api/playback/sessions. */
    private val sessions = ArrayDeque<String>()
    private var origin = "598.7"
    private var now = 0L

    private class FakePlayer : PlayerPort {
        val loads = mutableListOf<Pair<String, Double>>()
        var released = false
        var registered: PlayerPort.Listener? = null
        override var positionSec = 0.0
        override val isPlaying = true
        override fun load(url: String, startPositionSec: Double) { loads += url to startPositionSec }
        override fun play() {}
        override fun pause() {}
        override fun seekTo(positionSec: Double) { this.positionSec = positionSec }
        override fun setListener(listener: PlayerPort.Listener) { registered = listener }
        override fun release() { released = true }
    }

    private fun session(id: String, mode: String, resume: Double = 0.0, duration: Double = 1500.0, next: String? = null) = """
        {"sessionId":"$id","mode":"$mode","streamUrl":"/api/playback/sessions/$id/stream","mimeType":"video/mp4",
         "seek":"${if (mode == "direct") "native" else "restart"}","duration":$duration,"resumePosition":$resume,
         "mediaKey":"k","title":"T","next":${next ?: "null"}}
    """.trimIndent()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.url.encodedPath
                return when {
                    request.method == "POST" && path == "/api/playback/sessions" -> json(sessions.removeFirst())
                    path.endsWith("/origin") -> json("""{"start":$origin}""")
                    else -> MockResponse.Builder().code(204).build()
                }
            }
        }
        server.start()
    }

    @After fun tearDown() = server.close()

    private fun json(body: String) = MockResponse.Builder().code(200).setHeader("Content-Type", "application/json").body(body).build()

    private fun TestScope.controller(player: FakePlayer) = PlaybackController(server.api(), player, caps, this) { now }

    /**
     * Attend que le faux serveur ait reçu `count` requêtes correspondantes. L'attente suspend
     * (au lieu de bloquer) pour que les suites des coroutines puissent s'exécuter entre-temps.
     */
    private suspend fun TestScope.waitFor(count: Int, predicate: (RecordedRequest) -> Boolean): List<RecordedRequest> {
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline && requests.count(predicate) < count) {
            advanceUntilIdle()
            withContext(Dispatchers.IO) { Thread.sleep(20) }
        }
        advanceUntilIdle()
        return requests.filter(predicate)
    }

    private val isProgress: (RecordedRequest) -> Boolean = { it.url.encodedPath.endsWith("/progress") }
    private val isCreate: (RecordedRequest) -> Boolean = { it.method == "POST" && it.url.encodedPath == "/api/playback/sessions" }

    @Test
    fun lectureDirecteRepriseALaPosition() = runTest {
        sessions += session("s1", "direct", resume = 120.0)
        val player = FakePlayer()
        controller(player).start(MediaRef.Library(3))
        assertEquals(listOf(server.url("/").toString().trimEnd('/') + "/api/playback/sessions/s1/stream" to 120.0), player.loads)
    }

    @Test
    fun conversionAvanceRapideEtOrigine() = runTest {
        sessions += session("s1", "transcode")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.seekTo(600.0)
        val url = player.loads.last().first
        assertTrue(url.contains("start=600"))
        val firstStream = Regex("stream=([\\w-]+)").find(player.loads.first().first)!!.groupValues[1]
        val secondStream = Regex("stream=([\\w-]+)").find(url)!!.groupValues[1]
        assertNotEquals(firstStream, secondStream)
        player.positionSec = 5.0
        assertEquals(603.7, c.position, 0.0001)
    }

    @Test
    fun replisEnConversionUneSeuleFois() = runTest {
        sessions += session("s1", "direct")
        sessions += session("s2", "transcode")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        player.positionSec = 42.0
        player.registered!!.onError(IllegalStateException("décodeur"))
        advanceUntilIdle()
        val creates = waitFor(2, isCreate)
        assertEquals(2, creates.size)
        assertTrue(creates[1].body!!.utf8().contains(""""mode":"transcode""""))
        player.registered!!.onError(IllegalStateException("encore"))
        advanceUntilIdle()
        assertEquals(2, requests.count(isCreate))
        assertNotNull(c.state.value.error)
    }

    @Test
    fun progressionToutesLesDixSecondes() = runTest {
        sessions += session("s1", "direct")
        val c = controller(FakePlayer())
        c.start(MediaRef.Library(3))
        for (s in 1..9) { now = s * 1000L; c.tick() }
        advanceUntilIdle()
        Thread.sleep(300)
        assertEquals(0, requests.count(isProgress))
        now = 10_000L; c.tick()
        advanceUntilIdle()
        assertEquals(1, waitFor(1, isProgress).size)
    }

    @Test
    fun episodeSuivantApresLeCompteARebours() = runTest {
        val next = """{"media":{"type":"episode","titleId":7,"season":1,"episode":3},"label":"É3"}"""
        sessions += session("s1", "direct", duration = 1500.0, next = next)
        sessions += session("s2", "direct")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Episode(7, 1, 2))
        player.positionSec = 1481.0
        c.tick()
        assertEquals(10, c.state.value.nextCountdown)
        repeat(10) { c.tick() }
        advanceUntilIdle()
        val creates = waitFor(2, isCreate)
        assertTrue(creates[1].body!!.utf8().contains(""""episode":3"""))
        assertNull(c.state.value.nextCountdown)
    }

    @Test
    fun arretEnvoieLaPositionPuisFermeLaSession() = runTest {
        sessions += session("s1", "direct")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.stop()
        val paths = requests.map { it.method + " " + it.url.encodedPath }
        assertEquals(
            listOf("POST /api/playback/sessions", "POST /api/playback/sessions/s1/progress", "DELETE /api/playback/sessions/s1"),
            paths,
        )
        assertTrue(player.released)
    }

    @Test
    fun arretMemeServeurInjoignable() = runTest {
        sessions += session("s1", "direct")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        server.close()
        c.stop()
        assertTrue(player.released)
    }

    @Suppress("unused")
    private fun unused() = server.takeRequest(1, TimeUnit.MILLISECONDS)
}
