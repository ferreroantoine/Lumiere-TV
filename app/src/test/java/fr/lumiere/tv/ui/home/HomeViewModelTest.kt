package fr.lumiere.tv.ui.home

import fr.lumiere.tv.testing.MainDispatcherRule
import fr.lumiere.tv.testing.api
import fr.lumiere.tv.testing.fixture
import fr.lumiere.tv.testing.routes
import fr.lumiere.tv.ui.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private fun serveAll() = server.routes(
        "/api/home" to (200 to fixture("home")),
        "/api/playback/continue" to (200 to fixture("continue")),
        "/api/library" to (200 to fixture("library")),
    )

    @Test
    fun accueilComplet() = runTest {
        serveAll()
        val vm = HomeViewModel(server.api())
        val state = vm.state.first { it !is UiState.Loading }
        state as UiState.Ready
        assertEquals(state.value.featured, state.value.featuredCandidates.first())
        assertTrue(state.value.movies.isNotEmpty())
        assertEquals(2, state.value.resume.size)
    }

    @Test
    fun rechargementGardeLeContenuAffiche() = runTest {
        // Retour sur l'accueil : le contenu reste à l'écran (focus, défilement) pendant le rechargement.
        serveAll()
        val vm = HomeViewModel(server.api())
        vm.state.first { it is UiState.Ready }
        vm.reload()
        assertTrue(vm.state.value is UiState.Ready)
    }

    @Test
    fun serveurInjoignable() = runTest {
        val api = server.api()
        server.close()
        val vm = HomeViewModel(api)
        val state = vm.state.first { it !is UiState.Loading }
        assertEquals(true, (state as UiState.Error).unreachable)
    }

    @Test
    fun rechargementApresErreur() = runTest {
        server.routes()
        val vm = HomeViewModel(server.api())
        assertTrue(vm.state.first { it !is UiState.Loading } is UiState.Error)
        serveAll()
        vm.reload()
        assertTrue(vm.state.value is UiState.Loading)
        assertTrue(vm.state.first { it !is UiState.Loading } is UiState.Ready)
    }
}
