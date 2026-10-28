package fr.lumiere.tv.ui.catalog

import fr.lumiere.tv.data.api.ApiException
import fr.lumiere.tv.data.api.Page
import fr.lumiere.tv.ui.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedLoaderTest {

    private fun page(n: Int, hasMore: Boolean) = Page(listOf("p$n-a", "p$n-b"), n, 2, 10, hasMore)

    @Test
    fun unSeulAppelPendantUnChargement() = runTest {
        val calls = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()
        val loader = PagedLoader(this) { n -> calls += n; gate.await(); page(n, true) }
        loader.loadMore()
        loader.loadMore()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(1), calls)
    }

    @Test
    fun rienApresLaDernierePage() = runTest {
        val calls = mutableListOf<Int>()
        val loader = PagedLoader(this) { n -> calls += n; page(n, hasMore = false) }
        loader.loadMore()
        advanceUntilIdle()
        loader.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1), calls)
    }

    @Test
    fun pagesAjouteesDansLOrdre() = runTest {
        val loader = PagedLoader(this) { n -> page(n, hasMore = n < 2) }
        loader.loadMore(); advanceUntilIdle()
        loader.loadMore(); advanceUntilIdle()
        assertEquals(listOf("p1-a", "p1-b", "p2-a", "p2-b"), loader.items.value)
    }

    @Test
    fun erreurPuisNouvelEssaiDeLaMemePage() = runTest {
        val calls = mutableListOf<Int>()
        var fail = true
        val loader = PagedLoader(this) { n ->
            calls += n
            if (fail) throw ApiException(500, "Erreur") else page(n, true)
        }
        loader.loadMore(); advanceUntilIdle()
        assertTrue(loader.state.value is UiState.Error)
        fail = false
        loader.loadMore(); advanceUntilIdle()
        assertEquals(listOf(1, 1), calls)
        assertTrue(loader.state.value is UiState.Ready)
    }

    @Test
    fun remiseAZero() = runTest {
        val calls = mutableListOf<Int>()
        val loader = PagedLoader(this) { n -> calls += n; page(n, true) }
        loader.loadMore(); advanceUntilIdle()
        loader.reset(); advanceUntilIdle()
        assertEquals(listOf(1, 1), calls)
        assertEquals(listOf("p1-a", "p1-b"), loader.items.value)
    }
}
