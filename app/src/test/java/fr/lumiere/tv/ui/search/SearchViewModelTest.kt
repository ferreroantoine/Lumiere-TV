package fr.lumiere.tv.ui.search

import fr.lumiere.tv.testing.MainDispatcherRule
import fr.lumiere.tv.testing.api
import fr.lumiere.tv.testing.fixture
import fr.lumiere.tv.testing.routes
import fr.lumiere.tv.ui.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class SearchViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val server = MockWebServer()

    @Before fun setUp() {
        server.start()
        server.routes("/api/titles" to (200 to fixture("titles-page")))
    }

    @After fun tearDown() = server.close()

    @Test
    fun frappesRapidesUnSeulAppel() = runTest {
        val vm = SearchViewModel(server.api())
        vm.setQuery("a"); advanceTimeBy(100)
        vm.setQuery("am"); advanceTimeBy(100)
        vm.setQuery("ame"); advanceTimeBy(401); runCurrent()
        val state = vm.state.first { it is UiState.Ready && it.value.isNotEmpty() }
        assertTrue(state is UiState.Ready)
        assertEquals(1, server.requestCount)
        assertEquals("ame", server.takeRequest(1, TimeUnit.SECONDS)!!.url.queryParameter("q"))
    }

    @Test
    fun uneSeuleLettreNeDeclenchePasDeRecherche() = runTest {
        val vm = SearchViewModel(server.api())
        vm.setQuery("a"); advanceTimeBy(1_000); runCurrent()
        assertEquals(0, server.requestCount)
    }
}
