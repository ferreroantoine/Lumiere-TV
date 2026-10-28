package fr.lumiere.tv.data.server

import fr.lumiere.tv.testing.address
import fr.lumiere.tv.testing.reply
import fr.lumiere.tv.testing.testClient
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ServerCheckTest {
    private val server = MockWebServer()
    private val check = ServerCheck(testClient(readTimeoutMs = 1_000))

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    @Test
    fun lumiereRepondAvecSaVersion() = runTest {
        server.reply(body = """{"status":"ok"}""")
        server.reply(body = """{"version":"1.0.0","tmdbConfigured":true}""")
        assertEquals(CheckResult.Ok("1.0.0"), check.check(server.address()))
    }

    @Test
    fun pageIntrouvableNEstPasLumiere() = runTest {
        server.reply(code = 404, body = "Not found", contentType = "text/plain")
        assertEquals(CheckResult.NotLumiere, check.check(server.address()))
    }

    @Test
    fun pageHtmlNEstPasLumiere() = runTest {
        server.reply(body = "<html><body>Freebox</body></html>", contentType = "text/html")
        assertEquals(CheckResult.NotLumiere, check.check(server.address()))
    }

    @Test
    fun erreurServeur() = runTest {
        server.reply(code = 500, body = "")
        assertEquals(CheckResult.HttpError(500), check.check(server.address()))
    }

    @Test
    fun reponseTropLente() = runTest {
        server.reply(body = """{"status":"ok"}""", delayMs = 3_000)
        assertEquals(CheckResult.Timeout, check.check(server.address()))
    }

    @Test
    fun serveurArrete() = runTest {
        val address = server.address()
        server.close()
        assertEquals(CheckResult.Unreachable, check.check(address))
    }
}
