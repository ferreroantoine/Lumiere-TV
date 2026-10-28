package fr.lumiere.tv.player

import fr.lumiere.tv.data.api.ClientCapabilities
import fr.lumiere.tv.data.api.MediaRef
import fr.lumiere.tv.testing.api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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

    /** Origines renvoyées dans l'ordre (sinon [origin]), délai de réponse de /origin et des créations. */
    private val origins = ArrayDeque<String>()
    private var originDelayMs = 0L
    private val createDelays = ArrayDeque<Long>()
    private var now = 0L

    private class FakePlayer : PlayerPort {
        val loads = mutableListOf<Pair<String, Double>>()
        var released = false
        var registered: PlayerPort.Listener? = null
        override var positionSec = 0.0
        override var isPlaying = true
        override var playWhenReady = true
        override fun load(url: String, startPositionSec: Double) { loads += url to startPositionSec }
        override fun play() { playWhenReady = true }
        override fun pause() { playWhenReady = false }
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
                    request.method == "POST" && path == "/api/playback/sessions" ->
                        json(sessions.removeFirst(), createDelays.removeFirstOrNull() ?: 0)
                    path.endsWith("/origin") -> json("""{"start":${origins.removeFirstOrNull() ?: origin}}""", originDelayMs)
                    else -> MockResponse.Builder().code(204).build()
                }
            }
        }
        server.start()
    }

    @After fun tearDown() = server.close()

    private fun json(body: String, delayMs: Long = 0) = MockResponse.Builder().code(200)
        .setHeader("Content-Type", "application/json").body(body)
        .apply { if (delayMs > 0) headersDelay(delayMs, TimeUnit.MILLISECONDS) }
        .build()

    private fun startParams(player: FakePlayer) =
        player.loads.mapNotNull { Regex("start=([^&]+)").find(it.first)?.groupValues?.get(1) }

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
    private val isOrigin: (RecordedRequest) -> Boolean = { it.url.encodedPath.endsWith("/origin") }

    @Test
    fun retourPendantLeChargementGardeLaReprise() = runTest {
        // L'origine n'arrive qu'une fois ffmpeg lancé : la position doit rester celle demandée.
        sessions += session("s1", "transcode", resume = 1200.0)
        origin = "1200"
        originDelayMs = 1_500
        val player = FakePlayer()
        val c = controller(player)
        val job = launch { c.start(MediaRef.Library(3)) }
        waitFor(1, isOrigin)
        assertEquals(1200.0, c.position, 0.0)
        c.stop()
        val progress = waitFor(1, isProgress)
        assertTrue(progress.first().body!!.utf8().contains(""""position":1200"""))
        job.cancel()
    }

    @Test
    fun okPendantUnChargementMetEnPause() = runTest {
        // Après un saut, l'image attend le flux (isPlaying faux) : ce n'est pas une pause.
        sessions += session("s1", "transcode")
        origin = "0"
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        player.isPlaying = false
        assertEquals(false, c.paused)
        c.togglePause()
        assertEquals(true, c.paused)
        c.togglePause()
        assertEquals(false, c.paused)
    }

    @Test
    fun deuxAppuisRapidesSAdditionnent() = runTest {
        sessions += session("s1", "transcode", resume = 1200.0)
        origin = "1200"
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.seekTo(c.position + 10)
        c.seekTo(c.position + 10)
        advanceTimeBy(400)
        runCurrent()
        assertEquals(listOf("1200", "1220"), startParams(player))
    }

    @Test
    fun sautEnAvantRelanceAuDelaDeLImageCle() = runTest {
        // Remux : l'image clé atteinte (595) précède la position quittée (600) : relance plus loin.
        sessions += session("s1", "remux", resume = 600.0)
        origins += listOf("600", "595", "626")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.seekTo(610.0)
        advanceTimeBy(400)
        runCurrent()
        waitFor(3, isOrigin)
        assertEquals(listOf("600", "610", "626"), startParams(player))
    }

    @Test
    fun passageALEpisodeSuivantUneSeuleFois() = runTest {
        val next = """{"media":{"type":"episode","titleId":7,"season":1,"episode":3},"label":"É3"}"""
        sessions += session("s1", "direct", duration = 1500.0, next = next)
        sessions += session("s2", "direct")
        createDelays += listOf(0L, 1_500L)
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Episode(7, 1, 2))
        player.positionSec = 1481.0
        repeat(11) { c.tick() }
        waitFor(2, isCreate)
        // Pendant la création de la session suivante : ni nouveau compte à rebours, ni second passage.
        c.tick()
        assertNull(c.state.value.nextCountdown)
        player.registered!!.onEnded()
        withContext(Dispatchers.IO) { Thread.sleep(1_800) }
        advanceUntilIdle()
        assertEquals(2, requests.count(isCreate))
        assertNull(c.state.value.nextCountdown)
    }

    @Test
    fun coupureReseauRepriseAuMemeEndroit() = runTest {
        sessions += session("s1", "direct")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.tick()
        player.positionSec = 300.0
        player.registered!!.onError(PlayerFailure("le flux vidéo ne répond plus", network = true))
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(1, requests.count(isCreate))
        assertEquals(300.0, player.loads.last().second, 0.0)
        assertNull(c.state.value.error)
    }

    @Test
    fun compteAReboursAnnuleHorsDeLaFenetre() = runTest {
        val next = """{"media":{"type":"episode","titleId":7,"season":1,"episode":3},"label":"É3"}"""
        sessions += session("s1", "direct", duration = 1500.0, next = next)
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Episode(7, 1, 2))
        player.positionSec = 1481.0
        c.tick()
        assertEquals(10, c.state.value.nextCountdown)
        player.positionSec = 1000.0
        c.tick()
        assertNull(c.state.value.nextCountdown)
    }

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
        origins += listOf("0", "598.7")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.seekTo(600.0)
        advanceTimeBy(400)
        runCurrent()
        waitFor(2, isOrigin)
        val url = player.loads.last().first
        assertTrue(url.contains("start=600"))
        val firstStream = Regex("stream=([\\w-]+)").find(player.loads.first().first)!!.groupValues[1]
        val secondStream = Regex("stream=([\\w-]+)").find(url)!!.groupValues[1]
        assertNotEquals(firstStream, secondStream)
        player.positionSec = 5.0
        assertEquals(603.7, c.position, 0.0001)
    }

    @Test
    fun avanceRapideSArreteAvantLaFin() = runTest {
        origin = "0"
        sessions += session("s1", "transcode", duration = 40.0)
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        c.seekTo(45.0)
        advanceTimeBy(400)
        runCurrent()
        assertTrue(player.loads.last().first.contains("start=37&"))
    }

    @Test
    fun erreurDansLesDernieresSecondesVautFin() = runTest {
        origin = "0"
        sessions += session("s1", "transcode", duration = 40.0)
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        player.positionSec = 38.5
        player.registered!!.onError(IllegalStateException("fin de flux"))
        assertEquals(true, c.state.value.ended)
        assertNull(c.state.value.error)
    }

    @Test
    fun origineNegativeNeDonnePasDePositionNegative() = runTest {
        // Le serveur peut annoncer un départ réel juste avant zéro (image clé).
        origin = "-0.083"
        sessions += session("s1", "remux")
        sessions += session("s2", "transcode")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        assertEquals(0.0, c.position, 0.0)
        player.registered!!.onError(IllegalStateException("décodeur"))
        advanceUntilIdle()
        waitFor(2, isCreate)
        assertTrue(player.loads.none { it.first.contains("start=-") })
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
    fun replisEnConversionDepuisLeRemux() = runTest {
        // Vidéo copiée telle quelle (remux) que le décodeur refuse : HEVC 10 bits par exemple.
        sessions += session("s1", "remux")
        sessions += session("s2", "transcode")
        val player = FakePlayer()
        val c = controller(player)
        c.start(MediaRef.Library(3))
        player.registered!!.onError(IllegalStateException("décodeur"))
        advanceUntilIdle()
        val creates = waitFor(2, isCreate)
        assertEquals(2, creates.size)
        assertTrue(creates[1].body!!.utf8().contains(""""mode":"transcode""""))
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
