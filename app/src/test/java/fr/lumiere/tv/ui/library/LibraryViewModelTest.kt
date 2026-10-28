package fr.lumiere.tv.ui.library

import fr.lumiere.tv.testing.MainDispatcherRule
import fr.lumiere.tv.testing.api
import fr.lumiere.tv.testing.routes
import fr.lumiere.tv.ui.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val server = MockWebServer()

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private fun item(id: Int, kind: String, title: String, season: Int? = null, episode: Int? = null, exists: Boolean = true) =
        """{"id":$id,"kind":"$kind","title":"$title","season":${season ?: "null"},"episode":${episode ?: "null"},""" +
            """"fileName":"f$id.mkv","size":1,"exists":$exists}"""

    private suspend fun load(vararg items: String): LibraryContent {
        server.routes("/api/library" to (200 to items.joinToString(",", "[", "]")))
        val vm = LibraryViewModel(server.api())
        return (vm.state.first { it !is UiState.Loading } as UiState.Ready).value
    }

    @Test
    fun repartitionEtTri() = runTest {
        val content = load(
            item(1, "movie", "Big Buck Moon"),
            item(2, "episode", "Orbite", 1, 2),
            item(3, "episode", "Les Explorateurs", 2, 1),
            item(4, "episode", "Les Explorateurs", 1, 3),
            item(5, "other", "Bonus"),
        )
        assertEquals(listOf(1), content.movies.map { it.id })
        assertEquals(listOf(4, 3, 2), content.episodes.map { it.id })
    }

    @Test
    fun fichierDisparuIgnore() = runTest {
        val content = load(item(1, "movie", "A"), item(2, "movie", "B", exists = false))
        assertEquals(listOf(1), content.movies.map { it.id })
    }

    @Test
    fun bibliothequeVide() = runTest {
        assertEquals(LibraryContent(emptyList(), emptyList()), load())
    }
}
